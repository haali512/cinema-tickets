package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.PurchaseError;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

/**
 * Tests that valid purchases are charged and seated correctly, and that rejected purchases never
 * reach the payment or seat reservation services. Mocks stand in for both third-party services.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final long ACCOUNT_ID = 1L;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Nested
    class ValidPurchases {

        @Test
        void chargesAndReservesASeatForASingleAdult() {
            ticketService.purchaseTickets(ACCOUNT_ID, adults(1));

            verifyPaymentAndReservation(25, 1);
        }

        @Test
        void chargesAndReservesSeatsForAMixOfTicketTypes() {
            ticketService.purchaseTickets(ACCOUNT_ID, adults(2), children(3), infants(1));

            verifyPaymentAndReservation(2 * 25 + 3 * 15, 5);
        }

        @Test
        void infantsAreFreeAndNotAllocatedASeat() {
            ticketService.purchaseTickets(ACCOUNT_ID, adults(2), infants(2));

            verifyPaymentAndReservation(50, 2);
        }

        @Test
        void combinesSeparateRequestsForTheSameTicketType() {
            ticketService.purchaseTickets(ACCOUNT_ID, adults(1), children(1), adults(2), children(1));

            verifyPaymentAndReservation(3 * 25 + 2 * 15, 5);
        }

        @Test
        void chargesForExactlyTheMaximumOfTwentyFiveTickets() {
            ticketService.purchaseTickets(ACCOUNT_ID, adults(10), children(10), infants(5));

            verifyPaymentAndReservation(10 * 25 + 10 * 15, 20);
        }

        @ParameterizedTest(name = "{0} adults, {1} children, {2} infants -> £{3} for {4} seats")
        @CsvSource({
                "1, 0, 0, 25, 1",
                "1, 1, 0, 40, 2",
                "1, 0, 1, 25, 1",
                "1, 1, 1, 40, 2",
                "2, 5, 2, 125, 7",
                "25, 0, 0, 625, 25",
                "1, 24, 0, 385, 25",
                "13, 0, 12, 325, 13"
        })
        void calculatesTheCorrectAmountAndSeats(int adultCount, int childCount, int infantCount,
                                                int expectedAmount, int expectedSeats) {
            ticketService.purchaseTickets(ACCOUNT_ID, requestsFor(adultCount, childCount, infantCount));

            verifyPaymentAndReservation(expectedAmount, expectedSeats);
        }

        // Checks the exact amount and seats requested, and that nothing else was asked of either service.
        private void verifyPaymentAndReservation(int expectedAmount, int expectedSeats) {
            verify(ticketPaymentService).makePayment(ACCOUNT_ID, expectedAmount);
            verify(seatReservationService).reserveSeat(ACCOUNT_ID, expectedSeats);
            verifyNoMoreInteractions(ticketPaymentService, seatReservationService);
        }

        // Builds the requests for a table row, leaving out any type with a count of zero.
        private TicketTypeRequest[] requestsFor(int adultCount, int childCount, int infantCount) {
            return Stream.of(adults(adultCount), children(childCount), infants(infantCount))
                    .filter(request -> request.getNoOfTickets() > 0)
                    .toArray(TicketTypeRequest[]::new);
        }
    }

    @Nested
    class InvalidPurchases {

        // The rules themselves are covered in TicketPurchaseValidatorTest. These tests make sure the
        // service stops a rejected purchase before it reaches the payment or reservation services.
        static Stream<Arguments> invalidPurchases() {
            return Stream.of(
                    Arguments.of(PurchaseError.INVALID_ACCOUNT_ID, 0L, new TicketTypeRequest[]{adults(1)}),
                    Arguments.of(PurchaseError.NO_TICKETS_REQUESTED, ACCOUNT_ID, new TicketTypeRequest[]{}),
                    Arguments.of(PurchaseError.INVALID_TICKET_QUANTITY, ACCOUNT_ID, new TicketTypeRequest[]{adults(-1)}),
                    Arguments.of(PurchaseError.MAX_TICKETS_EXCEEDED, ACCOUNT_ID, new TicketTypeRequest[]{adults(26)}),
                    Arguments.of(PurchaseError.ADULT_REQUIRED, ACCOUNT_ID, new TicketTypeRequest[]{children(1)}),
                    Arguments.of(PurchaseError.INFANTS_EXCEED_ADULTS, ACCOUNT_ID, new TicketTypeRequest[]{adults(1), infants(2)})
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidPurchases")
        void neitherChargesNorReservesSeatsForAnInvalidPurchase(PurchaseError expectedError, Long accountId,
                                                                 TicketTypeRequest[] requests) {
            InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(accountId, requests));

            assertEquals(expectedError, exception.getError());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }
    }

    @Test
    void requiresAPaymentService() {
        assertThrows(NullPointerException.class, () -> new TicketServiceImpl(null, seatReservationService));
    }

    @Test
    void requiresASeatReservationService() {
        assertThrows(NullPointerException.class, () -> new TicketServiceImpl(ticketPaymentService, null));
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
