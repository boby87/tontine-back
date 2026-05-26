package cm.ftg.tontine.integration.mobilemoney.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MobileMoneyProperties.class)
public class MobileMoneyConfig {
}
