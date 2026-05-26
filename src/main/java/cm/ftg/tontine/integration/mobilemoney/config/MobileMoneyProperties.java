package cm.ftg.tontine.integration.mobilemoney.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mobile-money")
public class MobileMoneyProperties {

    /** "simulator" (defaut), "mtn", "orange". */
    private String provider = "simulator";

    /** Webhook secret partage pour la signature HMAC SHA-256. */
    private String webhookSecret = "";

    private final Mtn mtn = new Mtn();
    private final Orange orange = new Orange();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Mtn getMtn() {
        return mtn;
    }

    public Orange getOrange() {
        return orange;
    }

    public static class Mtn {
        private String baseUrl = "https://sandbox.momodeveloper.mtn.com";
        private String subscriptionKey = "";
        private String apiUser = "";
        private String apiKey = "";
        private String targetEnvironment = "sandbox";
        private String callbackUrl = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getSubscriptionKey() { return subscriptionKey; }
        public void setSubscriptionKey(String subscriptionKey) { this.subscriptionKey = subscriptionKey; }
        public String getApiUser() { return apiUser; }
        public void setApiUser(String apiUser) { this.apiUser = apiUser; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getTargetEnvironment() { return targetEnvironment; }
        public void setTargetEnvironment(String targetEnvironment) { this.targetEnvironment = targetEnvironment; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public static class Orange {
        private String baseUrl = "https://api.orange.com";
        private String clientId = "";
        private String clientSecret = "";
        private String merchantKey = "";
        private String callbackUrl = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getMerchantKey() { return merchantKey; }
        public void setMerchantKey(String merchantKey) { this.merchantKey = merchantKey; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }
}
