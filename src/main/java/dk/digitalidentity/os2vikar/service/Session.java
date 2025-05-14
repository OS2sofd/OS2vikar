package dk.digitalidentity.os2vikar.service;

import java.time.LocalDateTime;

import org.springframework.web.socket.WebSocketSession;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Session {
	private static long idMax = 0;
	private long id;
	private WebSocketSession session;
	private LocalDateTime cleanupTimestamp;
	private boolean authenticated;

	public Session(WebSocketSession session) {
		this.session = session;
		this.cleanupTimestamp = LocalDateTime.now().plusHours(2L);
		this.authenticated = false;
		this.id = Session.getNextId();
	}
	
	private static synchronized long getNextId() {
		return (++idMax);
	}
}
