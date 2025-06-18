package dk.digitalidentity.os2vikar.service.model;

import java.util.Objects;

import dk.digitalidentity.os2vikar.config.Commands;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
public class Response {
	private String transactionUuid;   // echo transactionUuid from request
	private String command;           // echo command from request
	private String target;            // echo target from request (except for AUTHENTICATE, where it contains the domain)
	private String status;            // true on success, false otherwise
	private String message;           // optional message (not under signature - status is the field to check against, message is for debugging)
	private String clientVersion;     // version of client
	private String signature;         // keyed hmac on above     

	public boolean verify(String key) {
		switch (command) {
			case Commands.AUTHENTICATE:
				try {	
					return Objects.equals(this.signature, HMacUtil.hmac(transactionUuid + "." + command + "." + status, key));
				}
				catch (Exception ex) {
					log.error("Failed to verify signature", ex);

					return false;
				}
			case Commands.SET_PASSWORD:
			case Commands.CREATE_ACCOUNT:
			case Commands.ASSOCIATE_ACCOUNT:
			case Commands.SET_EXPIRE:
			case Commands.UPDATE_LICENSE:
			case Commands.DELETE_ACCOUNT:
			case Commands.DISABLE_ACCOUNT:
			case Commands.ENABLE_ACCOUNT:
			case Commands.EMPLOYEE_SIGNATURE:
			case Commands.AD_GROUPS_SYNC:
			case Commands.UNLOCK_ACCOUNT:
			case Commands.SET_AUTHORIZATION_CODE:
				try {
					return Objects.equals(this.signature, HMacUtil.hmac(transactionUuid + "." + command + "." + target + "." + status, key));
				}
				catch (Exception ex) {
					log.error("Failed to verify signature", ex);

					return false;
				}
			default:
				log.error("Unknown command: " + command);
				break;
		}
		
		return false;
	}
}
