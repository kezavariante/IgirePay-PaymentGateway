
package com.igirepay;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    private int customerID;
    private String fullName;
    private String email;
    private String phoneNumber;
    private List<Account> accounts;

    public Customer(int customerID, String fullName,
                    String email, String phoneNumber) {
        setCustomerID(customerID);
        setFullName(fullName);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        this.accounts = new ArrayList<>();
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        if (customerID <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }
        this.customerID = customerID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        this.phoneNumber = phoneNumber;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        if (accounts == null) {
            throw new IllegalArgumentException("Accounts list is required");
        }
        this.accounts = accounts;
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account is required");
        }

        if (account.getCustomerID() != customerID) {
            throw new IllegalArgumentException(
                    "Account belongs to a different customer");
        }

        accounts.add(account);
    }

    @Override
    public String toString() {
        return "Customer ID: " + customerID
                + ", Name: " + fullName
                + ", Email: " + email
                + ", Phone: " + phoneNumber
                + ", Number of accounts: " + accounts.size();
    }
}
