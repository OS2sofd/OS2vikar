package dk.digitalidentity.os2vikar.controller.mvc.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.os2vikar.controller.mvc.dto.enums.Status;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class SubstituteDTO {
	private long id;
	private String name;
	private String surname;
	private String userName;
	private Status status;
	private List<WorkplaceDTO> workplaces;
	private String agency;
	private String email;
	private String phone;
	private String workplaceString;

	public SubstituteDTO(Substitute substitute, boolean mobile, long treshold) {
		this.id = substitute.getId();
		this.name = substitute.getName();
		this.surname = substitute.getSurname();

		LocalDate nowStart = LocalDate.now();
		LocalDate nowStop = LocalDate.now().minusDays(treshold);
		List<Workplace> activeWorkplaces = substitute.getWorkplaces()
				.stream()
				.filter(w -> (w.getStartDate().equals(nowStart) || w.getStartDate().isBefore(nowStart)) &&
							 (w.getStopDate().equals(nowStop) || w.getStopDate().isAfter(nowStop)))
				.collect(Collectors.toList());

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<ul class=\"workplaces\">");
		for (Workplace workplace : activeWorkplaces) {
			stringBuilder.append("<li>").append(workplace.getTitle()).append(" i ").append(workplace.getOrgUnit().getName()).append(" (").append(workplace.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(" til ").append(workplace.getStopDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(")</li>");
		}
		stringBuilder.append("</ul>");
		this.workplaceString = stringBuilder.toString();

		if (mobile) {
			List<Workplace> futureWorkplaces = substitute.getWorkplaces().stream().filter(w -> w.getStartDate().isAfter(nowStart)).collect(Collectors.toList());
			if (!activeWorkplaces.isEmpty()) {
				this.status = Status.GREEN;
			}
			else if (!futureWorkplaces.isEmpty()) {
				this.status = Status.YELLOW;
			}
			else {
				this.status = Status.RED;
			}
		}
		else {
			this.agency = substitute.getAgency();
			this.userName = substitute.getUsername();
		}
	}
}
