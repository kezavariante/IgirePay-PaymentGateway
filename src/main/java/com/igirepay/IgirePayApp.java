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

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "IgirePay@123";

    private static String currentRole = "";
    private static int currentCustomerID = -1;
    private static int consecutiveFailedLogins = 0;

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final AccountDAO accountDAO = new AccountDAO();
    private static final TransactionDAO transactionDAO = new TransactionDAO();
    private static final PaymentService paymentService = new PaymentService();
    private static final LoanService loanService = new LoanService();

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            System.out.println("\n========== WELCOME TO IGIREPAY ==========");
            System.out.println("1. Admin");
            System.out.println("2. Customer");
            System.out.println("0. Exit");
            System.out.print("Choose your role: ");

            try {
                switch (readLine().trim()) {
                    case "1":
                        if (adminLogin()) {
                            currentRole = "ADMIN";
                            currentCustomerID = -1;
                            adminMenu();
                            logout();
                        }
                        break;
                    case "2":
                        customerAccessMenu();
                        break;
                    case "0":
                        running = false;
                        System.out.println("Thank you for using IgirePay!");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void customerAccessMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== CUSTOMER ACCESS ==========");
            System.out.println("Do you already have an account?");
            System.out.println("1. Yes (Login)");
            System.out.println("2. No (Register)");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            switch (readLine().trim()) {
                case "1":
                    try {
                        if (loginCustomer()) {
                            currentRole = "CUSTOMER";
                            customerMenu();
                            logout();
                            running = false;
                        }
                    } catch (SQLException e) {
                        System.out.println("Login failed: " + e.getMessage());
                    }
                    break;
                case "2":
                    try {
                        registerCustomer();
                    } catch (SQLException e) {
                        System.out.println("Registration failed: " + e.getMessage());
                    }
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static boolean adminLogin() {
        System.out.print("Admin username: ");
        String username = readLine().trim();
        String password = readPin("Admin password: ");

        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            System.out.println("Admin login successful.");
            return true;
        }

        System.out.println("Invalid admin credentials.");
        return false;
    }

    private static void logout() {
        currentRole = "";
        currentCustomerID = -1;
        System.out.println("You have been logged out.");
    }

    private static void adminMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== ADMIN MENU ==========");
            System.out.println("1. Customer management");
            System.out.println("2. Account management");
            System.out.println("3. Transactions and reports");
            System.out.println("4. PIN management");
            System.out.println("5. Loan management");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> customerManagementMenu();
                    case "2" -> accountManagementMenu();
                    case "3" -> transactionMenu();
                    case "4" -> pinManagementMenu();
                    //case "5" -> adminLoanMenu();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void customerMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== CUSTOMER MENU ==========");
            System.out.println("1. View my profile");
            System.out.println("2. Update my profile");
            System.out.println("3. View my accounts");
            System.out.println("4. Create wallet account");
            System.out.println("5. Create savings account");
            System.out.println("6. View account balance");
            System.out.println("7. Deposit");
            System.out.println("8. Withdraw");
            System.out.println("9. Transfer");
            System.out.println("10. View transactions");
            System.out.println("11. Download / print statement");
            System.out.println("12. Change my PIN");
            System.out.println("13. Loans");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> showMyProfile();
                    case "2" -> updateCustomerForID(currentCustomerID);
                    case "3" -> showMyAccounts();
                    case "4" -> createAccount("WALLET");
                    case "5" -> createAccount("SAVINGS");
                    case "6" -> viewAccountBalance();
                    case "7" -> deposit();
                    case "8" -> withdraw();
                    case "9" -> transfer();
                    case "10" -> viewTransactionHistory();
                    case "11" -> downloadOrPrintStatement();
                    case "12" -> changeCustomerPin();
                    case "13" -> customerLoanMenu();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void customerLoanMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== LOANS ==========");
            System.out.println("1. Request a loan");
            System.out.println("2. Repay a loan");
            System.out.println("3. Check eligible limit");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> requestLoan();
                    case "2" -> repayLoan();
                    case "3" -> checkLoanLimit();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void customerManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== CUSTOMER MANAGEMENT ==========");
            System.out.println("1. Register customer");
            System.out.println("2. View customer");
            System.out.println("3. List all customers");
            System.out.println("4. Update customer details");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1":
                        registerCustomer();
                        break;
                    case "2":
                        Customer customer = customerDAO.getCustomerById(
                                readInt("Enter Customer ID: "));
                        System.out.println(customer == null
                                ? "Customer not found."
                                : customer);
                        break;
                    case "3":
                        listAllCustomers();
                        break;
                    case "4":
                        updateCustomer();
                        break;
                    case "0":
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void accountManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== ACCOUNT MANAGEMENT ==========");
            System.out.println("1. Create wallet");
            System.out.println("2. Create savings account");
            System.out.println("3. View customer accounts");
            System.out.println("4. View all accounts");
            System.out.println("5. View account balance");
            System.out.println("6. Delete inactive account");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> createAccount("WALLET");
                    case "2" -> createAccount("SAVINGS");
                    case "3" -> viewCustomerAccounts();
                    case "4" -> listAllAccounts();
                    case "5" -> viewAccountBalance();
                    case "6" -> deleteInactiveAccount();
                    case "0" -> inMenu = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void transactionMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== TRANSACTIONS AND REPORTS ==========");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. View transaction history");
            System.out.println("5. Export transactions to CSV");
            System.out.println("6. Daily transaction summary");
            System.out.println("7. Customer statement");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> deposit();
                    case "2" -> withdraw();
                    case "3" -> transfer();
                    case "4" -> viewTransactionHistory();
                    case "5" -> exportTransactions();
                    case "6" -> viewDailySummary();
                    case "7" -> printCustomerStatement();
                    case "0" -> inMenu = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static void pinManagementMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========== PIN MANAGEMENT ==========");
            System.out.println("1. Create customer PIN");
            System.out.println("2. Validate customer PIN");
            System.out.println("3. Change customer PIN");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            try {
                switch (readLine().trim()) {
                    case "1" -> createCustomerPin();
                    case "2" -> validateCustomerPin();
                    case "3" -> changeCustomerPin();
                    case "0" -> inMenu = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private static boolean registerCustomer() throws SQLException {
        System.out.println("\n========== CUSTOMER REGISTRATION ==========");

        System.out.print("Full name: ");
        String name = readLine().trim();
        System.out.print("Email: ");
        String email = readLine().trim();
        System.out.print("Phone number: ");
        String phone = readLine().trim();

        String pin = readPin("Create a 4-6 digit PIN: ");
        String confirmPin = readPin("Confirm PIN: ");

        if (!isValidPinFormat(pin)) {
            System.out.println("PIN must contain 4 to 6 digits.");
            return false;
        }
        if (!pin.equals(confirmPin)) {
            System.out.println("PINs do not match.");
            return false;
        }
        if (!customerDAO.isPinAvailable(pin)) {
            System.out.println("That PIN is already in use. Choose another.");
            return false;
        }

        try {
            Customer customer = new Customer(name, email, phone);
            int customerID = customerDAO.addCustomer(customer, pin);

            System.out.println("Registration successful!");
            System.out.println("Your customer ID is: " + customerID);
            System.out.println("Log in with the PIN you just created.");

            if ("ADMIN".equals(currentRole)) {
                return true;
            }

            currentCustomerID = customerID;
            System.out.println("Welcome, " + name + "!");
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid details: " + e.getMessage());
            return false;
        }
    }

    private static boolean loginCustomer() throws SQLException {
        System.out.println("\n========== CUSTOMER LOGIN ==========");

        String pin = readPin("Enter your PIN: ");
        int customerID = customerDAO.getCustomerIDByPin(pin);

        if (customerID == -1) {
            System.out.println("Invalid PIN. Login failed.");
            return false;
        }

        Customer customer = customerDAO.getCustomerById(customerID);
        if (customer == null) {
            System.out.println("Customer not found.");
            return false;
        }

        currentCustomerID = customerID;
        System.out.println("Login successful!");
        System.out.println("Welcome, " + customer.getFullName() + "!");
        return true;
    }

    private static void showMyProfile() throws SQLException {
        Customer customer = customerDAO.getCustomerById(currentCustomerID);
        System.out.println(customer == null ? "Customer not found." : customer);
    }

    private static void showMyAccounts() throws SQLException {
        printAccounts(currentCustomerID);
    }

    private static void listAllCustomers() throws SQLException {
        List<Customer> customers = customerDAO.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        customers.forEach(System.out::println);
    }

    private static void listAllAccounts() throws SQLException {
        List<Account> accounts = accountDAO.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        accounts.forEach(System.out::println);
    }

    private static void updateCustomer() throws SQLException {
        int id = requestedCustomerID();
        if (id <= 0 || !authenticateCustomer(id)) {
            return;
        }
        updateCustomerForID(id);
    }

    private static int requestedCustomerID() {
        if ("CUSTOMER".equals(currentRole)) {
            return currentCustomerID;
        }
        return readInt("Enter customer ID: ");
    }

    private static void updateCustomerForID(int id) throws SQLException {
        Customer customer = customerDAO.getCustomerById(id);
        if (customer == null) {
            System.out.println("Customer not found.");
            return;
        }

        System.out.println("Current details: " + customer);
        System.out.print("New full name: ");
        String name = readLine().trim();
        System.out.print("New email: ");
        String email = readLine().trim();
        System.out.print("New phone number: ");
        String phone = readLine().trim();

        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            System.out.println("All fields are required.");
            return;
        }

        customer.setFullName(name);
        customer.setEmail(email);
        customer.setPhoneNumber(phone);

        System.out.println(customerDAO.updateCustomer(customer, id)
                ? "Customer updated."
                : "Customer was not updated.");
    }

    private static void createAccount(String type) throws SQLException {
        int customerID = requestedCustomerID();
        if (!authenticateCustomer(customerID)) {
            return;
        }
        if (customerDAO.getCustomerById(customerID) == null) {
            System.out.println("Customer not found.");
            return;
        }

        BigDecimal balance = readAmount("Initial balance: ", true);
        Account account = "WALLET".equals(type)
                ? new WalletAccount(1, "TEMP", balance, customerID)
                : new SavingsAccount(1, "TEMP", balance, customerID);

        int id = accountDAO.addAccount(account);
        System.out.println(type + " account created. Account ID: " + id);
        System.out.println("Account number: ACC-" + id);
    }

    private static void viewCustomerAccounts() throws SQLException {
        int id = requestedCustomerID();
        if (!authenticateCustomer(id)) {
            return;
        }
        printAccounts(id);
    }

    private static void printAccounts(int id) throws SQLException {
        List<Account> accounts = accountDAO.getAccountsByCustomerId(id);
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        accounts.forEach(System.out::println);
    }

    private static void viewAccountBalance() throws SQLException {
        Account account = getAuthenticatedAccount(readInt("Enter account ID: "));
        if (account == null) {
            return;
        }

        System.out.println("Type: " + account.getAccountType());
        System.out.println("Number: " + account.getAccountNumber());
        System.out.println("Balance: " + account.getBalance() + " RWF");
    }

    private static void deleteInactiveAccount() throws SQLException {
        Account account = getAuthenticatedAccount(readInt("Account ID to delete: "));
        if (account == null) {
            return;
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            System.out.println("Only accounts with zero balance can be deleted.");
            return;
        }

        System.out.print("Confirm deletion (yes/no): ");
        if (!readLine().trim().equalsIgnoreCase("yes")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        try {
            System.out.println(accountDAO.deleteAccount(account.getAccountID())
                    ? "Account deleted."
                    : "Account was not deleted.");
        } catch (SQLException e) {
            System.out.println("Cannot delete this account; it may have linked transactions.");
        }
    }

    private static void deposit() throws SQLException {
        int id = readInt("Account ID: ");
        if (getAuthenticatedAccount(id) == null) {
            return;
        }

        BigDecimal amount = readAmount("Deposit amount: ", false);
        Transaction transaction = new Transaction(
                generateReferenceID(), amount, "DEPOSIT", LocalDateTime.now());

        if (paymentService.processPayment(id, transaction)) {
            System.out.println("Deposit successful.");
            displayBalance(id);
        } else {
            System.out.println("Payment failed or was already processed.");
        }
    }

    private static void withdraw() throws SQLException {
        int id = readInt("Account ID: ");
        if (getAuthenticatedAccount(id) == null) {
            return;
        }

        BigDecimal amount = readAmount("Withdrawal amount: ", false);
        Transaction transaction = new Transaction(
                generateReferenceID(), amount, "WITHDRAW", LocalDateTime.now());

        if (paymentService.processPayment(id, transaction)) {
            System.out.println("Withdrawal successful.");
            displayBalance(id);
        } else {
            System.out.println("Withdrawal failed or was already processed.");
        }
    }

    private static void transfer() throws SQLException {
        int sourceID = readInt("Sender account ID: ");
        if (getAuthenticatedAccount(sourceID) == null) {
            return;
        }

        int destinationID = readInt("Receiver account ID: ");
        if (sourceID == destinationID) {
            System.out.println("The two accounts must be different.");
            return;
        }
        if (accountDAO.getAccountById(destinationID) == null) {
            System.out.println("Receiver account not found.");
            return;
        }

        BigDecimal amount = readAmount("Transfer amount: ", false);
        String reference = generateReferenceID();

        if (paymentService.transfer(sourceID, destinationID, amount, reference)) {
            System.out.println("Transfer successful. Reference: " + reference);
            displayBalance(sourceID);
        } else {
            System.out.println("Transfer failed or was already processed.");
        }
    }

    private static void viewTransactionHistory() throws SQLException {
        int id = readInt("Account ID: ");
        if (getAuthenticatedAccount(id) == null) {
            return;
        }

        List<Transaction> transactions = transactionDAO.getTransactionsByAccountId(id);
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        transactions.forEach(System.out::println);
    }

    private static void exportTransactions() throws Exception {
        if (!"ADMIN".equals(currentRole)) {
            System.out.println("Admin access required.");
            return;
        }

        System.out.print("CSV file name (default transactions.csv): ");
        String name = readLine().trim();
        if (name.isBlank()) {
            name = "transactions.csv";
        }

        transactionDAO.exportTransactionsToCSV(java.nio.file.Path.of(name));
        System.out.println("Exported to " + name);
    }

    private static void viewDailySummary() throws Exception {
        if (!"ADMIN".equals(currentRole)) {
            System.out.println("Admin access required.");
            return;
        }

        System.out.print("Date (YYYY-MM-DD): ");
        java.time.LocalDate date = java.time.LocalDate.parse(readLine().trim());
        System.out.println(transactionDAO.getDailyTransactionSummary(date));
    }

    private static void printCustomerStatement() throws Exception {
        int accountID = readInt("Account ID: ");
        Account account = getAuthenticatedAccount(accountID);
        if (account == null) {
            return;
        }
        System.out.println(transactionDAO.getCustomerTransactionStatement(
                account.getCustomerID()));
    }

    private static void downloadOrPrintStatement() throws Exception {
        System.out.println("\n========== STATEMENT ==========");
        System.out.println("1. Print on screen");
        System.out.println("2. Download to file");
        System.out.print("Choose an option: ");

        String choice = readLine().trim();
        String statement = transactionDAO.getCustomerTransactionStatement(currentCustomerID);

        if ("2".equals(choice)) {
            String fileName = "statement-" + currentCustomerID + ".txt";
            transactionDAO.exportCustomerStatement(
                    currentCustomerID, java.nio.file.Path.of(fileName));
            System.out.println("Statement saved to " + fileName);
        } else {
            System.out.println(statement);
        }
    }

    private static void createCustomerPin() throws SQLException {
        if (!"ADMIN".equals(currentRole)) {
            System.out.println("Only an admin can initialize a customer PIN.");
            return;
        }

        int id = readInt("Customer ID: ");
        if (customerDAO.getCustomerById(id) == null) {
            System.out.println("Customer not found.");
            return;
        }

        String pin = readPin("New 4-6 digit PIN: ");
        if (!isValidPinFormat(pin)) {
            System.out.println("PIN must contain 4 to 6 digits.");
            return;
        }
        if (!pin.equals(readPin("Confirm PIN: "))) {
            System.out.println("PINs do not match.");
            return;
        }
        if (!customerDAO.isPinAvailable(pin)) {
            System.out.println("That PIN is already in use. Choose another.");
            return;
        }

        System.out.println(customerDAO.createPin(id, pin)
                ? "PIN created."
                : "PIN creation failed; a PIN may already exist.");
    }

    private static void validateCustomerPin() throws SQLException {
        int id = readInt("Customer ID: ");
        String pin = readPin("PIN: ");
        System.out.println(customerDAO.validatePin(id, pin)
                ? "PIN is valid."
                : "PIN is invalid or not set.");
    }

    private static void changeCustomerPin() throws SQLException {
        int id = requestedCustomerID();
        if (!authenticateCustomer(id)) {
            return;
        }

        String oldPin = readPin("Current PIN: ");
        String newPin = readPin("New 4-6 digit PIN: ");

        if (!isValidPinFormat(newPin)) {
            System.out.println("PIN must contain 4 to 6 digits.");
            return;
        }
        if (!newPin.equals(readPin("Confirm new PIN: "))) {
            System.out.println("PINs do not match.");
            return;
        }
        if (!customerDAO.isPinAvailable(newPin)) {
            System.out.println("That PIN is already in use. Choose another.");
            return;
        }

        System.out.println(customerDAO.changePin(id, oldPin, newPin)
                ? "PIN changed."
                : "PIN change failed. Check your current PIN.");
    }

    private static void requestLoan() throws SQLException {
        int customerID = requestedCustomerID();
        if (!authenticateCustomer(customerID)) {
            return;
        }

        int accountID = readInt("Account ID: ");
        Account account = accountDAO.getAccountById(accountID);
        if (account == null || account.getCustomerID() != customerID) {
            System.out.println("That account does not belong to this customer.");
            return;
        }

        BigDecimal amount = readAmount("Loan amount (RWF): ", false);
        System.out.println(loanService.requestLoan(customerID, accountID, amount));
    }

    private static void repayLoan() throws SQLException {
        int id = requestedCustomerID();
        if (!authenticateCustomer(id)) {
            return;
        }
        System.out.println(loanService.repayLoan(id));
    }

    private static void checkLoanLimit() throws SQLException {
        int id = requestedCustomerID();
        if (!authenticateCustomer(id)) {
            return;
        }
        System.out.println("Eligible loan limit: "
                + loanService.getEligibleLimit(id) + " RWF");
    }

    private static void displayBalance(int id) throws SQLException {
        Account account = accountDAO.getAccountById(id);
        if (account != null) {
            System.out.println("Account " + id + " balance: "
                    + account.getBalance() + " RWF");
        }
    }

    private static boolean authenticateCustomer(int customerID) {
        if ("ADMIN".equals(currentRole)) {
            return true;
        }
        if (!"CUSTOMER".equals(currentRole) || customerID != currentCustomerID) {
            System.out.println("Access denied: this is not your customer account.");
            return false;
        }
        return true;
    }

    private static Account getAuthenticatedAccount(int accountID) throws SQLException {
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
            try {
                int number = Integer.parseInt(readLine().trim());
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a positive whole number.");
        }
    }

    private static BigDecimal readAmount(String prompt, boolean allowZero) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal amount = new BigDecimal(readLine().trim());
                if (amount.scale() > 2) {
                    System.out.println("Use at most two decimal places.");
                    continue;
                }

                int comparison = amount.compareTo(BigDecimal.ZERO);
                if (allowZero ? comparison >= 0 : comparison > 0) {
                    return amount;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println(allowZero
                    ? "Amount cannot be negative."
                    : "Amount must be greater than zero.");
        }
    }

    private static String generateReferenceID() {
        return UUID.randomUUID().toString();
    }
}
