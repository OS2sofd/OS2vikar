package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "substitute")
public class Substitute {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String uuid;

	@Column
	private String cpr;

	@Column(name = "firstname")
	private String name;

	@Column
	private String surname;

	@Column
	private boolean disabledInAd;

	@Column
	private LocalDateTime lastUpdated;

	@Column
	@CreationTimestamp
	private LocalDateTime created;

	@Column
	private LocalDate latestStopDate;

	@Column
	private LocalDateTime lastPwdChange;

	@Column
	private String username;

	@Column
	private String email;

	@Column
	private String phone;

	@Column
	private String agency;

	@Column
	private boolean assignEmployeeSignature;

	@Column
	private boolean usernameFromSofd;

	@OneToMany(mappedBy = "substitute", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<Workplace> workplaces;

	@Column(name = "has_o365_license")
	private boolean hasO365License;

	@Column
	private String authorizationCode;

	@Column
	private boolean authorizationCodeChecked;
}
