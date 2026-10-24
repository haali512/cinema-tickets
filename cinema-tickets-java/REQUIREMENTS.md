# Cinema Tickets – Coding Exercise Requirements
---

## Table of Contents

- [Objective](#objective)
- [Business Rules](#business-rules)
- [Ticket Prices](#ticket-prices)
- [Constraints](#constraints)
- [Assumptions](#assumptions)
- [Your Task](#your-task)
- [Submission Instructions](#submission-instructions)

---

## Objective

This is a coding exercise which will allow you to demonstrate how you code and your approach to a given problem.

You will be assessed on:

1. Your ability to write clean, well-tested and reusable code.
2. How you have ensured the following business rules are correctly met.

---

## Business Rules

1. There are 3 types of tickets i.e. **Infant**, **Child**, and **Adult**.
2. The ticket prices are based on the type of ticket (see [Ticket Prices](#ticket-prices)).
3. The ticket purchaser declares how many and what type of tickets they want to buy.
4. Multiple tickets can be purchased at any given time.
5. Only a maximum of **25 tickets** can be purchased at a time.
6. Infants do not pay for a ticket and are not allocated a seat. They will be sitting on an Adult's lap.
7. Child and Infant tickets cannot be purchased without purchasing an Adult ticket.
8. There is an existing `TicketPaymentService` responsible for taking payments.
9. There is an existing `SeatReservationService` responsible for reserving seats.

---

## Ticket Prices

| Ticket Type | Price |
|-------------|-------|
| INFANT      | £0    |
| CHILD       | £15   |
| ADULT       | £25   |

---

## Constraints

1. The `TicketService` interface **CANNOT** be modified.
2. The code in the `thirdparty.*` packages **CANNOT** be modified.
3. The `TicketTypeRequest` **SHOULD** be an immutable object.

---

## Assumptions

You can assume:

1. All accounts with an id greater than zero are valid. They also have sufficient funds to pay for any number of tickets.
2. The `TicketPaymentService` implementation is an external provider with no defects. You do not need to worry about how the actual payment happens.
3. The payment will always go through once a payment request has been made to the `TicketPaymentService`.
4. The `SeatReservationService` implementation is an external provider with no defects. You do not need to worry about how the seat reservation algorithm works.
5. The seat will always be reserved once a reservation request has been made to the `SeatReservationService`.

---

## Your Task

Provide a working implementation of a `TicketService` that:

1. Considers the above objective, business rules, constraints and assumptions.
2. Calculates the correct amount for the requested tickets and makes a payment request to the `TicketPaymentService`.
3. Calculates the correct number of seats to reserve and makes a seat reservation request to the `SeatReservationService`.
4. Rejects any invalid ticket purchase requests. It is up to you to identify what should be deemed as an invalid purchase request.

