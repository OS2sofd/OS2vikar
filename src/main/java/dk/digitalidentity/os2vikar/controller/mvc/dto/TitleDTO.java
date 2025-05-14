package dk.digitalidentity.os2vikar.controller.mvc.dto;

import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;
import dk.digitalidentity.os2vikar.dao.model.LocalTitle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TitleDTO {
	private Long id;
	private String title;
	private boolean global;
	
	public TitleDTO(GlobalTitle globalTitle) {
		this.id = globalTitle.getId();
		this.title = globalTitle.getTitle();
		this.global = true;
	}
	
	public TitleDTO(LocalTitle localTitle) {
		this.id = localTitle.getId();
		this.title = localTitle.getTitle();
		this.global = false;
	}
}
