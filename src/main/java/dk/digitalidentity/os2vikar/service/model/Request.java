package dk.digitalidentity.os2vikar.service.model;

import java.util.Objects;
import java.util.UUID;

import dk.digitalidentity.os2vikar.config.Commands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class Request {
	private String transactionUuid;     // random uuid
	private String command;             // AUTHENTICATE | SET_PASSWORD
	private String target;              // sAMAccountName or NULL for AUTHENTICATE messages
	private String payload;             // password to set/validate or NULL for AUTHENTICATE messages
	private String signature;           // keyed hmac of above
	
	// TODO: add a timestamp to the request, so the client can reject old messages (replay of all password changes for instance).
	//       does not need ms accuracy, just enough to avoid reply of old (quite old actually) password change requests
	
	public Request() {
		this.transactionUuid = UUID.randomUUID().toString();
	}
	
	public void sign(String key) throws Exception {
		switch (command) {
			case Commands.AUTHENTICATE:
				this.signature = HMacUtil.hmac(transactionUuid + "." + command, key);
				break;
			case Commands.CREATE_ACCOUNT:
			case Commands.ASSOCIATE_ACCOUNT:
			case Commands.SET_PASSWORD:
			case Commands.UPDATE_LICENSE:
			case Commands.SET_EXPIRE:
			case Commands.DELETE_ACCOUNT:
			case Commands.DISABLE_ACCOUNT:
			case Commands.ENABLE_ACCOUNT:
			case Commands.EMPLOYEE_SIGNATURE:
			case Commands.AD_GROUPS_SYNC:
			case Commands.UNLOCK_ACCOUNT:
			case Commands.SET_AUTHORIZATION_CODE:
				this.signature = HMacUtil.hmac(transactionUuid + "." + command + "." + target + "." + payload, key);
				break;
			default:
				throw new Exception("Unknown command: " + command);
		}
	}

	public boolean validateEcho(Response message) {
		switch (command) {
			case Commands.AUTHENTICATE:
				if (Objects.equals(command, message.getCommand())) {
					return true;
				}
				break;
			case Commands.CREATE_ACCOUNT:
			case Commands.ASSOCIATE_ACCOUNT:
			case Commands.SET_PASSWORD:
			case Commands.SET_EXPIRE:
			case Commands.UPDATE_LICENSE:
			case Commands.DELETE_ACCOUNT:
			case Commands.DISABLE_ACCOUNT:
			case Commands.ENABLE_ACCOUNT:
			case Commands.EMPLOYEE_SIGNATURE:
			case Commands.AD_GROUPS_SYNC:
			case Commands.UNLOCK_ACCOUNT:
			case Commands.SET_AUTHORIZATION_CODE:
				if (Objects.equals(command, message.getCommand()) &&
					Objects.equals(target, message.getTarget())) {
					return true;
				}
				break;
			default:
				log.error("Unknown command: " + command);
		}

		return false;
	}
}
