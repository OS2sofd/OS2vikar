package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Organisation {
	private boolean enabled = true;
    private String url = "http://os2sync.digital-identity.dk/api/hierarchy";
}