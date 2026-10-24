package uk.gov.dwp.uc.pairtest.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that every error has a message defined and that messages are built correctly from
 * messages.properties.
 */
class InvalidPurchaseExceptionTest {

    @ParameterizedTest
    @EnumSource(PurchaseError.class)
    void everyErrorHasAMessageDefined(PurchaseError error) {
        ResourceBundle messages = ResourceBundle.getBundle("messages");

        assertTrue(messages.containsKey(error.getMessageKey()), "Missing message for " + error);
        assertFalse(messages.getString(error.getMessageKey()).isBlank(), "Blank message for " + error);
    }

    @Test
    void carriesTheErrorAndItsMessage() {
        InvalidPurchaseException exception = new InvalidPurchaseException(PurchaseError.ADULT_REQUIRED);

        assertEquals(PurchaseError.ADULT_REQUIRED, exception.getError());
        assertEquals("Child and infant tickets cannot be purchased without an adult ticket", exception.getMessage());
    }

    @Test
    void fillsInMessageArguments() {
        InvalidPurchaseException exception = new InvalidPurchaseException(PurchaseError.MAX_TICKETS_EXCEEDED, 25);

        assertEquals("Cannot purchase more than 25 tickets at a time", exception.getMessage());
    }

    @Test
    void keepsApostrophesInMessages() {
        InvalidPurchaseException exception = new InvalidPurchaseException(PurchaseError.INFANTS_EXCEED_ADULTS);

        assertEquals("Each infant must sit on an adult's lap, so infants cannot outnumber adults",
                exception.getMessage());
    }
}
