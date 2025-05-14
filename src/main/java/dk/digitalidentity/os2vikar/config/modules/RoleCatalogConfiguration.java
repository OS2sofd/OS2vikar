package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCatalogConfiguration {
	private String url;
	private String apiKey;
	private boolean enabled = true;
	
	private boolean enabledForWorkplaces = false;
	private boolean allowAutomaticUserRoles = false;
}
