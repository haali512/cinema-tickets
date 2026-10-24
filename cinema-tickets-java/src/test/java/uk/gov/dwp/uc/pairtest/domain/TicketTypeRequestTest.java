package uk.gov.dwp.uc.pairtest.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a ticket request holds its values and cannot be changed once created.
 */
class TicketTypeRequestTest {

    @Test
    void exposesTheTypeAndNumberOfTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);

        assertEquals(TicketTypeRequest.Type.CHILD, request.getTicketType());
        assertEquals(3, request.getNoOfTickets());
    }

    @Test
    void cannotBeSubclassed() {
        assertTrue(Modifier.isFinal(TicketTypeRequest.class.getModifiers()));
    }

    @Test
    void allFieldsArePrivateAndFinal() {
        for (Field field : TicketTypeRequest.class.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            assertTrue(Modifier.isPrivate(field.getModifiers()), field.getName() + " should be private");
            assertTrue(Modifier.isFinal(field.getModifiers()), field.getName() + " should be final");
        }
    }

    @Test
    void hasNoSetters() {
        boolean hasSetter = Arrays.stream(TicketTypeRequest.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.startsWith("set"));

        assertFalse(hasSetter);
    }
}
