package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "statistic")
public class Statistic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long workplaceId;

	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;

	@Column
	private String orgunitUuid;

	@Column
	private String orgunitName;

	@Column
	private String substituteName;

	@Column
	private String substituteUserId;

}
