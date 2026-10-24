package uk.gov.dwp.uc.pairtest.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.PurchaseError;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

/**
 * Tests each business rule on its own. Rejections are checked by {@link PurchaseError} rather than by
 * message text, so rewording a message in messages.properties does not break these tests.
 */
class TicketPurchaseValidatorTest {

    private static final long ACCOUNT_ID = 1L;

    private final TicketPurchaseValidator validator = new TicketPurchaseValidator();

    @Nested
    class ValidPurchases {

        @Test
        void acceptsASingleAdult() {
            assertValid(ACCOUNT_ID, adults(1));
        }

        @Test
        void acceptsAdultsWithChildrenAndInfants() {
            assertValid(ACCOUNT_ID, adults(2), children(3), infants(2));
        }

        @Test
        void acceptsExactlyTheMaximumNumberOfTickets() {
            assertValid(ACCOUNT_ID, adults(10), children(10), infants(5));
        }

        @Test
        void acceptsAsManyInfantsAsAdults() {
            assertValid(ACCOUNT_ID, adults(3), infants(3));
        }

        @Test
        void acceptsTheSmallestValidAccountId() {
            assertValid(1L, adults(1));
        }

        @Test
        void acceptsAnAdultRequestedAfterChildren() {
            assertValid(ACCOUNT_ID, children(1), infants(1), adults(1));
        }

        private void assertValid(Long accountId, TicketTypeRequest... requests) {
            assertDoesNotThrow(() -> validator.validate(accountId, requests));
        }
    }

    @Nested
    class AccountId {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
        void rejectsAccountIdsThatAreNotGreaterThanZero(Long accountId) {
            assertRejected(PurchaseError.INVALID_ACCOUNT_ID, accountId, adults(1));
        }
    }

    @Nested
    class TicketRequests {

        @Test
        void rejectsANullRequestArray() {
            assertRejected(PurchaseError.NO_TICKETS_REQUESTED, ACCOUNT_ID, (TicketTypeRequest[]) null);
        }

        @Test
        void rejectsAPurchaseWithNoRequests() {
            assertRejected(PurchaseError.NO_TICKETS_REQUESTED, ACCOUNT_ID);
        }

        @Test
        void rejectsANullRequest() {
            assertRejected(PurchaseError.NULL_TICKET_REQUEST, ACCOUNT_ID, adults(1), null);
        }

        @Test
        void rejectsARequestWithoutATicketType() {
            assertRejected(PurchaseError.MISSING_TICKET_TYPE, ACCOUNT_ID, adults(1), new TicketTypeRequest(null, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        void rejectsARequestThatIsNotForAtLeastOneTicket(int noOfTickets) {
            assertRejected(PurchaseError.INVALID_TICKET_QUANTITY, ACCOUNT_ID,
                    adults(1), new TicketTypeRequest(CHILD, noOfTickets));
        }

        @Test
        void rejectsANegativeRequestEvenWhenItWouldBringTheTotalUnderTheMaximum() {
            assertRejected(PurchaseError.INVALID_TICKET_QUANTITY, ACCOUNT_ID, adults(30), adults(-10));
        }
    }

    @Nested
    class MaximumTickets {

        @Test
        void rejectsMoreThanTwentyFiveAdults() {
            assertRejected(PurchaseError.MAX_TICKETS_EXCEEDED, ACCOUNT_ID, adults(26));
        }

        @Test
        void rejectsMoreThanTwentyFiveTicketsAcrossTypes() {
            assertRejected(PurchaseError.MAX_TICKETS_EXCEEDED, ACCOUNT_ID, adults(10), children(10), adults(6));
        }

        @Test
        void countsInfantsTowardsTheMaximumEvenThoughTheyHaveNoSeat() {
            assertRejected(PurchaseError.MAX_TICKETS_EXCEEDED, ACCOUNT_ID, adults(13), infants(13));
        }

        // Summed as ints these would wrap round to 0, which would wrongly pass the maximum check.
        @Test
        void rejectsRequestsWhoseTotalWouldOverflowAnInteger() {
            assertRejected(PurchaseError.MAX_TICKETS_EXCEEDED, ACCOUNT_ID,
                    adults(Integer.MAX_VALUE), adults(Integer.MAX_VALUE), adults(2));
        }

        @Test
        void includesTheMaximumInTheMessage() {
            InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                    () -> validator.validate(ACCOUNT_ID, adults(26)));

            assertEquals("Cannot purchase more than 25 tickets at a time", exception.getMessage());
        }
    }

    @Nested
    class AdultSupervision {

        @Test
        void rejectsChildrenWithoutAnAdult() {
            assertRejected(PurchaseError.ADULT_REQUIRED, ACCOUNT_ID, children(2));
        }

        @Test
        void rejectsInfantsWithoutAnAdult() {
            assertRejected(PurchaseError.ADULT_REQUIRED, ACCOUNT_ID, infants(1));
        }

        @Test
        void rejectsChildrenAndInfantsWithoutAnAdult() {
            assertRejected(PurchaseError.ADULT_REQUIRED, ACCOUNT_ID, children(1), infants(1));
        }

        @Test
        void rejectsMoreInfantsThanAdults() {
            assertRejected(PurchaseError.INFANTS_EXCEED_ADULTS, ACCOUNT_ID, adults(1), infants(2));
        }
    }

    private void assertRejected(PurchaseError expectedError, Long accountId, TicketTypeRequest... requests) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(accountId, requests));

        assertEquals(expectedError, exception.getError());
    }

    private static TicketTypeRequest adults(int count) {
        return new TicketTypeRequest(ADULT, count);
    }

    private static TicketTypeRequest children(int count) {
        return new TicketTypeRequest(CHILD, count);
    }

    private static TicketTypeRequest infants(int count) {
        return new TicketTypeRequest(INFANT, count);
    }
}
