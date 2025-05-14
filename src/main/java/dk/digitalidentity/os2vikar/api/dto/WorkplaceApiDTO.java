package dk.digitalidentity.os2vikar.api.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkplaceApiDTO {
	private String masterId;
	private String orgUnit;
	private String title;
	private LocalDate startDate;
	private LocalDate stopDate;
	
}
