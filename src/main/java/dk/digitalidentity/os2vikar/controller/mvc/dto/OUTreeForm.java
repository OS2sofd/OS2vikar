package dk.digitalidentity.os2vikar.controller.mvc.dto;

import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OUTreeForm {
	private String id;
	private String parent;
	private String text;

	public OUTreeForm(OrgUnit orgUnit, boolean badge, List<String> systemBadgeNames) {
		if (badge) {
			this.text = orgUnit.isCanHaveSubstitutes() ? orgUnit.getName() + " <span class=\"badge badge-primary\">Vikarer</span>" : orgUnit.getName();
			if (orgUnit.isCanHaveSubstitutes()) {
				for (String systemBadgeName : systemBadgeNames) {
					this.text = this.text + " <span class=\"badge badge-plain\">" + systemBadgeName + "</span>";
				}
			}
		}
		else {
			this.text = orgUnit.getName();
		}
		
		this.id = orgUnit.getUuid();
		this.parent = (orgUnit.getParent() != null) ? orgUnit.getParent().getUuid() : "#";
	}
}
