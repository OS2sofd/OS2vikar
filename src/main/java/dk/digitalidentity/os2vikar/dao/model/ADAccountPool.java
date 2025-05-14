package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import dk.digitalidentity.os2vikar.dao.model.enums.ADAccountPoolStatus;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "ad_account_pool")
@Getter
@Setter
public class ADAccountPool {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String username;

	@Column(name = "with_o365_license")
	private boolean withO365License;

	@Column
	private LocalDateTime created;

	@Column
	@Enumerated(EnumType.STRING)
	private ADAccountPoolStatus status;

	@Column
	private String statusMessage;
}
