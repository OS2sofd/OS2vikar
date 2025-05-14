package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Nexus {
   	private boolean enabled = false;
    private String syncUrl = "https://nexus.digital-identity.dk/api/reset/";
    private String syncApiKey;
}
