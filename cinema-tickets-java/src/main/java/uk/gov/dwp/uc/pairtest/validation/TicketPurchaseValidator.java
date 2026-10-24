package uk.gov.dwp.uc.pairtest.validation;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.PurchaseError;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;
import static uk.gov.dwp.uc.pairtest.util.TicketRequestUtils.countTickets;
import static uk.gov.dwp.uc.pairtest.util.TicketRequestUtils.totalTickets;

/**
 * Checks a ticket purchase against the business rules before any payment or reservation is made.
 *
 * The checks run in a fixed order and stop at the first failure. The structural checks
 * (account and individual requests) run first, so the later rule checks can rely on every
 * request having a type and a positive number of tickets.
 */
public class TicketPurchaseValidator {

    /** The most tickets of any type, infants included, that can be bought in one purchase. */
    public static final int MAX_TICKETS_PER_PURCHASE = 25;

    /**
     * Validates the whole purchase.
     *
     * @param accountId          the purchasing account; must be greater than zero
     * @param ticketTypeRequests the tickets being bought
     * @throws InvalidPurchaseException describing the first rule the purchase breaks
     */
    public void validate(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        validateAccountId(accountId);
        validateEachRequest(ticketTypeRequests);
        validateTotalTicketCount(ticketTypeRequests);
        validateAdultSupervision(ticketTypeRequests);
    }

    private void validateAccountId(Long accountId) {
        // Only ids greater than zero are valid accounts.
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(PurchaseError.INVALID_ACCOUNT_ID);
        }
    }

    private void validateEachRequest(TicketTypeRequest[] ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(PurchaseError.NO_TICKETS_REQUESTED);
        }

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException(PurchaseError.NULL_TICKET_REQUEST);
            }
            if (request.getTicketType() == null) {
                throw new InvalidPurchaseException(PurchaseError.MISSING_TICKET_TYPE);
            }
            // Rejecting negatives stops a request like (ADULT, -10) cancelling out others to dodge
            // the maximum or reduce the price.
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException(PurchaseError.INVALID_TICKET_QUANTITY);
            }
        }
    }

    private void validateTotalTicketCount(TicketTypeRequest[] ticketTypeRequests) {
        // Infants count towards the maximum: they need a ticket even though they don't take a seat.
        if (totalTickets(ticketTypeRequests) > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(PurchaseError.MAX_TICKETS_EXCEEDED, MAX_TICKETS_PER_PURCHASE);
        }
    }

    private void validateAdultSupervision(TicketTypeRequest[] ticketTypeRequests) {
        int adults = countTickets(ADULT, ticketTypeRequests);
        int infants = countTickets(INFANT, ticketTypeRequests);

        // Every request is for at least one ticket, so no adults means only children and infants were requested.
        if (adults == 0) {
            throw new InvalidPurchaseException(PurchaseError.ADULT_REQUIRED);
        }
        // Infants sit on an adult's lap, and each adult has only one lap.
        if (infants > adults) {
            throw new InvalidPurchaseException(PurchaseError.INFANTS_EXCEED_ADULTS);
        }
    }

}
