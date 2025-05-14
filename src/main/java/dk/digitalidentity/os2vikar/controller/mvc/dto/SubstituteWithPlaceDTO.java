package dk.digitalidentity.os2vikar.controller.mvc.dto;

import java.time.LocalDate;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = { "id", "name", "surname", "orgUnitUuids", "start", "stop", "username", "requireO365License" })
public class SubstituteWithPlaceDTO {
	private Long id;
	private String cpr;
	private String name;
	private String surname;
	private LocalDate lastPwdChange;
	private String orgUnitUuids;
	private LocalDate start;
	private LocalDate stop;
	private ArrayList<RoleDTO> roles;
	private String title;
	private String email;
	private String phone;
	private String agency;
	private String username;
	private boolean requireO365License = false;
	private boolean assignEmployeeSignature = false;
	private long calendarId;
	private long resourceId;
	private String changeStrategy;
	private boolean useSofdADUser = false;
	private String sofdADUserId;
	private String authorizationCode;
}
