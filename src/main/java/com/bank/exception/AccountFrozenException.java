package com.bank.exception;

/**
 * Checked exception thrown when an operation is attempted on a frozen or suspended bank account.
 */
public class AccountFrozenException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final String accountNo;

    /**
     * Constructs an {@code AccountFrozenException} for the specified account number.
     *
     * @param accountNo the frozen account number
     */
    public AccountFrozenException(String accountNo) {
        super(ErrorCode.ACCOUNT_FROZEN, "Account " + accountNo + " is frozen. Transactions are suspended.");
        this.accountNo = accountNo;
    }

    /**
     * Constructs an {@code AccountFrozenException} with a custom message.
     *
     * @param accountNo the frozen account number
     * @param message detailed description of the violation
     */
    public AccountFrozenException(String accountNo, String message) {
        super(ErrorCode.ACCOUNT_FROZEN, message);
        this.accountNo = accountNo;
    }

    /**
     * Returns the account number of the frozen account.
     *
     * @return the frozen account number
     */
    public String getAccountNo() {
        return accountNo;
    }
}
