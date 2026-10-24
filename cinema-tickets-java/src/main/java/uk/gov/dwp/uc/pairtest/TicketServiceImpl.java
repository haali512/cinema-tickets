package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.validation.TicketPurchaseValidator;

import java.util.Objects;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;
import static uk.gov.dwp.uc.pairtest.util.TicketRequestUtils.countTickets;

/**
 * Purchases cinema tickets for an account.
 *
 * Each purchase is validated first. Only a valid purchase is charged through the
 * TicketPaymentService and has its seats reserved through the SeatReservationService,
 * so a rejected purchase never takes money or holds seats.
 */
public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    // Prices are in whole pounds, matching the int amount the payment service expects.
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    // Stateless, so a single instance can safely be shared across purchases.
    private final TicketPurchaseValidator ticketPurchaseValidator = new TicketPurchaseValidator();

    /**
     * @param ticketPaymentService   takes payment for valid purchases
     * @param seatReservationService reserves seats for valid purchases
     * @throws NullPointerException if either service is null
     */
    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "ticketPaymentService must not be null");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService must not be null");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        // Throws InvalidPurchaseException before either external service is called.
        ticketPurchaseValidator.validate(accountId, ticketTypeRequests);

        // Safe to count as ints now: the validator has capped the total at 25 tickets.
        int adults = countTickets(ADULT, ticketTypeRequests);
        int children = countTickets(CHILD, ticketTypeRequests);
        int infants = countTickets(INFANT, ticketTypeRequests);

        ticketPaymentService.makePayment(accountId, calculateTotalAmount(adults, children, infants));
        seatReservationService.reserveSeat(accountId, calculateSeatsToReserve(adults, children));
    }

    private int calculateTotalAmount(int adults, int children, int infants) {
        // Infants are free, but kept in the sum so the pricing rule stays visible in one place.
        return adults * ADULT_TICKET_PRICE
                + children * CHILD_TICKET_PRICE
                + infants * INFANT_TICKET_PRICE;
    }

    private int calculateSeatsToReserve(int adults, int children) {
        // Infants sit on an adult's lap, so they are not allocated a seat.
        return adults + children;
    }

}
