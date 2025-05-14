package dk.digitalidentity.os2vikar.service.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestHolder {
	private Request request;
	private LocalDateTime tts;
	
	public RequestHolder(Request request) {
		this.request = request;
		this.tts = LocalDateTime.now().plusMinutes(5L);
	}
}
