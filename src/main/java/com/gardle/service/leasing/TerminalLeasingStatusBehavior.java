package com.gardle.service.leasing;

import com.gardle.domain.Leasing;
import com.gardle.domain.enumeration.LeasingStatus;
import com.gardle.service.PaymentService;
import com.gardle.service.exception.InvalidLeasingStateServiceException;

public final class TerminalLeasingStatusBehavior implements LeasingStatusBehavior {
    @Override
    public void transition(Leasing current, LeasingStatus requested, boolean owner,
                           Runnable checkUpdatePeriod, PaymentService paymentService) {
        if (current.getStatus() != requested) {
            throw new InvalidLeasingStateServiceException();
        }
    }
}
