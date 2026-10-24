package uk.gov.dwp.uc.pairtest.exception;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Every reason a ticket purchase can be rejected. The wording for each one lives in
 * {@code messages.properties} under the key held by the constant, so messages can be changed
 * without touching the code.
 */
public enum PurchaseError {

    INVALID_ACCOUNT_ID("purchase.error.account.invalid"),
    NO_TICKETS_REQUESTED("purchase.error.request.empty"),
    NULL_TICKET_REQUEST("purchase.error.request.null"),
    MISSING_TICKET_TYPE("purchase.error.type.missing"),
    INVALID_TICKET_QUANTITY("purchase.error.quantity.invalid"),
    MAX_TICKETS_EXCEEDED("purchase.error.quantity.maximum"),
    ADULT_REQUIRED("purchase.error.adult.required"),
    INFANTS_EXCEED_ADULTS("purchase.error.infant.lap");

    // Loaded once from the classpath (src/main/resources/messages.properties)
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("messages");

    private final String messageKey;

    PurchaseError(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * @return the key of this error's message in {@code messages.properties}
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * Builds the readable message for this error.
     *
     * @param arguments values substituted into placeholders such as {@code {0}}
     * @return the formatted message
     */
    public String formatMessage(Object... arguments) {
        return MessageFormat.format(MESSAGES.getString(messageKey), arguments);
    }

}
