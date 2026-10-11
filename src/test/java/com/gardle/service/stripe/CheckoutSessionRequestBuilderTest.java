package com.gardle.service.stripe;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutSessionRequestBuilderTest {
    @Test
    void refusesARequestWithoutRequesterId() {
        CheckoutSessionRequestBuilder builder = new CheckoutSessionRequestBuilder()
            .urls("https://example.com/success", "https://example.com/cancel")
            .leasingMetadata(Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2030-01-10T00:00:00Z"), 1L, null)
            .lineItem("garden field", 1000);

        NullPointerException exception = assertThrows(NullPointerException.class, builder::build);
        assertEquals("requesterId", exception.getMessage());
    }
}
