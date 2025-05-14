package dk.digitalidentity.os2vikar.controller.rest.dto;

import java.util.List;

import dk.digitalidentity.os2vikar.controller.mvc.dto.RoleDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.TitleDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUDataDTO {
	private List<TitleDTO> titles;
	private List<RoleDTO> roles;
	private int maxDays;
	private int defaultDays;
}
