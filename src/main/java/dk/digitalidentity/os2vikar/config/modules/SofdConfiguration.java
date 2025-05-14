package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SofdConfiguration {
    private String baseUrl;
    private String apiKey;
    private String vikarMasterId = "OS2vikar";
    
    private boolean enableLookup = true;
}
