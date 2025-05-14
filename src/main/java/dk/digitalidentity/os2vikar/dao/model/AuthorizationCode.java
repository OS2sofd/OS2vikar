package dk.digitalidentity.os2vikar.dao.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationCode {
	private String code; // e.g. "ABY7ZQ"
	private String name; // e.g. "Sygeplejerske"
	private boolean prime;
}