package dk.digitalidentity.os2vikar.service.model;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseHolder {
	private ADResponse response;
	private LocalDateTime tts;
	
	public ResponseHolder(ADResponse response) {
		this.response = response;
		this.tts = LocalDateTime.now().plusMinutes(5L);
	}
}
