package dk.digitalidentity.os2vikar.dao.model;

import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "global_title")
public class GlobalTitle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String title;
	
	@OneToMany(mappedBy = "globalTitle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<GlobalTitleADGroupMapping> adGroups;

	@OneToMany(mappedBy = "globalTitle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<GlobalTitleUserRoleMapping> automaticUserRoles;

	@ElementCollection
	@CollectionTable(name = "global_title_constraint_it_system", joinColumns = @JoinColumn(name = "title_id"))
	@Column(name = "it_system")
	private Set<String> itSystems;
}
