package com.gardle.service;

import com.gardle.GardleApp;
import com.gardle.domain.GardenField;
import com.gardle.domain.Leasing;
import com.gardle.domain.User;
import com.gardle.repository.GardenFieldRepository;
import com.gardle.service.dto.CreatePaymentProviderAccountDTO;
import com.gardle.service.dto.FinalizePaymentAtPaymentProviderDTO;
import com.gardle.service.dto.GetStripeAccountLinkUrlDTO;
import com.gardle.service.dto.leasing.CreatorLeasingDTO;
import com.gardle.service.exception.StripeServiceException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Map;
import java.util.List;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = GardleApp.class)
@Transactional
public class StripeServiceTest {

    @MockBean
    private StripeClient stripeClient;

    @MockBean
    private GardenFieldRepository gardenFieldRepository;

    @MockBean
    private SecurityHelperService securityHelperService;

    @Autowired
    private StripeService stripeService;

    @Value("${stripe.payment-success-url}")
    private String paymentSuccessUrl;

    @Value("${stripe.payment-cancel-url}")
    private String paymentCancelUrl;

    private CreatorLeasingDTO creatorLeasingDTO;

    private GardenField gardenField;

    @BeforeEach
    public void init() {
        creatorLeasingDTO = new CreatorLeasingDTO();
        gardenField = new GardenField();
        gardenField.setPricePerM2(1.0);
        gardenField.setSizeInM2(1.0);
        gardenField.setId(1L);

        when(gardenFieldRepository.getOne(1L)).thenReturn(gardenField);
        when(gardenFieldRepository.findById(1L)).thenReturn(Optional.of(gardenField));

        creatorLeasingDTO.setGardenFieldId(gardenField.getId());
        creatorLeasingDTO.setGardenFieldName("gardenfield name");
    }

    @Test
    public void testCreateCheckoutSessionPaymentNull() {
        assertThrows(IllegalArgumentException.class, () ->
            stripeService.createCheckoutSession(null));
    }

    @Test
    public void testCreateCheckoutSession() throws StripeException {
        creatorLeasingDTO.setFrom(Instant.now().plus(100, ChronoUnit.DAYS));
        creatorLeasingDTO.setTo(creatorLeasingDTO.getFrom().plus(100, ChronoUnit.DAYS));
        creatorLeasingDTO.setGardenFieldName("gardenfield name");

        User user = new User();
        user.setId(2L);
        when(securityHelperService.getLoggedInUser()).thenReturn(user);
        when(stripeClient.createCheckoutSession(anyMap())).thenReturn(new Session());

        stripeService.createCheckoutSession(creatorLeasingDTO);
        verify(stripeClient).createCheckoutSession(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckoutRequestParameters() throws StripeException {
        creatorLeasingDTO.setFrom(Instant.parse("2030-01-01T00:00:00Z"));
        creatorLeasingDTO.setTo(Instant.parse("2030-01-10T00:00:00Z"));
        User user = new User();
        user.setId(2L);
        when(securityHelperService.getLoggedInUser()).thenReturn(user);
        when(stripeClient.createCheckoutSession(anyMap())).thenReturn(new Session());

        stripeService.createCheckoutSession(creatorLeasingDTO);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(stripeClient).createCheckoutSession(captor.capture());
        Map<String, Object> request = captor.getValue();
        assertThat(request).containsOnlyKeys("success_url", "cancel_url", "payment_method_types", "line_items", "payment_intent_data");
        assertThat(request.get("success_url")).isEqualTo(paymentSuccessUrl);
        assertThat(request.get("cancel_url")).isEqualTo(paymentCancelUrl);
        assertThat((List<String>) request.get("payment_method_types")).containsExactly("card");

        Map<String, Object> lineItem = (Map<String, Object>) ((List<?>) request.get("line_items")).get(0);
        assertThat(lineItem).containsOnlyKeys("name", "amount", "currency", "quantity")
            .containsEntry("name", "gardenfield name")
            .containsEntry("amount", 1000).containsEntry("currency", "eur")
            .containsEntry("quantity", 1);
        assertThat((List<?>) request.get("line_items")).hasSize(1);

        Map<String, Object> paymentIntent = (Map<String, Object>) request.get("payment_intent_data");
        assertThat(paymentIntent).containsOnlyKeys("capture_method", "metadata");
        assertThat(paymentIntent.get("capture_method")).isEqualTo("manual");
        assertThat((Map<String, String>) paymentIntent.get("metadata"))
            .containsEntry("leasing_from", "2030-01-01T00:00:00Z")
            .containsEntry("leasing_to", "2030-01-10T00:00:00Z")
            .containsEntry("leasing_gardenfield_id", "1")
            .containsEntry("leasing_requester_id", "2")
            .hasSize(4);
    }

    @Test
    public void testCancelPayment() throws StripeException {
        String sessionId = "testCheckouId";

        stripeService.cancelPayment(sessionId);

        verify(stripeClient).cancelPaymentIntent(sessionId);
    }

    @Test
    public void testCancelPaymentWithException() throws StripeException {
        String sessionId = "testCheckouId";
        doThrow(InvalidRequestException.class).when(stripeClient).cancelPaymentIntent(anyString());

        assertThrows(StripeServiceException.class, () -> stripeService.cancelPayment(sessionId));
    }

    @Test
    public void testFinalizePayment() throws StripeException {
        String testAccId = "testAccountId";
        String sessionId = "checkoutSessionId";
        Leasing leasing = new Leasing();
        leasing.setFrom(Instant.now().plus(100, ChronoUnit.DAYS));
        leasing.setTo(leasing.getFrom().plus(100, ChronoUnit.DAYS));
        leasing.setPaymentSessionId(sessionId);
        leasing.setGardenField(gardenField);

        stripeService.finalizePayment(new FinalizePaymentAtPaymentProviderDTO(testAccId, leasing));
        verify(stripeClient).capturePaymentIntent(sessionId);
    }

    @Test
    public void testCreateAccount() throws StripeException {
        String bankAccountIBAN = "4000000400000008";
        when(stripeClient.createAccount(anyMap())).thenReturn(new Account());

        stripeService.createAccount(new CreatePaymentProviderAccountDTO("email@mail.com", bankAccountIBAN));

        verify(stripeClient).createAccount(any());
    }

    @Test
    public void testGetAccountLinkUrl() throws StripeException {
        when(stripeClient.getAccountLink(anyMap())).thenReturn("testUrl");

        String accountId = "testAccId";
        String res;

        res = stripeService.getAccountLinkUrl(new GetStripeAccountLinkUrlDTO(accountId, ""));
        assertThat(res).isNotNull();
        assertThat(res).isNotEmpty();
    }

}
