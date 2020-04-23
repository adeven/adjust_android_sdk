package com.adjust.sdk;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdjustSubscription {
    private Double revenue;         // [M] revenue
    private Long purchaseTime;      // [M] transaction_date
    private String currency;        // [M] currency
    private String sku;             // [M] product_id
    private String signature;       // [M] receipt
    private String purchaseToken;   // [M] purchase_token
    private String billingStore;    // [M] billing_store
    private String orderId;         // [O] transaction_id
    private String salesRegion;     // [O] sales_region
    private Map<String, String> callbackParameters; // [O] callback_params
    private Map<String, String> partnerParameters;  // [O] partner_params

    private static ILogger logger = AdjustFactory.getLogger();

    public AdjustSubscription(final Double revenue,
                              final Long purchaseTime,
                              final String currency,
                              final String sku,
                              final String signature,
                              final String purchaseToken) {
        this.revenue = revenue;
        this.purchaseTime = purchaseTime;
        this.currency = currency;
        this.sku = sku;
        this.signature = signature;
        this.purchaseToken = purchaseToken;
        this.billingStore = "GooglePlay";
    }

    Double getRevenue() {
        return revenue;
    }

    Long getPurchaseTime() {
        return purchaseTime;
    }

    String getCurrency() {
        return currency;
    }

    String getSku() {
        return sku;
    }

    String getOrderId() {
        return orderId;
    }

    String getSignature() {
        return signature;
    }

    String getBillingStore() {
        return billingStore;
    }

    String getPurchaseToken() {
        return purchaseToken;
    }

    String getSalesRegion() {
        return salesRegion;
    }

    Map<String, String> getCallbackParameters() {
        return callbackParameters;
    }

    Map<String, String> getPartnerParameters() {
        return partnerParameters;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setSalesRegion(final String salesRegion) {
        this.salesRegion = salesRegion;
    }

    public void addCallbackParameter(String key, String value) {
        if (!Util.isValidParameter(key, "key", "Callback")) {
            return;
        }
        if (!Util.isValidParameter(value, "value", "Callback")) {
            return;
        }

        if (callbackParameters == null) {
            callbackParameters = new LinkedHashMap<String, String>();
        }

        String previousValue = callbackParameters.put(key, value);
        if (previousValue != null) {
            logger.warn("Key %s was overwritten", key);
        }
    }

    public void addPartnerParameter(String key, String value) {
        if (!Util.isValidParameter(key, "key", "Partner")) {
            return;
        }
        if (!Util.isValidParameter(value, "value", "Partner")) {
            return;
        }

        if (partnerParameters == null) {
            partnerParameters = new LinkedHashMap<String, String>();
        }

        String previousValue = partnerParameters.put(key, value);
        if (previousValue != null) {
            logger.warn("Key %s was overwritten", key);
        }
    }
}