package com.gardle.service.leasing;

import com.gardle.domain.Leasing;
import com.gardle.domain.User;
import com.gardle.domain.enumeration.LeasingStatus;
import com.gardle.service.PaymentProviderService;
import com.gardle.service.PaymentService;
import com.gardle.service.SecurityHelperService;
import com.gardle.service.dto.FinalizePaymentAtPaymentProviderDTO;
import com.gardle.service.exception.InvalidLeasingStateServiceException;
import com.gardle.service.exception.LeasingUpdateNotAllowedServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LeasingStatusBehaviorTest {
    private PaymentProviderService provider;
    private PaymentService payment;
    private Leasing leasing;
    private Runnable allowedPeriod;

    @BeforeEach
    void setUp() {
        provider = mock(PaymentProviderService.class);
        SecurityHelperService security = mock(SecurityHelperService.class);
        User owner = new User();
        owner.setStripeAccountId("acct_owner");
        when(security.getLoggedInUser()).thenReturn(owner);
        payment = new PaymentService(security, provider);
        leasing = new Leasing();
        leasing.setStatus(LeasingStatus.OPEN);
        leasing.setPaymentSessionId("session_1");
        allowedPeriod = mock(Runnable.class);
    }

    @Test
    void requesterCanCancelAndPaymentIsCancelled() {
        transition(LeasingStatus.CANCELLED, false);
        assertEquals(LeasingStatus.CANCELLED, leasing.getStatus());
        verify(provider).cancelPayment("session_1");
        verify(provider, never()).finalizePayment(any());
    }

    @Test
    void ownerCanReserveAndPaymentIsFinalized() {
        transition(LeasingStatus.RESERVED, true);
        assertEquals(LeasingStatus.RESERVED, leasing.getStatus());
        verify(allowedPeriod).run();
        ArgumentCaptor<FinalizePaymentAtPaymentProviderDTO> dto =
            ArgumentCaptor.forClass(FinalizePaymentAtPaymentProviderDTO.class);
        verify(provider).finalizePayment(dto.capture());
        assertSame(leasing, dto.getValue().getLeasing());
        assertEquals("acct_owner", dto.getValue().getAccountId());
        verify(provider, never()).cancelPayment(anyString());
    }

    @Test
    void ownerCanRejectAndPaymentIsCancelledWithoutDateCheck() {
        transition(LeasingStatus.REJECTED, true);
        assertEquals(LeasingStatus.REJECTED, leasing.getStatus());
        verify(provider).cancelPayment("session_1");
        verifyZeroInteractions(allowedPeriod);
    }

    @Test
    void ownerCannotCancelOrKeepOpen() {
        assertThrows(InvalidLeasingStateServiceException.class, () -> transition(LeasingStatus.CANCELLED, true));
        assertThrows(InvalidLeasingStateServiceException.class, () -> transition(LeasingStatus.OPEN, true));
        assertEquals(LeasingStatus.OPEN, leasing.getStatus());
        verifyZeroInteractions(provider);
    }

    @Test
    void requesterCannotReserveOrReject() {
        assertThrows(InvalidLeasingStateServiceException.class, () -> transition(LeasingStatus.RESERVED, false));
        assertThrows(InvalidLeasingStateServiceException.class, () -> transition(LeasingStatus.REJECTED, false));
        assertEquals(LeasingStatus.OPEN, leasing.getStatus());
        verifyZeroInteractions(provider);
    }

    @Test
    void requesterCanKeepOpenWithoutPaymentEffect() {
        transition(LeasingStatus.OPEN, false);
        assertEquals(LeasingStatus.OPEN, leasing.getStatus());
        verifyZeroInteractions(provider);
    }

    @Test
    void reservationStillChecksTheAllowedPeriod() {
        Runnable deniedPeriod = () -> { throw new LeasingUpdateNotAllowedServiceException(); };
        assertThrows(LeasingUpdateNotAllowedServiceException.class,
            () -> LeasingStatusBehaviors.forStatus(LeasingStatus.OPEN)
                .transition(leasing, LeasingStatus.RESERVED, true, deniedPeriod, payment));
        assertEquals(LeasingStatus.OPEN, leasing.getStatus());
        verifyZeroInteractions(provider);
    }

    @Test
    void terminalStatusesOnlyAcceptTheSameStatus() {
        for (LeasingStatus terminal : new LeasingStatus[] {
            LeasingStatus.RESERVED, LeasingStatus.REJECTED, LeasingStatus.CANCELLED
        }) {
            leasing.setStatus(terminal);
            for (boolean owner : new boolean[] {true, false}) {
                LeasingStatusBehaviors.forStatus(terminal)
                    .transition(leasing, terminal, owner, allowedPeriod, payment);
                for (LeasingStatus requested : LeasingStatus.values()) {
                    if (requested != terminal) {
                        assertThrows(InvalidLeasingStateServiceException.class,
                            () -> LeasingStatusBehaviors.forStatus(terminal)
                                .transition(leasing, requested, owner, allowedPeriod, payment));
                    }
                }
            }
            assertEquals(terminal, leasing.getStatus());
        }
        verifyZeroInteractions(provider);
    }

    private void transition(LeasingStatus requested, boolean owner) {
        LeasingStatusBehaviors.forStatus(leasing.getStatus())
            .transition(leasing, requested, owner, allowedPeriod, payment);
    }
}
