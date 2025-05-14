package dk.digitalidentity.os2vikar.controller.mvc.dto;

import dk.digitalidentity.os2vikar.dao.model.GlobalRole;
import dk.digitalidentity.os2vikar.dao.model.LocalRole;
import dk.digitalidentity.os2vikar.dao.model.WorkplaceAssignedRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoleDTO {
	private Long id;
	private String name;
	private boolean global;
	
	public RoleDTO(GlobalRole globalRole) {
		this.id = globalRole.getId();
		this.name = globalRole.getUserRole().getName();
		this.global = true;
	}
	
	public RoleDTO(LocalRole localRole) {
		this.id = localRole.getId();
		this.name = localRole.getUserRole().getName();
		this.global = false;
	}
	
	public RoleDTO(WorkplaceAssignedRole workplaceAssignedRole) {
		this.id = workplaceAssignedRole.getId();
		this.name = workplaceAssignedRole.getName();
		this.global = true;
	}
}
