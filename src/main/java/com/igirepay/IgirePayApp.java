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
            displayMainMenu();
            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        customerManagementMenu();
                        break;
                    case "2":
                        accountManagementMenu();
                        break;
                    case "3":
                        transactionMenu();
                        break;
                    case "4":
                        pinManagementMenu();
                        break;
                    case "5":
                        loanManagementMenu();
                        break;
                    case "0":
                        running = false;
                        System.out.println("Thank you for using IgirePay!");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private static void displayMainMenu() {
        System.out.println("========== IGIREPAY ==========");
        System.out.println("1. Customer Management");
        System.out.println("2. Account Management");
        System.out.println("3. Transactions");
        System.out.println("4. PIN Management");
        System.out.println("5. Loan Management");
        System.out.println("0. Exit");
        System.out.print("Choose a section: ");
    }

    private static void customerManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== CUSTOMER MANAGEMENT ==========");
            System.out.println("1. Register customer");
            System.out.println("2. View customer");
            System.out.println("3. Update customer details");
            System.out.println("9. Back to main menu");
            System.out.print("Choose an option: ");

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        registerCustomer();
                        break;
                    case "2":
                        viewCustomer();
                        break;
                    case "3":
                        updateCustomer();
                        break;
                    case "9":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void accountManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== ACCOUNT MANAGEMENT ==========");
            System.out.println("1. Create wallet account");
            System.out.println("2. Create savings account");
            System.out.println("3. View customer accounts");
            System.out.println("4. View account balance");
            System.out.println("5. Delete inactive account");
            System.out.println("9. Back to main menu");
            System.out.print("Choose an option: ");

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        createAccount("WALLET");
                        break;
                    case "2":
                        createAccount("SAVINGS");
                        break;
                    case "3":
                        viewCustomerAccounts();
                        break;
                    case "4":
                        viewAccountBalance();
                        break;
                    case "5":
                        deleteInactiveAccount();
                        break;
                    case "9":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void transactionMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== TRANSACTIONS ==========");
            System.out.println("1. Deposit money");
            System.out.println("2. Withdraw money");
            System.out.println("3. Transfer money");
            System.out.println("4. View transaction history");
            System.out.println("5. Export transactions to CSV");
            System.out.println("6. View daily transaction summary");
            System.out.println("7. Print customer transaction statement");
            System.out.println("9. Back to main menu");
            System.out.print("Choose an option: ");

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        deposit();
                        break;
                    case "2":
                        withdraw();
                        break;
                    case "3":
                        transfer();
                        break;
                    case "4":
                        viewTransactionHistory();
                        break;
                    case "5":
                        exportTransactions();
                        break;
                    case "6":
                        viewDailySummary();
                        break;
                    case "7":
                        printCustomerStatement();
                        break;
                    case "9":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
    }
    private static void exportTransactions() throws Exception {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        System.out.print("Enter CSV file name (default: transactions.csv): ");
        String fileName = readLine().trim();

        if (fileName.isBlank()) {
            fileName = "transactions.csv";
        }

        transactionDAO.exportTransactionsToCSV(
                java.nio.file.Path.of(fileName));

        System.out.println("Transactions exported to: " + fileName);
    }

    private static void viewDailySummary() throws Exception {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        System.out.print("Enter date (YYYY-MM-DD): ");
        java.time.LocalDate date =
                java.time.LocalDate.parse(readLine().trim());

        String summary =
                transactionDAO.getDailyTransactionSummary(date);

        System.out.println("\n" + summary);
    }

    private static void printCustomerStatement() throws Exception {

        int accountID = readInt("Enter account ID: ");

        Account account = getAuthenticatedAccount(accountID);

        if (account == null) {
            return;
        }

        String statement =
                transactionDAO.getCustomerTransactionStatement(
                        account.getCustomerID());

        System.out.println("\n" + statement);
    }

    private static void pinManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== PIN MANAGEMENT ==========");
            System.out.println("1. Create customer PIN");
            System.out.println("2. Validate customer PIN");
            System.out.println("3. Change customer PIN");
            System.out.println("9. Back to main menu");
            System.out.print("Choose an option: ");

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        createCustomerPin();
                        break;
                    case "2":
                        validateCustomerPin();
                        break;
                    case "3":
                        changeCustomerPin();
                        break;
                    case "9":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void loanManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== LOAN MANAGEMENT ==========");
            System.out.println("1. Request a loan");
            System.out.println("2. Repay a loan");
            System.out.println("3. Check eligible loan limit");
            System.out.println("9. Back to main menu");
            System.out.print("Choose an option: ");

            String choice = readLine().trim();

            try {
                switch (choice) {
                    case "1":
                        requestLoan();
                        break;
                    case "2":
                        repayLoan();
                        break;
                    case "3":
                        checkLoanLimit();
                        break;
                    case "9":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
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