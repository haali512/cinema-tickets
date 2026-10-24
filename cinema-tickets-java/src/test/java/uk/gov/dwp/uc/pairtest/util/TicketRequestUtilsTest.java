package uk.gov.dwp.uc.pairtest.util;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

/**
 * Tests the shared counting helpers, including that large values are handled without silent overflow.
 */
class TicketRequestUtilsTest {

    @Test
    void totalsTicketsAcrossAllTypes() {
        long total = TicketRequestUtils.totalTickets(
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 3),
                new TicketTypeRequest(INFANT, 1));

        assertEquals(6, total);
    }

    @Test
    void totalIsZeroWhenThereAreNoRequests() {
        assertEquals(0, TicketRequestUtils.totalTickets());
    }

    @Test
    void totalDoesNotOverflowForVeryLargeRequests() {
        long total = TicketRequestUtils.totalTickets(
                new TicketTypeRequest(ADULT, Integer.MAX_VALUE),
                new TicketTypeRequest(ADULT, Integer.MAX_VALUE));

        assertEquals(2L * Integer.MAX_VALUE, total);
    }

    @Test
    void countsOnlyTicketsOfTheRequestedType() {
        TicketTypeRequest[] requests = {
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 3),
                new TicketTypeRequest(ADULT, 4)
        };

        assertEquals(6, TicketRequestUtils.countTickets(ADULT, requests));
        assertEquals(3, TicketRequestUtils.countTickets(CHILD, requests));
        assertEquals(0, TicketRequestUtils.countTickets(INFANT, requests));
    }

    @Test
    void countFailsRatherThanOverflowing() {
        assertThrows(ArithmeticException.class, () -> TicketRequestUtils.countTickets(ADULT,
                new TicketTypeRequest(ADULT, Integer.MAX_VALUE),
                new TicketTypeRequest(ADULT, 1)));
    }
}
