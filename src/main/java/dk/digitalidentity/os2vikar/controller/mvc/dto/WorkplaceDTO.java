package dk.digitalidentity.os2vikar.controller.mvc.dto;

import java.time.LocalDate;
import java.util.List;

import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkplaceDTO {
	private OrgUnit orgUnit;
	private String title;
	private LocalDate start;
	private LocalDate stop;
	private List<RoleDTO> roles;
	private long id;
	private boolean canBeExtended;
	private boolean requireO365License = false;
	private long calendarId;
	private long resourceId;
	private String changeStrategy;
}
