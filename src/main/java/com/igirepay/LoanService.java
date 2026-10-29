
package com.igirepay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LoanService {

    private static final BigDecimal FIRST_LIMIT =
            new BigDecimal("10000.00");

    private static final BigDecimal LIMIT_INCREASE =
            new BigDecimal("5000.00");

    private static final BigDecimal INTEREST_RATE =
            new BigDecimal("5.00");

    private static final BigDecimal INTEREST_DIVISOR =
            new BigDecimal("100.00");

    private static final int LOAN_DAYS = 30;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public String getLoanSummary() throws SQLException {
        String sql = """
                SELECT status, COUNT(*) AS total_loans,
                       COALESCE(SUM(amount), 0) AS total_amount,
                       COALESCE(SUM(total_due), 0) AS total_due
                FROM loan_requests
                GROUP BY status
                ORDER BY status
                """;

        StringBuilder report = new StringBuilder("LOAN SUMMARY\n");
        report.append("--------------------------------------------------\n");
        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                found = true;
                report.append("Status: ").append(result.getString("status")).append("\n");
                report.append("Loans: ").append(result.getLong("total_loans")).append("\n");
                report.append("Amount issued: ").append(result.getBigDecimal("total_amount"))
                        .append(" RWF\n");
                report.append("Total due: ").append(result.getBigDecimal("total_due"))
                        .append(" RWF\n");
                report.append("--------------------------------------------------\n");
            }
        }

        return found ? report.toString() : "No loan requests found.";
    }

    public String listLoans(String status) throws SQLException {
        String sql = """
                SELECT lr.id, lr.customer_id, c.full_name, lr.account_id,
                       lr.amount, lr.total_due, lr.amount_paid, lr.status, lr.due_date
                FROM loan_requests lr
                JOIN customers c ON c.id = lr.customer_id
                WHERE (? IS NULL OR lr.status = ?)
                ORDER BY lr.id DESC
                """;

        StringBuilder report = new StringBuilder();
        if (status == null) {
            report.append("ALL LOAN REQUESTS\n");
        } else {
            report.append(status).append(" LOANS\n");
        }
        report.append("--------------------------------------------------\n");
        boolean found = false;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (status == null) {
                statement.setNull(1, java.sql.Types.VARCHAR);
                statement.setNull(2, java.sql.Types.VARCHAR);
            } else {
                statement.setString(1, status);
                statement.setString(2, status);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    found = true;
                    report.append("Loan ID: ").append(result.getInt("id")).append("\n");
                    report.append("Customer ID: ").append(result.getInt("customer_id"))
                            .append(" (").append(result.getString("full_name")).append(")\n");
                    report.append("Account ID: ").append(result.getInt("account_id")).append("\n");
                    report.append("Amount: ").append(result.getBigDecimal("amount"))
                            .append(" RWF\n");
                    report.append("Total due: ").append(result.getBigDecimal("total_due"))
                            .append(" RWF\n");
                    report.append("Amount paid: ").append(result.getBigDecimal("amount_paid"))
                            .append(" RWF\n");
                    report.append("Status: ").append(result.getString("status")).append("\n");
                    report.append("Due date: ").append(result.getTimestamp("due_date")).append("\n");
                    report.append("--------------------------------------------------\n");
                }
            }
        }

        return found ? report.toString() : "No loans found.";
    }

    public BigDecimal getEligibleLimit(int customerID)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM loan_requests
                WHERE customer_id = ?
                  AND status = 'REPAID'
                  AND repaid_at <= due_date
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet result = statement.executeQuery()) {
                result.next();

                int successfulLoans = result.getInt(1);

                return FIRST_LIMIT.add(
                        LIMIT_INCREASE.multiply(
                                BigDecimal.valueOf(successfulLoans)));
            }
        }
    }

    public String requestLoan(int customerID, int accountID,
                              BigDecimal amount)
            throws SQLException {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Loan amount must be greater than zero.";
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                String accountSQL =
                        "SELECT id FROM accounts " +
                                "WHERE id = ? AND customer_id = ? FOR UPDATE";

                try (PreparedStatement statement =
                             connection.prepareStatement(accountSQL)) {

                    statement.setInt(1, accountID);
                    statement.setInt(2, customerID);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return "Customer or account not found.";
                        }
                    }
                }

                String activeSQL =
                        "SELECT id FROM loan_requests " +
                                "WHERE customer_id = ? " +
                                "AND status = 'APPROVED' FOR UPDATE";

                try (PreparedStatement statement =
                             connection.prepareStatement(activeSQL)) {

                    statement.setInt(1, customerID);

                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            return "You must repay your current loan first.";
                        }
                    }
                }

                BigDecimal limit =
                        getEligibleLimit(connection, customerID);

                if (amount.compareTo(limit) > 0) {
                    connection.rollback();
                    return "Request denied. Your maximum loan is "
                            + limit + " RWF.";
                }

                BigDecimal interest = amount
                        .multiply(INTEREST_RATE)
                        .divide(INTEREST_DIVISOR, 2, RoundingMode.HALF_UP);

                BigDecimal totalDue = amount.add(interest);

                LocalDateTime dueDate =
                        LocalDateTime.now().plusDays(LOAN_DAYS);

                String loanSQL = """
                        INSERT INTO loan_requests
                        (customer_id, account_id, amount, interest_rate,
                         interest_amount, total_due, amount_paid,
                         status, due_date)
                        VALUES (?, ?, ?, ?, ?, ?, 0, 'APPROVED', ?)
                        RETURNING id
                        """;

                int loanID;

                try (PreparedStatement statement =
                             connection.prepareStatement(loanSQL)) {

                    statement.setInt(1, customerID);
                    statement.setInt(2, accountID);
                    statement.setBigDecimal(3, amount);
                    statement.setBigDecimal(4, INTEREST_RATE);
                    statement.setBigDecimal(5, interest);
                    statement.setBigDecimal(6, totalDue);
                    statement.setTimestamp(7, Timestamp.valueOf(dueDate));

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                    "Loan could not be created.");
                        }

                        loanID = result.getInt("id");
                    }
                }

                String depositSQL =
                        "UPDATE accounts SET balance = balance + ? " +
                                "WHERE id = ? AND customer_id = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(depositSQL)) {

                    statement.setBigDecimal(1, amount);
                    statement.setInt(2, accountID);
                    statement.setInt(3, customerID);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Account deposit failed.");
                    }
                }

                addTransaction(connection, accountID, amount,
                        "DEPOSIT", "LOAN-" + UUID.randomUUID());

                connection.commit();

                return "LOAN APPROVED!\n"
                        + "Loan reference: " + loanID + "\n"
                        + "Amount deposited: " + amount + " RWF\n"
                        + "Interest (5%): " + interest + " RWF\n"
                        + "Total repayment: " + totalDue + " RWF\n"
                        + "Due date: " + dueDate.format(DATE_FORMAT);

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public String repayLoan(int customerID)
            throws SQLException {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                String loanSQL = """
                        SELECT id, account_id, total_due, amount_paid,
                               due_date
                        FROM loan_requests
                        WHERE customer_id = ?
                          AND status = 'APPROVED'
                        ORDER BY id DESC
                        LIMIT 1
                        FOR UPDATE
                        """;

                int loanID;
                int accountID;
                BigDecimal totalDue;
                BigDecimal amountPaid;
                LocalDateTime dueDate;

                try (PreparedStatement statement =
                             connection.prepareStatement(loanSQL)) {

                    statement.setInt(1, customerID);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return "No outstanding approved loan found.";
                        }

                        loanID = result.getInt("id");
                        accountID = result.getInt("account_id");
                        totalDue = result.getBigDecimal("total_due");
                        amountPaid = result.getBigDecimal("amount_paid");
                        dueDate = result.getTimestamp("due_date")
                                .toLocalDateTime();
                    }
                }

                BigDecimal remaining = totalDue.subtract(amountPaid);

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    connection.rollback();
                    return "This loan has no outstanding amount.";
                }

                String balanceSQL =
                        "UPDATE accounts SET balance = balance - ? " +
                                "WHERE id = ? AND customer_id = ? " +
                                "AND balance >= ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(balanceSQL)) {

                    statement.setBigDecimal(1, remaining);
                    statement.setInt(2, accountID);
                    statement.setInt(3, customerID);
                    statement.setBigDecimal(4, remaining);

                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return "Insufficient account balance to repay "
                                + remaining + " RWF.";
                    }
                }

                addTransaction(connection, accountID, remaining,
                        "WITHDRAW", "REPAY-" + UUID.randomUUID());

                String updateLoanSQL = """
                        UPDATE loan_requests
                        SET amount_paid = total_due,
                            status = 'REPAID',
                            repaid_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND customer_id = ?
                          AND status = 'APPROVED'
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateLoanSQL)) {

                    statement.setInt(1, loanID);
                    statement.setInt(2, customerID);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Loan repayment update failed.");
                    }
                }

                connection.commit();

                String message =
                        "Loan repaid successfully!\n"
                                + "Amount paid: " + remaining + " RWF\n";

                if (!LocalDateTime.now().isAfter(dueDate)) {
                    message += "You repaid on time. Your eligible limit "
                            + "may increase by 5,000 RWF.\n";
                } else {
                    message += "Payment was late. Your limit will not "
                            + "increase for this loan.\n";
                }

                return message;

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private BigDecimal getEligibleLimit(Connection connection,
                                        int customerID)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM loan_requests
                WHERE customer_id = ?
                  AND status = 'REPAID'
                  AND repaid_at <= due_date
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet result = statement.executeQuery()) {
                result.next();

                int successfulLoans = result.getInt(1);

                return FIRST_LIMIT.add(
                        LIMIT_INCREASE.multiply(
                                BigDecimal.valueOf(successfulLoans)));
            }
        }
    }

    private void addTransaction(Connection connection, int accountID,
                                BigDecimal amount, String type,
                                String referenceID)
            throws SQLException {

        String sql = """
                INSERT INTO transactions
                (account_id, reference_id, transaction_type, amount)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);
            statement.setString(2, referenceID);
            statement.setString(3, type);
            statement.setBigDecimal(4, amount);

            statement.executeUpdate();
        }
    }
}
