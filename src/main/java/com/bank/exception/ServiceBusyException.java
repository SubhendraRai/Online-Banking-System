package com.bank.exception;

/**
 * Checked exception thrown when an operation cannot be serviced due to lock acquisition timeouts,
 * high transaction concurrency contention, or temporary service saturation.
 */
public class ServiceBusyException extends BankingException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code ServiceBusyException} with a default service busy message.
     */
    public ServiceBusyException() {
        super(ErrorCode.SERVICE_BUSY);
    }

    /**
     * Constructs a {@code ServiceBusyException} with an explicit explanation.
     *
     * @param message detailed description of the busy condition
     */
    public ServiceBusyException(String message) {
        super(ErrorCode.SERVICE_BUSY, message);
    }

    /**
     * Constructs a {@code ServiceBusyException} with an explicit explanation and root cause.
     *
     * @param message detailed description of the busy condition
     * @param cause root cause (e.g. an {@link InterruptedException})
     */
    public ServiceBusyException(String message, Throwable cause) {
        super(ErrorCode.SERVICE_BUSY, message, cause);
    }
}
