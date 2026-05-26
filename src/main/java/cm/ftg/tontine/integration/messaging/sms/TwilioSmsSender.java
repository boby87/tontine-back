package cm.ftg.tontine.integration.messaging.sms;

import cm.ftg.tontine.integration.messaging.config.MessagingProperties;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "app.messaging.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final MessagingProperties properties;
    private final RestClient client;

    public TwilioSmsSender(MessagingProperties properties) {
        this.properties = properties;
        MessagingProperties.Twilio cfg = properties.getSms().getTwilio();
        if (cfg.getAccountSid() == null || cfg.getAuthToken() == null) {
            throw new IllegalStateException(
                    "Provider Twilio actif mais accountSid/authToken non configures");
        }
        String basic = Base64.getEncoder().encodeToString(
                (cfg.getAccountSid() + ":" + cfg.getAuthToken()).getBytes());
        this.client = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + basic)
                .build();
    }

    @Override
    public void send(String toPhone, String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toPhone);
        form.add("From", properties.getSms().getFromNumber());
        form.add("Body", body);
        String sid = properties.getSms().getTwilio().getAccountSid();
        try {
            client.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", sid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Echec envoi SMS Twilio to={} : {}",
                    mask(toPhone), e.getMessage());
            throw e;
        }
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 5) {
            return "***";
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }
}
