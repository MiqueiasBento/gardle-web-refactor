package com.gardle.service.leasing;

import com.gardle.domain.enumeration.LeasingStatus;

import java.util.EnumMap;
import java.util.Map;

public final class LeasingStatusBehaviors {
    private static final Map<LeasingStatus, LeasingStatusBehavior> BEHAVIORS = new EnumMap<>(LeasingStatus.class);

    static {
        LeasingStatusBehavior terminal = new TerminalLeasingStatusBehavior();
        BEHAVIORS.put(LeasingStatus.OPEN, new OpenLeasingStatusBehavior());
        BEHAVIORS.put(LeasingStatus.RESERVED, terminal);
        BEHAVIORS.put(LeasingStatus.REJECTED, terminal);
        BEHAVIORS.put(LeasingStatus.CANCELLED, terminal);
    }

    private LeasingStatusBehaviors() { }

    public static LeasingStatusBehavior forStatus(LeasingStatus status) {
        return BEHAVIORS.get(status);
    }
}
