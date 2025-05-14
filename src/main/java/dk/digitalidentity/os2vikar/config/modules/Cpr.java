package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cpr {
	private String url = "http://cprservice.digital-identity.dk";
	private boolean dev = false;
}
