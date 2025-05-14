package dk.digitalidentity.os2vikar.service.model;

import dk.digitalidentity.os2vikar.dao.model.PasswordChangeQueue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ADPasswordRequest {
	private String userName;
	private String password;

	public ADPasswordRequest(String samaccountName, String password) {
		this.userName = samaccountName;
		this.password = password;
	}

	public ADPasswordRequest(PasswordChangeQueue change, String password) {
		this.userName = change.getSamaccountName();
		this.password = password;
	}
}
