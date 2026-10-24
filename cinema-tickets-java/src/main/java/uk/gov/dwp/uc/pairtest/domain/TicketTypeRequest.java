package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 *
 * A request for a number of tickets of a single type. The class is final and its fields are
 * final, so a request cannot change after it has been validated.
 */

public final class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT, CHILD , INFANT
    }

}
