package com.gardle.service.leasing;

import com.gardle.domain.Leasing;
import com.gardle.domain.enumeration.LeasingStatus;
import com.gardle.service.PaymentService;

/** Defines how a leasing in a given status responds to an update request. */
public interface LeasingStatusBehavior {
    void transition(Leasing current, LeasingStatus requested, boolean owner,
                    Runnable checkUpdatePeriod, PaymentService paymentService);
}
