package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "workplace")
public class Workplace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String masterId;

	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_id")
	private Substitute substitute;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	private OrgUnit orgUnit;

	@Column
	private String title;

	@OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<WorkplaceAssignedRole> assignedRoles;
	
	@Column(name = "require_o365_license")
	private boolean requireO365License;

}
