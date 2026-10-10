package com.gardle.service.leasing;

import com.gardle.domain.Leasing;
import com.gardle.domain.enumeration.LeasingStatus;
import com.gardle.service.PaymentService;
import com.gardle.service.exception.InvalidLeasingStateServiceException;

public final class OpenLeasingStatusBehavior implements LeasingStatusBehavior {
    @Override
    public void transition(Leasing current, LeasingStatus requested, boolean owner,
                           Runnable checkUpdatePeriod, PaymentService paymentService) {
        if (owner) {
            if (requested == LeasingStatus.RESERVED) {
                checkUpdatePeriod.run();
                current.setStatus(LeasingStatus.RESERVED);
                paymentService.finalizePayment(current);
            } else if (requested == LeasingStatus.REJECTED) {
                current.setStatus(LeasingStatus.REJECTED);
                paymentService.cancelPayment(current.getPaymentSessionId());
            } else {
                throw new InvalidLeasingStateServiceException();
            }
        } else if (requested == LeasingStatus.CANCELLED) {
            current.setStatus(LeasingStatus.CANCELLED);
            paymentService.cancelPayment(current.getPaymentSessionId());
        } else if (requested != LeasingStatus.OPEN) {
            throw new InvalidLeasingStateServiceException();
        }
    }
}
