package dk.digitalidentity.os2vikar.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteApiDTO {
	private String cpr;
	private String uuid;
	private String firstname;
	private String surname;
	private String username;
	private boolean usernameIsEstimate;
	private List<WorkplaceApiDTO> workplaces;
	private String lastUpdated;
}
