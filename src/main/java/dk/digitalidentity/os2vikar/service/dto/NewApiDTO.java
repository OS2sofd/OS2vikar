package dk.digitalidentity.os2vikar.service.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class NewApiDTO {
	private long itSystemId;
	private String itSystemName;
	private List<UserRoleNewApiDTO> roles;
}