package uk.gov.dwp.uc.pairtest.exception;

/**
 * Thrown when a ticket purchase breaks a business rule.
 *
 * <p>The exception carries the {@link PurchaseError} that caused it, so callers can react to the
 * reason without depending on the wording, and a readable message taken from
 * {@code messages.properties}.
 */
public class InvalidPurchaseException extends RuntimeException {

    private final PurchaseError error;

    /**
     * @param error            the reason the purchase was rejected
     * @param messageArguments values for any placeholders such as {@code {0}} in the error's message
     */
    public InvalidPurchaseException(PurchaseError error, Object... messageArguments) {
        super(error.formatMessage(messageArguments));
        this.error = error;
    }

    /**
     * @return the reason the purchase was rejected
     */
    public PurchaseError getError() {
        return error;
    }

}
