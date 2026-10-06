package com.bank.exception;

/**
 * Checked exception thrown when an operation references an account number that does not exist.
 */
public class AccountNotFoundException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final String accountNo;

    /**
     * Constructs an {@code AccountNotFoundException} for the specified account number.
     *
     * @param accountNo the non-existent account number
     */
    public AccountNotFoundException(String accountNo) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountNo);
        this.accountNo = accountNo;
    }

    /**
     * Constructs an {@code AccountNotFoundException} with an explicit message.
     *
     * @param accountNo the non-existent account number
     * @param message detailed description of the error
     */
    public AccountNotFoundException(String accountNo, String message) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, message);
        this.accountNo = accountNo;
    }

    /**
     * Returns the account number that could not be located.
     *
     * @return the account number
     */
    public String getAccountNo() {
        return accountNo;
    }
}
