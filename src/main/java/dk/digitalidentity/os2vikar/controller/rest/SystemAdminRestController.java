package dk.digitalidentity.os2vikar.controller.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.validation.Valid;

import dk.digitalidentity.os2vikar.dao.model.GlobalTitleUserRoleMapping;
import dk.digitalidentity.os2vikar.dao.model.OrgUnitUserRoleMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.model.ADGroup;
import dk.digitalidentity.os2vikar.dao.model.GlobalRole;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitleADGroupMapping;
import dk.digitalidentity.os2vikar.dao.model.LocalRole;
import dk.digitalidentity.os2vikar.dao.model.LocalTitle;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.dao.model.OrgUnitADGroupMapping;
import dk.digitalidentity.os2vikar.dao.model.UserRole;
import dk.digitalidentity.os2vikar.datatables.AuditLogDatatablesDao;
import dk.digitalidentity.os2vikar.datatables.GlobalRoleDatatablesDao;
import dk.digitalidentity.os2vikar.datatables.model.AuditLogDatatable;
import dk.digitalidentity.os2vikar.datatables.model.GlobalRoleDatatable;
import dk.digitalidentity.os2vikar.interceptors.AuditLogIntercepted;
import dk.digitalidentity.os2vikar.security.RequireSystemAdminAccess;
import dk.digitalidentity.os2vikar.service.ADGroupService;
import dk.digitalidentity.os2vikar.service.GlobalRoleService;
import dk.digitalidentity.os2vikar.service.GlobalTitleService;
import dk.digitalidentity.os2vikar.service.LocalRoleService;
import dk.digitalidentity.os2vikar.service.LocalTitleService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import dk.digitalidentity.os2vikar.service.SettingsService;
import dk.digitalidentity.os2vikar.service.UserRoleService;
import dk.digitalidentity.os2vikar.service.dto.DeleteAfterPeriod;

@RestController
@RequireSystemAdminAccess
public class SystemAdminRestController {
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private GlobalTitleService globalTitleService;
	
	@Autowired 
	private GlobalRoleService globalRoleService;
	
	@Autowired
	private LocalTitleService localTitleService;
	
	@Autowired 
	private LocalRoleService localRoleService;
	
	@Autowired
	private UserRoleService userRoleService;
	
	@Autowired
	private AuditLogDatatablesDao auditLogDatatablesDao;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private GlobalRoleDatatablesDao globalRoleDatatablesDao;
	
	@Autowired
	private OS2VikarConfiguration configuration;
	
	@Autowired
	private ADGroupService adGroupService;

	@Autowired
	private OS2VikarConfiguration os2VikarConfiguration;
	
	@AuditLogIntercepted(operation = "Slet lokal arbejdstitel", args = {"id"})
	@PostMapping("rest/systemadmin/localtitles/{id}/delete")
	public ResponseEntity<?> deleteLocalTitle(@PathVariable long id) {
		LocalTitle title = localTitleService.getById(id);
		if (title == null) {
			return new ResponseEntity<>("Kunne ikke finde lokal arbejdstitel med id " + id, HttpStatus.BAD_REQUEST);
		}
		
		localTitleService.delete(title);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Opret lokal arbejdstitel på enhed", args = {"title", "uuid"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/localtitles/save")
	public ResponseEntity<?> saveLocalTitle(@RequestParam String title, @PathVariable String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		
		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}
		
