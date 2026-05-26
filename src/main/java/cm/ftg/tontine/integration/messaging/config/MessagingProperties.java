package cm.ftg.tontine.integration.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private final Sms sms = new Sms();
    private final Email email = new Email();

    public Sms getSms() {
        return sms;
    }

    public Email getEmail() {
        return email;
    }

    public static class Sms {
        /** "log" (defaut) ou "twilio". */
        private String provider = "log";
        private String fromNumber;
        private final Twilio twilio = new Twilio();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }

        public Twilio getTwilio() {
            return twilio;
        }
    }

    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String baseUrl = "https://api.twilio.com";

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Email {
        /** "log" (defaut) ou "smtp". */
        private String provider = "log";
        private String fromAddress = "no-reply@tontine.local";
        private String fromName = "Tontine";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }
    }
}
