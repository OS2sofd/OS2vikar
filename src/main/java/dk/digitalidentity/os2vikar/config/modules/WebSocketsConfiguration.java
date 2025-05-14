package dk.digitalidentity.os2vikar.config.modules;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "os2vikar.websockets")
public class WebSocketsConfiguration {
	private String webSocketKey;

	// if this is enabled the setting in the Vikar agent should also be enabled (checkStatusWhenSetExpire)
	private boolean checkStatusWhenSetExpire = false;
}
