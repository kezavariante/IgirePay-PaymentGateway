
package com.igirepay;

import com.igirepay.dao.AccountDAO;
import com.igirepay.dao.CustomerDAO;
import com.igirepay.dao.TransactionDAO;

import java.io.Console;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class IgirePayApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Console console = System.console();

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final AccountDAO accountDAO = new AccountDAO();
    private static final TransactionDAO transactionDAO =
            new TransactionDAO();
    private static final PaymentService paymentService =
            new PaymentService();
    private static final PinService pinService = new PinService();
    private static final LoanService loanService = new LoanService();

    public static void main(String[] args) {

        boolean running = true;

        while (running) {
            displayMenu();

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        registerCustomer();
                        break;
                    case "2":
                        createAccount("WALLET");
                        break;
                    case "3":
                        createAccount("SAVINGS");
                        break;
                    case "4":
                        viewCustomer();
                        break;
                    case "5":
                        updateCustomer();
                        break;
                    case "6":
                        viewCustomerAccounts();
                        break;
                    case "7":
                        viewAccountBalance();
                        break;
                    case "8":
                        deleteInactiveAccount();
                        break;
                    case "9":
                        deposit();
                        break;
                    case "10":
                        withdraw();
                        break;
                    case "11":
                        transfer();
                        break;
                    case "12":
                        viewTransactionHistory();
                        break;
                    case "13":
                        createCustomerPin();
                        break;
                    case "14":
                        validateCustomerPin();
                        break;
                    case "15":
                        changeCustomerPin();
                        break;
                    case "16":
                        requestLoan();
                        break;
                    case "17":
                        repayLoan();
                        break;
                    case "18":
                        checkLoanLimit();
                        break;
                    case "19":
                        running = false;
                        System.out.println(
                                "Thank you for using IgirePay!");
                        break;
                    default:
                        System.out.println(
                                "Invalid choice. Try again.");
                }

            } catch (Exception e) {
                System.out.println(
                        "Operation failed: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("========== IGIREPAY ==========");
        System.out.println("1. Register customer");
        System.out.println("2. Create wallet account");
        System.out.println("3. Create savings account");
        System.out.println("4. View customer");
        System.out.println("5. Update customer details");
        System.out.println("6. View customer accounts");
        System.out.println("7. View account balance");
        System.out.println("8. Delete inactive account");
        System.out.println("9. Deposit money");
        System.out.println("10. Withdraw money");
        System.out.println("11. Transfer money");
        System.out.println("12. View transaction history");
        System.out.println("13. Create customer PIN");
        System.out.println("14. Validate customer PIN");
        System.out.println("15. Change customer PIN");
        System.out.println("16. Request a loan");
        System.out.println("17. Repay a loan");
        System.out.println("18. Check eligible loan limit");
        System.out.println("19. Exit");
        System.out.print("Choose an option: ");
    }


    private static String readLine() {
        if (console != null) {
            String value = console.readLine();
            return value == null ? "" : value;
        }

        return scanner.nextLine();
    }

    private static String readPin(String prompt) {
        if (console != null) {
            char[] enteredPin = console.readPassword("%s", prompt);

            if (enteredPin == null) {
                return "";
            }

            try {
                return new String(enteredPin);
            } finally {
                java.util.Arrays.fill(enteredPin, '\0');
            }
        }

        System.out.print(prompt);
        return readLine().trim();
    }

    private static boolean isValidPinFormat(String pin) {
        return pin != null && pin.matches("\\d{4,6}");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = readLine().trim();

            try {
                int number = Integer.parseInt(value);

                if (number > 0) {
                    return number;
                }

                System.out.println(
                        "Enter a positive whole number.");

            } catch (NumberFormatException e) {
                System.out.println(
                        "Please enter a valid whole number.");
            }
        }
    }

    private static BigDecimal readAmount(
            String prompt, boolean allowZero) {

        while (true) {
            System.out.print(prompt);
            String value = readLine().trim();

            try {
                BigDecimal amount = new BigDecimal(value);

                if (amount.scale() > 2) {
                    System.out.println(
                            "Enter an amount with at most two decimal places.");
                    continue;
                }

                int comparison =
                        amount.compareTo(BigDecimal.ZERO);

                if (allowZero ? comparison >= 0 : comparison > 0) {
                    return amount;
                }

                if (allowZero) {
                    System.out.println(
                            "Amount cannot be negative.");
                } else {
                    System.out.println(
                            "Amount must be greater than zero.");
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Please enter a valid amount.");
            }
        }
    }



    private static boolean authenticateCustomer(int customerID)
            throws SQLException {

        Customer customer =
                customerDAO.getCustomerById(customerID);

        if (customer == null) {
            System.out.println("Customer not found.");
            return false;
        }

        String pin = readPin("Enter your PIN: ");

        if (!pinService.validatePin(customerID, pin)) {
            System.out.println(
                    "Invalid PIN or customer has no PIN. Access denied.");
            return false;
        }

        return true;
    }


    private static Account getAuthenticatedAccount(int accountID)
            throws SQLException {

        Account account = accountDAO.getAccountById(accountID);

        if (account == null) {
            System.out.println("Account not found.");
            return null;
        }

        if (!authenticateCustomer(account.getCustomerID())) {
            return null;
        }

        return account;
    }

    private static void registerCustomer() throws SQLException {

        System.out.print("Enter full name: ");
        String fullName = readLine().trim();

        System.out.print("Enter email: ");
        String email = readLine().trim();

        System.out.print("Enter phone number: ");
        String phoneNumber = readLine().trim();

        if (fullName.isBlank() || email.isBlank()) {
            System.out.println(
                    "Name and email are required.");
            return;
        }


        Customer customer = new Customer(
                1, fullName, email, phoneNumber);

        int customerID = customerDAO.addCustomer(customer);

        System.out.println(
                "Customer registered successfully. Customer ID: "
                        + customerID);
        System.out.println(
                "Create a PIN using option 13 before using protected features.");
    }

    private static void viewCustomer() throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        Customer customer =
                customerDAO.getCustomerById(customerID);

        System.out.println(customer);
    }

    private static void updateCustomer() throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        Customer customer =
                customerDAO.getCustomerById(customerID);

        System.out.println("Current details: " + customer);

        System.out.print("Enter new full name: ");
        String fullName = readLine().trim();

        System.out.print("Enter new email: ");
        String email = readLine().trim();

        System.out.print("Enter new phone number: ");
        String phoneNumber = readLine().trim();

        if (fullName.isBlank() || email.isBlank()) {
            System.out.println(
                    "Name and email are required.");
            return;
        }

        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhoneNumber(phoneNumber);

        boolean updated =
                customerDAO.updateCustomer(customer, customerID);

        if (updated) {
            System.out.println(
                    "Customer details updated successfully.");
        } else {
            System.out.println(
                    "Customer details could not be updated.");
        }
    }


    private static void createAccount(String accountType)
            throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        BigDecimal initialBalance =
                readAmount("Enter initial balance: ", true);

        Account account;

        if ("WALLET".equals(accountType)) {
            account = new WalletAccount(
                    1, "TEMP", initialBalance, customerID);
        } else {
            account = new SavingsAccount(
                    1, "TEMP", initialBalance, customerID);
        }

        int accountID = accountDAO.addAccount(account);

        System.out.println(
                accountType + " account created successfully.");
        System.out.println("Account ID: " + accountID);
        System.out.println("Account number: ACC-" + accountID);
        System.out.println(
                "Initial balance: " + initialBalance);
    }

    private static void viewCustomerAccounts()
            throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        List<Account> accounts =
                accountDAO.getAccountsByCustomerId(customerID);

        if (accounts.isEmpty()) {
            System.out.println(
                    "No accounts found for this customer.");
            return;
        }

        for (Account account : accounts) {
            System.out.println(account);
        }
    }

    private static void viewAccountBalance()
            throws SQLException {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        System.out.println(
                "Account type: " + account.getAccountType());
        System.out.println(
                "Account number: " + account.getAccountNumber());
        System.out.println(
                "Current balance: " + account.getBalance());
    }

    private static void deleteInactiveAccount()
            throws SQLException {

        int accountID =
                readInt("Enter account ID to delete: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            System.out.println(
                    "This account cannot be deleted because its balance is not zero.");
            return;
        }

        System.out.println("Account details: " + account);
        System.out.print("Confirm deletion? (yes/no): ");

        String confirmation = readLine().trim();

        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Account deletion cancelled.");
            return;
        }

        try {
            boolean deleted =
                    accountDAO.deleteAccount(accountID);

            if (deleted) {
                System.out.println(
                        "Account deleted successfully.");
            } else {
                System.out.println(
                        "Account could not be deleted.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Account could not be deleted. It may have linked transactions.");
        }
    }


    private static void deposit() throws SQLException {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        BigDecimal amount =
                readAmount("Enter deposit amount: ", false);

        Transaction transaction = new Transaction(
                generateReferenceID(),
                amount,
                "DEPOSIT",
                LocalDateTime.now());

        boolean success =
                paymentService.processPayment(
                        accountID, transaction);

        if (success) {
            System.out.println(
                    "Deposit completed successfully.");
            displayBalance(accountID);
        } else {
            System.out.println(
                    "Request already processed.");
        }
    }

    private static void withdraw() throws SQLException {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        BigDecimal amount =
                readAmount("Enter withdrawal amount: ", false);

        Transaction transaction = new Transaction(
                generateReferenceID(),
                amount,
                "WITHDRAW",
                LocalDateTime.now());

        boolean success =
                paymentService.processPayment(
                        accountID, transaction);

        if (success) {
            System.out.println(
                    "Withdrawal completed successfully.");
            displayBalance(accountID);
        } else {
            System.out.println(
                    "Request already processed.");
        }
    }

    private static void transfer() throws SQLException {

        int sourceAccountID =
                readInt("Enter sender account ID: ");

        Account sourceAccount =
                getAuthenticatedAccount(sourceAccountID);

        if (sourceAccount == null) {
            return;
        }

        int destinationAccountID =
                readInt("Enter receiver account ID: ");

        if (sourceAccountID == destinationAccountID) {
            System.out.println(
                    "Sender and receiver accounts must be different.");
            return;
        }

        Account destinationAccount =
                accountDAO.getAccountById(destinationAccountID);

        if (destinationAccount == null) {
            System.out.println(
                    "Receiver account not found.");
            return;
        }


        BigDecimal amount =
                readAmount("Enter transfer amount: ", false);

        String referenceID = generateReferenceID();

        boolean success = paymentService.transfer(
                sourceAccountID,
                destinationAccountID,
                amount,
                referenceID);

        if (success) {
            System.out.println(
                    "Transfer completed successfully.");
            System.out.println(
                    "Reference ID: " + referenceID);

            displayBalance(sourceAccountID);
            displayBalance(destinationAccountID);

        } else {
            System.out.println(
                    "Request already processed.");
        }
    }

    private static void viewTransactionHistory()
            throws SQLException {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        List<Transaction> transactions =
                transactionDAO.getTransactionsByAccountId(accountID);

        if (transactions.isEmpty()) {
            System.out.println(
                    "No transactions found.");
            return;
        }

        System.out.println("Transaction history:");

        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }

    private static void displayBalance(int accountID)
            throws SQLException {

        Account account = accountDAO.getAccountById(accountID);

        if (account != null) {
            System.out.println(
                    "Account " + accountID
                            + " balance: " + account.getBalance());
        } else {
            System.out.println("Account not found.");
        }
    }



    private static void createCustomerPin() throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        Customer customer =
                customerDAO.getCustomerById(customerID);

        if (customer == null) {
            System.out.println("Customer not found.");
            return;
        }

        String pin = readPin("Enter a new 4-6 digit PIN: ");

        if (!isValidPinFormat(pin)) {
            System.out.println(
                    "PIN must contain 4 to 6 digits.");
            return;
        }

        String confirmation =
                readPin("Confirm the new PIN: ");

        if (!pin.equals(confirmation)) {
            System.out.println("PINs do not match.");
            return;
        }

        if (pinService.createPin(customerID, pin)) {
            System.out.println("PIN created successfully.");
        } else {
            System.out.println(
                    "PIN creation failed. The customer may already have a PIN.");
        }
    }

    private static void validateCustomerPin()
            throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        String pin = readPin("Enter PIN: ");

        if (pinService.validatePin(customerID, pin)) {
            System.out.println(
                    "PIN validated successfully.");
        } else {
            System.out.println(
                    "Invalid PIN or customer has no PIN.");
        }
    }

    private static void changeCustomerPin()
            throws SQLException {

        int customerID = readInt("Enter customer ID: ");


        String currentPin = readPin("Enter current PIN: ");

        String newPin = readPin("Enter new 4-6 digit PIN: ");

        if (!isValidPinFormat(newPin)) {
            System.out.println(
                    "New PIN must contain 4 to 6 digits.");
            return;
        }

        String confirmation =
                readPin("Confirm new PIN: ");

        if (!newPin.equals(confirmation)) {
            System.out.println(
                    "New PINs do not match.");
            return;
        }

        if (pinService.changePin(
                customerID, currentPin, newPin)) {

            System.out.println("PIN changed successfully.");

        } else {
            System.out.println(
                    "PIN change failed. Check the customer and current PIN.");
        }
    }



    private static void requestLoan() throws SQLException {

        int customerID = readInt("Enter customer ID: ");
        int accountID = readInt("Enter account ID: ");

        Account account = accountDAO.getAccountById(accountID);

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }


        if (account.getCustomerID() != customerID) {
            System.out.println(
                    "The account does not belong to this customer.");
            return;
        }

        if (!authenticateCustomer(customerID)) {
            return;
        }

        BigDecimal amount = readAmount(
                "Enter the amount to borrow (RWF): ", false);

        String message = loanService.requestLoan(
                customerID, accountID, amount);

        System.out.println(message);
    }

    private static void repayLoan() throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        String message = loanService.repayLoan(customerID);

        System.out.println(message);
    }

    private static void checkLoanLimit() throws SQLException {

        int customerID = readInt("Enter customer ID: ");

        if (!authenticateCustomer(customerID)) {
            return;
        }

        BigDecimal limit =
                loanService.getEligibleLimit(customerID);

        System.out.println(
                "Your current eligible loan limit is "
                        + limit + " RWF.");
    }

    private static String generateReferenceID() {
        return UUID.randomUUID().toString();
    }
}
