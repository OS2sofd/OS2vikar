package dk.digitalidentity.os2vikar.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CprLookupDto {
	private String name;
	private String surname;
	private String cpr;
	private boolean exists;
	private boolean inSofd;
	private boolean hasSofdADUser;
	private String sofdADUserId;
}
