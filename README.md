# Cinema Tickets

A Java implementation of the DWP cinema tickets coding exercise. `TicketServiceImpl` validates a ticket purchase against the business rules. For a valid purchase it charges the correct amount through `TicketPaymentService` and reserves the correct number of seats through `SeatReservationService`. An invalid purchase is rejected with an `InvalidPurchaseException`, and neither service is called.

The solution lives in [`cinema-tickets-java`](cinema-tickets-java).

---

## Contents

- [Running the tests](#running-the-tests)
- [The exercise in brief](#the-exercise-in-brief)
- [Design](#design)
- [Where each business rule is enforced](#where-each-business-rule-is-enforced)
- [What counts as an invalid purchase](#what-counts-as-an-invalid-purchase)
- [Assumptions and decisions](#assumptions-and-decisions)
- [Constraints](#constraints)
- [Testing](#testing)

---

## Running the tests

Requires **Java 21** and **Maven 3.9+**.

```bash
cd cinema-tickets-java
mvn clean test
```

---

## The exercise in brief

| Ticket type | Price | Gets a seat? |
|-------------|-------|--------------|
| ADULT       | £25   | Yes          |
| CHILD       | £15   | Yes          |
| INFANT      | £0    | No – sits on an adult's lap |

- Multiple tickets can be bought at once, up to a maximum of **25**.
- Child and infant tickets can't be bought without an adult ticket.
- Accounts with an id greater than zero are valid and have enough funds.
- The payment and seat reservation services are external, have no defects and always succeed.

---

## Design

```
uk.gov.dwp.uc.pairtest
├── TicketServiceImpl            purchase flow and ticket prices
├── validation
│   └── TicketPurchaseValidator  every business rule, and the 25-ticket limit
├── util
│   └── TicketRequestUtils       shared counting helpers (total, count by type)
├── exception
│   ├── InvalidPurchaseException carries a PurchaseError and its message
│   └── PurchaseError            one constant per rejection reason
└── domain
    └── TicketTypeRequest        immutable "N tickets of type X"

src/main/resources/messages.properties   wording for every error message
```

### Purchase flow

```
purchaseTickets(accountId, requests)
  1. TicketPurchaseValidator.validate(...)  → throws InvalidPurchaseException on the first rule broken
  2. count adults, children, infants        → TicketRequestUtils.countTickets(...)
  3. TicketPaymentService.makePayment(accountId, adults × 25 + children × 15)
  4. SeatReservationService.reserveSeat(accountId, adults + children)
```

Validation finishes before either external service is called, so a rejected purchase never takes payment or reserves a seat.

### Why it's split this way

- **`TicketServiceImpl`** only runs the purchase. The starter code says it should have no public methods other than `purchaseTickets`, so pricing and seat calculations are private methods. The two third-party services are passed in through the constructor, so tests can replace them with mocks.
- **`TicketPurchaseValidator`** keeps all the rules in one place. The rules can be tested without mocks, and anything else that needs to check a purchase can reuse them.
- **`TicketRequestUtils`** holds the counting that the validator and the service both need. `totalTickets` returns a `long`, so huge requests can't overflow an `int` and slip under the limit. `countTickets` uses `Math.toIntExact`, so it fails loudly instead of returning a wrong number.
- **`PurchaseError` + `messages.properties`** keep message wording out of the code. Callers and tests check `exception.getError()` rather than message text, so rewording a message doesn't break anything. A test checks that every `PurchaseError` has a message defined.

---

## Where each business rule is enforced

| Rule | Enforced in |
|------|-------------|
| Prices: adult £25, child £15, infant £0 | `TicketServiceImpl.calculateTotalAmount` |
| Infants are not allocated a seat | `TicketServiceImpl.calculateSeatsToReserve` |
| Multiple tickets and requests per purchase | `TicketRequestUtils.countTickets` combines requests of the same type |
| Maximum of 25 tickets per purchase | `TicketPurchaseValidator.validateTotalTicketCount` |
| Child and infant tickets need an adult ticket | `TicketPurchaseValidator.validateAdultSupervision` |
| Infants sit on an adult's lap | `TicketPurchaseValidator.validateAdultSupervision` (no more infants than adults) |
| Only account ids greater than zero are valid | `TicketPurchaseValidator.validateAccountId` |

---

## What counts as an invalid purchase

The checks run in this order and stop at the first failure:

| `PurchaseError` | Thrown when | Message |
|-----------------|-------------|---------|
| `INVALID_ACCOUNT_ID` | account id is `null`, zero or negative | Account id must be greater than zero |
| `NO_TICKETS_REQUESTED` | no requests, or a `null` request array | At least one ticket request must be provided |
| `NULL_TICKET_REQUEST` | any individual request is `null` | Ticket request must not be null |
| `MISSING_TICKET_TYPE` | a request has no ticket type | Ticket type must be specified |
| `INVALID_TICKET_QUANTITY` | a request is for zero or fewer tickets | Number of tickets must be greater than zero |
| `MAX_TICKETS_EXCEEDED` | more than 25 tickets in total, infants included | Cannot purchase more than 25 tickets at a time |
| `ADULT_REQUIRED` | no adult tickets in the purchase | Child and infant tickets cannot be purchased without an adult ticket |
| `INFANTS_EXCEED_ADULTS` | more infants than adults | Each infant must sit on an adult's lap, so infants cannot outnumber adults |

---

## Assumptions and decisions

The exercise leaves it to the candidate to decide what is invalid. These are the decisions made:

1. **One infant per adult.** An infant sits on an adult's lap and an adult has one lap, so `infants > adults` is rejected.
2. **Infants count towards the 25-ticket limit.** The rule is about tickets, not seats.
3. **Every request must be for at least one ticket.** Zero is treated as a mistake. Negative numbers are rejected so that something like `(ADULT, 30), (ADULT, -10)` can't get round the limit or lower the price.
4. **Totals are overflow-safe.** Two requests of `Integer.MAX_VALUE` would wrap round as `int`s, so the total is summed as a `long`.
5. **Payment is taken before seats are reserved.** Both services are assumed to always succeed, so the order doesn't change the result.
6. **The validator is created by the service, not injected.** It has no state and no outside connections, so there's nothing to replace with a mock. The third-party services are injected because they are external.

---

## Constraints

- `TicketService` interface is **unchanged**.
- `thirdparty.*` packages are **unchanged**.
- `TicketTypeRequest` is **immutable**: the class is `final`, its fields are `private final` and it has no setters. A test checks this with reflection.

---

## Testing

68 tests, using JUnit 5 and Mockito.

| Test class | Covers |
|------------|--------|
| `TicketPurchaseValidatorTest` | Every rule, checked by `PurchaseError`. Boundaries such as exactly 25 tickets, equal infants and adults, and account id 1. Tricks such as negative quantities and integer overflow. |
| `TicketServiceImplTest` | Exact amount charged and seats reserved for many combinations, including a parameterised table. For each kind of invalid purchase, no payment and no reservation. Constructor null checks. |
| `TicketRequestUtilsTest` | Totals, per-type counts, empty input, and overflow behaviour. |
| `InvalidPurchaseExceptionTest` | Every `PurchaseError` has a message in `messages.properties`; placeholders and apostrophes format correctly. |
| `TicketTypeRequestTest` | Values are held correctly and the class can't be changed. |

`pom.xml` configures Surefire to load Mockito as a `-javaagent`. This is the setup Mockito recommends on JDK 21+, and it replaces the deprecated self-attach that prints warnings on every run.
