package dk.digitalidentity.os2vikar.controller.mvc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleDTO {
	private Long id;
	private String name;
	private String description;
	private String itSystem;
	private boolean checked;
	private boolean global;
}
