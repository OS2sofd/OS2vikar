package dk.digitalidentity.os2vikar.datatables.model;

import java.time.LocalDateTime;

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
@Table(name = "auditlog")
public class AuditLogDatatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(name = "tts")
	private LocalDateTime timestamp;
	
	@Column
	private String ip;
	
	@Column
	private String administrator;

	@Column
	private String substitute;
	
	@Column
	private String operation;
	
	@Column
	private String details;
}
