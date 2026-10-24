package uk.gov.dwp.uc.pairtest.util;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

import java.util.Arrays;

/**
 * Helpers for adding up ticket requests. They expect non-null requests with a ticket type,
 * which {@link uk.gov.dwp.uc.pairtest.validation.TicketPurchaseValidator} guarantees.
 */
public final class TicketRequestUtils {

    private TicketRequestUtils() {
    }

    /**
     * Returns the total number of tickets across all requests. The sum is a long so that
     * very large requests cannot overflow and wrap round to a small number.
     */
    public static long totalTickets(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests)
                .mapToLong(TicketTypeRequest::getNoOfTickets)
                .sum();
    }

    /**
     * Returns the number of tickets of the given type, combining separate requests for that type.
     *
     * @throws ArithmeticException if the count does not fit in an int
     */
    public static int countTickets(Type type, TicketTypeRequest... ticketTypeRequests) {
        long count = Arrays.stream(ticketTypeRequests)
                .filter(request -> request.getTicketType() == type)
                .mapToLong(TicketTypeRequest::getNoOfTickets)
                .sum();

        return Math.toIntExact(count);
    }

}