		LocalTitle localTitle = new LocalTitle();
		localTitle.setTitle(title);
		localTitle.setOrgUnit(orgUnit);
		localTitleService.save(localTitle);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Administrer lokal rettighed på enhed", args = {"uuid", "id", "checked"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/localroles/save")
	public ResponseEntity<?> saveLocalRole(@PathVariable String uuid, @RequestParam Long id, @RequestParam boolean checked) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		
		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}
		
		if (checked) {
			UserRole userRole = userRoleService.getById(id);
			if (userRole == null) {
				return new ResponseEntity<>("Kunne ikke finde jobfunktionsrolle med id " + id, HttpStatus.BAD_REQUEST);
			}
			LocalRole role = new LocalRole();
			role.setUserRole(userRole);
			role.setOrgUnit(orgUnit);
			localRoleService.save(role);
		} else {
			LocalRole role = orgUnit.getLocalRoles().stream().filter(l -> l.getUserRole().getId() == id).findAny().orElse(null);
			if (role == null) {
				return new ResponseEntity<>("Kunne ikke finde lokal rettighed med jobfunktionsrolle med id " + id, HttpStatus.BAD_REQUEST);
			}
			
			localRoleService.delete(role);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Administrer Active Directory gruppe på enhed", args = {"uuid", "guid", "checked"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/adgroups/save")
	public ResponseEntity<?> saveActiveDirectoryGroup(@PathVariable String uuid, @RequestParam String guid, @RequestParam boolean checked) {
		if (!configuration.getSyncADGroups().isEnabled()) {
			return new ResponseEntity<>("AD gruppe synkronisering er ikke slået til", HttpStatus.BAD_REQUEST);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);

		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}
		
		if (checked) {
			ADGroup adGroup = adGroupService.getByObjectGuid(guid);
			if (adGroup == null) {
				return new ResponseEntity<>("Kunne ikke finde ADGroup med guid " + guid, HttpStatus.BAD_REQUEST);
			}
			
			if (orgUnit.getAdGroups() == null) {
				orgUnit.setAdGroups(new ArrayList<>());
			}
			
			OrgUnitADGroupMapping mapping = new OrgUnitADGroupMapping();
			mapping.setAdGroup(adGroup);
			mapping.setOrgUnit(orgUnit);
			orgUnit.getAdGroups().add(mapping);
			orgUnitService.save(orgUnit);
		} else {
			orgUnit.getAdGroups().removeIf(g -> g.getAdGroup().getObjectGuid().equals(guid));
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Administrer jobfunktionsrolle på enhed", args = {"uuid", "id", "checked"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/userroles/save")
	public ResponseEntity<?> saveUserRoleOnOrgUnit(@PathVariable String uuid, @RequestParam long id, @RequestParam boolean checked) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);

		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}

		if (checked) {
			UserRole userRole = userRoleService.getById(id);
			if (userRole == null) {
				return new ResponseEntity<>("Kunne ikke finde userRole med id " + id, HttpStatus.BAD_REQUEST);
			}

			if (orgUnit.getAutomaticUserRoles() == null) {
				orgUnit.setAutomaticUserRoles(new ArrayList<>());
			}

			OrgUnitUserRoleMapping mapping = new OrgUnitUserRoleMapping();
			mapping.setUserRole(userRole);
			mapping.setOrgUnit(orgUnit);
			orgUnit.getAutomaticUserRoles().add(mapping);
		} else {
			orgUnit.getAutomaticUserRoles().removeIf(g -> g.getUserRole().getId() == id);
		}

		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Administrer IT-system tilknyttet til en enhed", args = {"uuid", "identifier", "checked"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/itsystems/save")
	public ResponseEntity<?> saveITSystem(@PathVariable String uuid, @RequestParam String identifier, @RequestParam boolean checked) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}

		if (checked) {
			if (os2VikarConfiguration.getConstraintITSystems().stream().noneMatch(i -> i.getIdentifier().equals(identifier))) {
				return new ResponseEntity<>("Kunne ikke finde IT-system med identifier " + identifier, HttpStatus.BAD_REQUEST);
			}

			if (orgUnit.getItSystems() == null) {
				orgUnit.setItSystems(new HashSet<>());
			}

			orgUnit.getItSystems().add(identifier);
			orgUnitService.save(orgUnit);
		} else {
			orgUnit.getItSystems().removeIf(i -> i.equals(identifier));
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Slet global arbejdstitel", args = {"id"})
	@PostMapping("rest/systemadmin/globaltitles/{id}/delete")
	public ResponseEntity<?> deleteGlobalTitle(@PathVariable long id) {
		GlobalTitle title = globalTitleService.getById(id);
		if (title == null) {
			return new ResponseEntity<>("Kunne ikke finde global arbejdstitel med id " + id, HttpStatus.BAD_REQUEST);
		}
		
		globalTitleService.delete(title);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	record CreateGlobalTitleDTO (long id, String title, List<String> adGroupObjectGuids, List<String> systemIdentifiers, List<Long> userRoleIds) {}
	@AuditLogIntercepted(operation = "Administrer global arbejdstitel", args = {"dto"})
	@PostMapping("rest/systemadmin/globaltitles/save")
	public ResponseEntity<?> saveGlobalTitle(@RequestBody CreateGlobalTitleDTO dto) {
		GlobalTitle globalTitle = globalTitleService.getById(dto.id());
		if (globalTitle == null) {
			globalTitle = new GlobalTitle();
			globalTitle.setTitle(dto.title());
		}

		if (configuration.getSyncADGroups().isEnabled() && dto.adGroupObjectGuids() != null) {
			if (globalTitle.getAdGroups() == null) {
				globalTitle.setAdGroups(new ArrayList<>());
			}

			for (String guid : dto.adGroupObjectGuids()) {
				ADGroup adGroup = adGroupService.getByObjectGuid(guid);
				if (adGroup == null) {
					return new ResponseEntity<>("Kunne ikke finde ADGroup med guid " + guid, HttpStatus.BAD_REQUEST);
				}

				boolean foundMatch = globalTitle.getAdGroups().stream().anyMatch(t -> t.getAdGroup().getObjectGuid().equals(guid));
				if (foundMatch) {
					continue;
				}

				GlobalTitleADGroupMapping mapping = new GlobalTitleADGroupMapping();
				mapping.setAdGroup(adGroup);
				mapping.setGlobalTitle(globalTitle);
				globalTitle.getAdGroups().add(mapping);
			}

			globalTitle.getAdGroups().removeIf(g -> !dto.adGroupObjectGuids().contains(g.getAdGroup().getObjectGuid()));
		}

		if (dto.systemIdentifiers != null) {
			if (globalTitle.getItSystems() == null) {
				globalTitle.setItSystems(new HashSet<>());
			}

			for (String identifier : dto.systemIdentifiers()) {
				if (os2VikarConfiguration.getConstraintITSystems().stream().noneMatch(i -> i.getIdentifier().equals(identifier))) {
					return new ResponseEntity<>("Kunne ikke finde IT-system med identifier " + identifier, HttpStatus.BAD_REQUEST);
				}

				boolean foundMatch = globalTitle.getItSystems().contains(identifier);
				if (foundMatch) {
					continue;
				}

				globalTitle.getItSystems().add(identifier);
			}

			globalTitle.getItSystems().removeIf(i -> !dto.systemIdentifiers().contains(i));
		}

		if (dto.userRoleIds != null) {
			if (globalTitle.getAutomaticUserRoles() == null) {
				globalTitle.setAutomaticUserRoles(new ArrayList<>());
			}

			for (long id : dto.userRoleIds()) {
				UserRole userRole = userRoleService.getById(id);
				if (userRole == null) {
					return new ResponseEntity<>("Kunne ikke finde userRole med id " + id, HttpStatus.BAD_REQUEST);
				}

				boolean foundMatch = globalTitle.getAutomaticUserRoles().stream().anyMatch(t -> t.getUserRole().getId() == id);
				if (foundMatch) {
					continue;
				}

				GlobalTitleUserRoleMapping mapping = new GlobalTitleUserRoleMapping();
				mapping.setUserRole(userRole);
				mapping.setGlobalTitle(globalTitle);
				globalTitle.getAutomaticUserRoles().add(mapping);
			}

			globalTitle.getAutomaticUserRoles().removeIf(g -> !dto.userRoleIds().contains(g.getUserRole().getId()));
		}
		
		globalTitleService.save(globalTitle);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Administrer global rettighed", args = {"id", "checked"})
	@PostMapping("rest/systemadmin/globalroles/save")
	public ResponseEntity<?> saveGlobalRole(@RequestParam Long id, @RequestParam boolean checked) {
		if (checked) {
			UserRole userRole = userRoleService.getById(id);
			if (userRole == null) {
				return new ResponseEntity<>("Kunne ikke finde jobfunktionsrolle med id " + id, HttpStatus.BAD_REQUEST);
			}
			GlobalRole role = new GlobalRole();
			role.setUserRole(userRole);
			globalRoleService.save(role);
		}
		else {
			GlobalRole role = globalRoleService.getAll().stream().filter(g -> g.getUserRole().getId() == id).findAny().orElse(null);
			if (role == null) {
				return new ResponseEntity<>("Kunne ikke finde global rettighed med jobfunktionsrolle med id " + id, HttpStatus.BAD_REQUEST);
			}
			
			globalRoleService.delete(role);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Sæt om enhed må have vikarer", args = {"allowed", "uuid"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/substitutes")
	public ResponseEntity<?> setSubstitutesAllowed(@RequestParam boolean allowed, @PathVariable String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		
		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}
		
		orgUnit.setCanHaveSubstitutes(allowed);
		if (allowed && orgUnit.getMaxSubstituteWorkingDays() == 0) {
			orgUnit.setMaxSubstituteWorkingDays(configuration.getDefaultMaxDays());
			orgUnit.setDefaultSubstituteWorkingDays(configuration.getDefaultMaxDays());
		}
		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@AuditLogIntercepted(operation = "Sæt det maksimale antal arbejdsdage for vikarer i en enhed", args = {"days", "uuid"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/maxworkingdays")
	public ResponseEntity<?> setMaxWorkingDays(@RequestParam int days, @PathVariable String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		
		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}
		
		orgUnit.setMaxSubstituteWorkingDays(days);
		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Sæt default antal arbejdsdage for vikarer i en enhed", args = {"days", "uuid"})
	@PostMapping("rest/systemadmin/orgunits/{uuid}/defaultworkingdays")
	public ResponseEntity<?> setDefaultWorkingDays(@RequestParam int days, @PathVariable String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);

		if (orgUnit == null) {
			return new ResponseEntity<>("Kunne ikke finde enhed med uuid " + uuid, HttpStatus.BAD_REQUEST);
		}

		if (days > orgUnit.getMaxSubstituteWorkingDays()) {
			return new ResponseEntity<>("Standard arbejdstid for vikar må ikke være større end den maksimale", HttpStatus.BAD_REQUEST);
		}

		orgUnit.setDefaultSubstituteWorkingDays(days);
		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/systemadmin/auditlog")
	public DataTablesOutput<AuditLogDatatable> adminEventLogsDataTable(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<AuditLogDatatable> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		return auditLogDatatablesDao.findAll(input);
	}

	record settingDTO(boolean deleteAccountInAD, DeleteAfterPeriod deleteSubstituteAfter) {}
	@PostMapping("/rest/systemadmin/settings")
	public ResponseEntity<?> setSettings(@RequestBody settingDTO dto) {
		settingsService.setBooleanValueForKey(SettingsService.DELETE_ACCOUNT_IN_AD, dto.deleteAccountInAD);
		settingsService.setDeleteSubstituteAfter(dto.deleteSubstituteAfter);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/systemadmin/globalroles")
	public DataTablesOutput<GlobalRoleDatatable> adminGlobalRolesDataTable(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<GlobalRoleDatatable> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		return globalRoleDatatablesDao.findAll(input);
	}
}
