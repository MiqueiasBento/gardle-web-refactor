package com.gardle.service.event;

import com.gardle.domain.Leasing;

import java.util.Objects;

/** A saved leasing whose participants should receive a notification. */
public final class LeasingChangedEvent {
    private final Leasing leasing;

    public LeasingChangedEvent(Leasing leasing) {
        this.leasing = Objects.requireNonNull(leasing, "leasing");
    }

    public Leasing getLeasing() {
        return leasing;
    }
}
