package com.gardle.domain.enumeration;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeasingStateTest {

    private final Instant now = Instant.parse("2020-01-02T03:04:05Z");

    @Test
    void pastUsesOnlyPastParameter() {
        LeasingState.QueryInstants result = LeasingState.queryInstantsFor(LeasingState.PAST, now);
        assertEquals(now, result.getPast());
        assertNull(result.getOngoing());
        assertNull(result.getFuture());
    }

    @Test
    void ongoingUsesOnlyOngoingParameter() {
        LeasingState.QueryInstants result = LeasingState.queryInstantsFor(LeasingState.ONGOING, now);
        assertNull(result.getPast());
        assertEquals(now, result.getOngoing());
        assertNull(result.getFuture());
    }

    @Test
    void futureUsesOnlyFutureParameter() {
        LeasingState.QueryInstants result = LeasingState.queryInstantsFor(LeasingState.FUTURE, now);
        assertNull(result.getPast());
        assertNull(result.getOngoing());
        assertEquals(now, result.getFuture());
    }

    @Test
    void absentStateLeavesAllParametersUnset() {
        LeasingState.QueryInstants result = LeasingState.queryInstantsFor(null, now);
        assertNull(result.getPast());
        assertNull(result.getOngoing());
        assertNull(result.getFuture());
    }
}
