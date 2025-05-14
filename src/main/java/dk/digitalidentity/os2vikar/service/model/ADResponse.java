package dk.digitalidentity.os2vikar.service.model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADResponse {
	public enum ADStatus { NOOP, OK, FAILURE, TECHNICAL_ERROR, TIMEOUT, NO_CONNECTION }
	
	private ADStatus status;
	private String message;
	
	public boolean isSuccess() {
		switch (status) {
			case FAILURE:
			case TECHNICAL_ERROR:
			case TIMEOUT:
			case NO_CONNECTION:
				return false;
			case NOOP:
			case OK:
				return true;
		}
	
		// hmmm.... well, the above code will warn about new possible values we need to handle
		return false;
	}
	
	public static boolean isCritical(ADStatus status) {
		switch (status) {
			case FAILURE:
			case TECHNICAL_ERROR:
				return true;
			case NOOP:
			case OK:
			case TIMEOUT:
			case NO_CONNECTION:
				return false;
		}
		
		// hmmm.... well, the above code will warn about new possible values we need to handle
		return true;
	}
}
