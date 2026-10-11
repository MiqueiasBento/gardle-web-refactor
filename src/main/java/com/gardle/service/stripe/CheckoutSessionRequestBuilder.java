package com.gardle.service.stripe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds the request expected by the existing Stripe checkout client. */
public final class CheckoutSessionRequestBuilder {
    private String successUrl;
    private String cancelUrl;
    private Instant leasingFrom;
    private Instant leasingTo;
    private Long gardenFieldId;
    private Long requesterId;
    private String itemName;
    private Integer amountInCents;

    public CheckoutSessionRequestBuilder urls(String successUrl, String cancelUrl) {
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        return this;
    }

    public CheckoutSessionRequestBuilder leasingMetadata(Instant from, Instant to, Long gardenFieldId, Long requesterId) {
        this.leasingFrom = from;
        this.leasingTo = to;
        this.gardenFieldId = gardenFieldId;
        this.requesterId = requesterId;
        return this;
    }

    public CheckoutSessionRequestBuilder lineItem(String name, Integer amountInCents) {
        this.itemName = name;
        this.amountInCents = amountInCents;
        return this;
    }

    public Map<String, Object> build() {
        Objects.requireNonNull(successUrl, "successUrl");
        Objects.requireNonNull(cancelUrl, "cancelUrl");
        Objects.requireNonNull(leasingFrom, "leasingFrom");
        Objects.requireNonNull(leasingTo, "leasingTo");
        Objects.requireNonNull(gardenFieldId, "gardenFieldId");
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(itemName, "itemName");
        Objects.requireNonNull(amountInCents, "amountInCents");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("leasing_from", leasingFrom.toString());
        metadata.put("leasing_to", leasingTo.toString());
        metadata.put("leasing_gardenfield_id", gardenFieldId.toString());
        metadata.put("leasing_requester_id", requesterId.toString());

        Map<String, Object> paymentIntentData = new HashMap<>();
        paymentIntentData.put("capture_method", "manual");
        paymentIntentData.put("metadata", metadata);

        Map<String, Object> item = new HashMap<>();
        item.put("name", itemName);
        item.put("amount", amountInCents);
        item.put("currency", "eur");
        item.put("quantity", 1);
        List<Object> lineItems = new ArrayList<>();
        lineItems.add(item);

        List<Object> paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card");

        Map<String, Object> request = new HashMap<>();
        request.put("success_url", successUrl);
        request.put("cancel_url", cancelUrl);
        request.put("payment_method_types", paymentMethodTypes);
        request.put("line_items", lineItems);
        request.put("payment_intent_data", paymentIntentData);
        return request;
    }
}
