package dk.digitalidentity.os2vikar.controller.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.controller.mvc.dto.RoleDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.SubstituteDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.SubstituteWithPlaceDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.TitleDTO;
import dk.digitalidentity.os2vikar.controller.rest.dto.OUDataDTO;
import dk.digitalidentity.os2vikar.dao.model.ADAccountPool;
import dk.digitalidentity.os2vikar.dao.model.AuthorizationCode;
import dk.digitalidentity.os2vikar.dao.model.GlobalRole;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitleUserRoleMapping;
import dk.digitalidentity.os2vikar.dao.model.LocalRole;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.dao.model.OrgUnitUserRoleMapping;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.UserRole;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.dao.model.WorkplaceAssignedRole;
import dk.digitalidentity.os2vikar.dao.model.enums.ADAccountPoolStatus;
import dk.digitalidentity.os2vikar.datatables.SubstituteDatatablesDao;
import dk.digitalidentity.os2vikar.interceptors.AuditLogIntercepted;
import dk.digitalidentity.os2vikar.interceptors.LogInterceptor;
import dk.digitalidentity.os2vikar.security.RequireSubstituteAdminAccess;
import dk.digitalidentity.os2vikar.security.SecurityUtil;
import dk.digitalidentity.os2vikar.service.ADAccountPoolService;
import dk.digitalidentity.os2vikar.service.AuthorizationCodeService;
import dk.digitalidentity.os2vikar.service.CprService;
import dk.digitalidentity.os2vikar.service.GlobalRoleService;
import dk.digitalidentity.os2vikar.service.GlobalTitleService;
import dk.digitalidentity.os2vikar.service.LocalRoleService;
import dk.digitalidentity.os2vikar.service.NexusSyncService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import dk.digitalidentity.os2vikar.service.PasswordChangeQueueService;
import dk.digitalidentity.os2vikar.service.PasswordService;
import dk.digitalidentity.os2vikar.service.SofdService;
import dk.digitalidentity.os2vikar.service.StatisticService;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import dk.digitalidentity.os2vikar.service.WebSocketService;
import dk.digitalidentity.os2vikar.service.dto.CprLookupDto;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireSubstituteAdminAccess
public class SubstituteAdminRestController {

	@Autowired
	private CprService cprService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private GlobalTitleService globalTitleService;

	@Autowired
	private GlobalRoleService globalRoleService;

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private LocalRoleService localRoleService;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private PasswordChangeQueueService passwordChangeQueueService;
	
	@Autowired
	private ADAccountPoolService adAccountPoolService;
	
	@Autowired
	private WebSocketService webSocketService;

	@Autowired
	private LogInterceptor logInterceptor;

	@Autowired
	private OS2VikarConfiguration config;

	@Autowired
	private NexusSyncService nexusSyncService;
	
	@Autowired
	private SofdService sofdService;

	@Autowired
	private AuthorizationCodeService authorizationCodeService;

	@Autowired
	private SubstituteDatatablesDao substituteDatatablesDao;
	
	@Autowired
	private StatisticService statisticService;

	@AuditLogIntercepted(operation = "CPR-opslag", args = { "cpr" })
	@GetMapping("/rest/substituteadmin/substitutes/new/cprlookup/{cpr}")
	public ResponseEntity<?> cprLookup(@PathVariable String cpr) {
		CprLookupDto cprLookupDto = cprService.cprLookup(cpr);

		if (cprLookupDto == null) {
			return new ResponseEntity<>("Kunne ikke gennemføre cpr-opslag", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		Substitute substitute = substituteService.getByCpr(cpr);
		if (substitute != null) {
			cprLookupDto.setExists(true);
		}

		SofdService.InSofdResult result = sofdService.isInSofd(cpr);
		if (result != null && result.inSofd()) {
			cprLookupDto.setInSofd(true);
			cprLookupDto.setHasSofdADUser(result.hasADUser());
			cprLookupDto.setSofdADUserId(result.ADUsername());
		}

		return new ResponseEntity<>(cprLookupDto, HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Henter data fra enhed(er)", args = { "uuids" })
	@GetMapping("/rest/substituteadmin/orgunits/oudata")
	public ResponseEntity<?> getOUDataFromOUs(@RequestParam String uuids) {
		List<OrgUnit> orgUnits = getOrgUnitsFromUuids(uuids);
		if (orgUnits.isEmpty()) {
			return new ResponseEntity<>("Kunne ikke finde de specificerede enheder", HttpStatus.BAD_REQUEST);
		}

		if (!allSelectedOrgUnitsHaveSameItSystemConstraints(orgUnits)) {
			return new ResponseEntity<>("Alle valgte enheder skal have de samme IT-systemer tilknyttet af hensyn til arbejdstitler. Arbejdsstederne kan i stedet oprettes enkeltvis.", HttpStatus.BAD_REQUEST);
		}

		List<RoleDTO> roles = getRoles(orgUnits);
		roles.addAll(globalRoleService.getAll().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList()));

		List<TitleDTO> titles = getTitles(orgUnits);
		int maxDays = orgUnits.stream().mapToInt(o -> o.getMaxSubstituteWorkingDays()).max().orElse(1);
		int defaultDays = orgUnits.stream().mapToInt(o -> o.getDefaultSubstituteWorkingDays()).min().orElse(1);

		OUDataDTO ouData = new OUDataDTO();
		ouData.setRoles(roles);
		ouData.setTitles(titles);
		ouData.setMaxDays(maxDays);
		ouData.setDefaultDays(defaultDays);

		return new ResponseEntity<>(ouData, HttpStatus.OK);
	}

	private List<RoleDTO> getRoles(List<OrgUnit> orgUnits) {
		List<RoleDTO> roles = new ArrayList<RoleDTO>();
		List<Long> addedLocalRoles = new ArrayList<>();

		for (OrgUnit ou : orgUnits) {
			List<RoleDTO> localRoles = ou.getLocalRoles().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList());
			
			for (RoleDTO role : localRoles) {
				if (!addedLocalRoles.contains(role.getId())) {
					addedLocalRoles.add(role.getId());
					roles.add(role);
				}
			}
		}

		return roles;
	}

	private List<TitleDTO> getTitles(List<OrgUnit> orgUnits) {
		List<TitleDTO> titles = new ArrayList<TitleDTO>();
		Set<String> addedTitles = new HashSet<>();

		// add local titles - they are never filtered
		for (OrgUnit ou : orgUnits) {
			List<TitleDTO> localTitles = ou.getLocalTitles().stream().map(t -> new TitleDTO(t)).collect(Collectors.toList());

			for (TitleDTO title : localTitles) {
				if (!addedTitles.contains(title.getTitle())) {
					addedTitles.add(title.getTitle());
					titles.add(title);
				}
			}
		}

		// it is assumed they are identical for all OUs, so we can just grab the first one
		Set<String> orgUnitConstraintItSystems = orgUnits.get(0).getItSystems() == null ? new HashSet<>() : orgUnits.get(0).getItSystems();

		// user might have constraints based on incoming role
		Set<String> userConstraintItSystems = SecurityUtil.getITSystemConstraint(config.getConstraintItSystemIdentifier());

		// now load global titles, and perform filtering
		int added = 0;
		List<GlobalTitle> globalTitles = globalTitleService.getAll();
		for (GlobalTitle globalTitle : globalTitles) {
			boolean filtered = false;

			// any title without it-systems on it is never filtered
			if (!globalTitle.getItSystems().isEmpty()) {

				// filter on orgUnit constraints
				if (!orgUnitConstraintItSystems.isEmpty()) {
					boolean match = false;
					
					for (String titleItSystem : globalTitle.getItSystems()) {
						for (String constraintItSystem : orgUnitConstraintItSystems) {
							if (Objects.equals(constraintItSystem, titleItSystem)) {
								match = true;
								break;
							}
						}
						
						if (match) {
							break;
						}
					}
					
					if (!match) {
						filtered = true;
					}
				}
				
				// filter on user constraints
				if (!userConstraintItSystems.isEmpty()) {
					boolean match = false;

					for (String titleItSystem : globalTitle.getItSystems()) {
						for (String constraintItSystem : userConstraintItSystems) {
							if (Objects.equals(constraintItSystem, titleItSystem)) {
								match = true;
								break;
							}
						}
						
						if (match) {
							break;
						}
					}
					
					if (!match) {
						filtered = true;
					}
				}
			}
			
			if (!filtered) {
				added++;
				titles.add(new TitleDTO(globalTitle));
			}
		}
		
		log.info("Filtered " + globalTitles.size() + " titles to " + added);

		return titles;
	}

	private boolean allSelectedOrgUnitsHaveSameItSystemConstraints(List<OrgUnit> orgUnits) {
		Set<String> commonConstraintSystems = orgUnits.get(0).getItSystems() == null ? new HashSet<>() : orgUnits.get(0).getItSystems();
		
		for (OrgUnit orgUnit : orgUnits) {
			boolean allMatches = true;

			if (orgUnit.getItSystems() == null || orgUnit.getItSystems().isEmpty()) {
				if (!commonConstraintSystems.isEmpty()) {
					allMatches = false;
				}
			}
			else {
				for (String itSystem : orgUnit.getItSystems()) {
					if (!commonConstraintSystems.contains(itSystem)) {
						allMatches = false;
						break;
					}
				}

				// check the other way
				if (allMatches) {
					for (String itSystem : commonConstraintSystems) {
						if (!orgUnit.getItSystems().contains(itSystem)) {
							allMatches = false;
							break;
						}
					}
				}
			}

			if (!allMatches) {
				return false;
			}
		}
		
		return true;
	}

	@AuditLogIntercepted(operation = "Opret vikar", args = { "dto" })
	@ResponseBody
	@PostMapping("/rest/substituteadmin/substitutes/save")
	public ResponseEntity<?> save(@RequestBody SubstituteWithPlaceDTO dto) {
		try {
			if (dto.getCpr() == null || dto.getCpr().length() != 10) {
				throw new Exception("Ugyldig cpr værdi");
			}
			
	    	String day = dto.getCpr().substring(0, 2);
	    	String month = dto.getCpr().substring(2, 4);
	    	String yearString = dto.getCpr().substring(4, 6);
	    	int year = Integer.parseInt(yearString);
	    	
	    	switch (dto.getCpr().charAt(6)) {
		    	case '0':
		    	case '1':
		    	case '2':
		    	case '3':
		    		yearString = "19" + yearString;
		    		break;
		    	case '4':
		    	case '9':
		    		if (year <= 36) {
			    		yearString = "20" + yearString;		    			
		    		}
		    		else {
			    		yearString = "19" + yearString;
		    		}
		    		break;
		    	case '5':
		    	case '6':
		    	case '7':
		    	case '8':
		    		if (year <= 57) {
			    		yearString = "20" + yearString;		    			
		    		}
		    		else {
			    		yearString = "18" + yearString;
		    		}
		    		break;
	    		default:
	    			throw new Exception();
	    	}
	    	
	    	String date = yearString + "-" + month + "-" + day;

			LocalDate.parse(date);
		}
		catch (Exception ex) {
			return new ResponseEntity<>("Ugyldigt personnummer", HttpStatus.BAD_REQUEST);			
		}

		Substitute substitute = substituteService.getByCpr(dto.getCpr());
		if (substitute != null) {
			return new ResponseEntity<>("Der findes allerede en vikar med dette personnummer.", HttpStatus.BAD_REQUEST);
		}

		List<OrgUnit> orgUnits = getOrgUnitsFromUuids(dto.getOrgUnitUuids());
		if (orgUnits.isEmpty()) {
			return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
		}

		if (dto.getCpr().trim().length() == 0 || dto.getName().trim().length() == 0 || dto.getSurname().trim().length() == 0 || dto.getTitle() == null || dto.getStart() == null || dto.getStop() == null) {
			return new ResponseEntity<>("Felterne med * skal udfyldes", HttpStatus.BAD_REQUEST);
		}
		
		if (dto.getStart().isAfter(dto.getStop())) {
			return new ResponseEntity<>("Startdatoen kan ikke være efter stopdatoen", HttpStatus.BAD_REQUEST);
		}

		int maxDays = getMaxDays(orgUnits);
		LocalDate maxStop = dto.getStart().plusDays(maxDays);
		if (dto.getStop().isAfter(maxStop)) {
			return new ResponseEntity<>("Den maksimale stopdato er overskredet", HttpStatus.BAD_REQUEST);
		}
		
		if (dto.getTitle().equals("0") || dto.getTitle() == null) {
			return new ResponseEntity<>("Der skal vælges en arbejdstitel", HttpStatus.BAD_REQUEST);
		}

		if (dto.isRequireO365License() && adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, true).isEmpty()) {
			return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere med Office licens i puljen.", HttpStatus.BAD_REQUEST);
		}
		else if (!dto.isRequireO365License() && adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, false).isEmpty()) {
			return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere uden Office licens i puljen.", HttpStatus.BAD_REQUEST);
		}

		// create substitute
		substitute = new Substitute();
		substitute.setCpr(dto.getCpr());
		substitute.setName(dto.getName());
		substitute.setSurname(dto.getSurname());
		substitute.setEmail(dto.getEmail());
		substitute.setAgency(dto.getAgency());
		substitute.setPhone(dto.getPhone());
		substitute.setAssignEmployeeSignature(dto.isAssignEmployeeSignature());

		// handle roles
		List<GlobalRole> globalRoles = new ArrayList<>();
		List<LocalRole> localRoles = new ArrayList<>();
		for (RoleDTO role : dto.getRoles()) {
			GlobalRole globalRole = null;
			LocalRole localRole = null;
			String type = "";

			if (role.isGlobal()) {
				globalRole = globalRoleService.getById(role.getId());
				type = "global";
			} else {
				localRole = localRoleService.getById(role.getId());
				type = "lokal";
			}

			if (globalRole == null && localRole == null) {
				return new ResponseEntity<>("Kunne ikke finde " + type + " rolle med id " + role.getId(), HttpStatus.BAD_REQUEST);
			} else if (globalRole != null) {
				globalRoles.add(globalRole);
			} else if (localRole != null) {
				localRoles.add(localRole);
			}
		}

		// handle creation of workplaces
		List<GlobalTitle> globalTitles = globalTitleService.getAll();
		List<Workplace> workplaces = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnits) {
			Workplace workplace = new Workplace();
			workplace.setStartDate(dto.getStart());
			workplace.setStopDate(dto.getStop());
			workplace.setOrgUnit(orgUnit);
			workplace.setMasterId(UUID.randomUUID().toString());
			workplace.setTitle(dto.getTitle());
			workplace.setRequireO365License(dto.isRequireO365License());

			List<WorkplaceAssignedRole> assignedRoles = new ArrayList<>();
			for (GlobalRole global : globalRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, global.getUserRole());
			}
			
			for (LocalRole local : localRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, local.getUserRole());
			}

			for (OrgUnitUserRoleMapping orgUnitAutomaticUserRole : orgUnit.getAutomaticUserRoles()) {
				createWorkplaceAssignedRole(workplace, assignedRoles, orgUnitAutomaticUserRole.getUserRole());
			}

			GlobalTitle globalTitle = globalTitles.stream().filter(g -> g.getTitle().equals(dto.getTitle())).findAny().orElse(null);
			if (globalTitle != null) {
				for (GlobalTitleUserRoleMapping titleAutomaticUserRole : globalTitle.getAutomaticUserRoles()) {
					createWorkplaceAssignedRole(workplace, assignedRoles, titleAutomaticUserRole.getUserRole());
				}
			}

			workplace.setAssignedRoles(assignedRoles);
			workplace.setSubstitute(substitute);
			workplaces.add(workplace);
		}

		substitute.setWorkplaces(workplaces);
		substitute.setLatestStopDate(dto.getStop());

		// handling AD account
		boolean responseTimeout = false;
		String failAssignmentAccount = "";
		if (dto.isUseSofdADUser() && StringUtils.hasLength(dto.getSofdADUserId())) {
			substitute.setUsername(dto.getSofdADUserId());
			substitute.setUsernameFromSofd(true);
		}
		else {
			ADAccountPool account = adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, dto.isRequireO365License()).stream().findFirst().orElse(null);
			if (account == null) {
				String word = dto.isRequireO365License()? "med" : "uden";

				return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere " + word + " Office licens i puljen.", HttpStatus.BAD_REQUEST);
			}

			ADResponse response = webSocketService.associateAccount(substitute, account.getUsername());
			if (response.isSuccess() || response.getStatus().equals(ADStatus.TIMEOUT)) {
				substitute.setUsername(account.getUsername());
				substitute.setHasO365License(dto.isRequireO365License());
				adAccountPoolService.delete(account);
			}
			else {
				account.setStatus(ADAccountPoolStatus.BROKEN);
				account.setStatusMessage(response.getMessage());
				adAccountPoolService.save(account);

				account = adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, dto.isRequireO365License()).stream().findFirst().orElse(null);
				if (account == null) {
					String word = dto.isRequireO365License()? "med" : "uden";
					return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere " + word + " Office licens i puljen.", HttpStatus.BAD_REQUEST);
				}

				response = webSocketService.associateAccount(substitute, account.getUsername());
				if (response.isSuccess() || response.getStatus().equals(ADStatus.TIMEOUT)) {
					substitute.setUsername(account.getUsername());
					substitute.setHasO365License(dto.isRequireO365License());
					adAccountPoolService.delete(account);
				}
				else {
					account.setStatus(ADAccountPoolStatus.BROKEN);
					account.setStatusMessage(response.getMessage());
					adAccountPoolService.save(account);

					log.error("Second attempt of linking AD account to substitute " + substitute.getName() + " " + substitute.getSurname() + " failed.");
					return new ResponseEntity<>("Vikaren kan ikke oprettes. Der skete en fejl under tildelingen af AD konto.", HttpStatus.BAD_REQUEST);
				}

				// handling employee signature
				if (dto.isAssignEmployeeSignature()) {
					ADResponse responseEmployeeSignature = webSocketService.addToEmployeeSignatureGroup(substitute);
					if (!responseEmployeeSignature.isSuccess() && !responseEmployeeSignature.getStatus().equals(ADStatus.TIMEOUT)) {
						log.error("Failed to add substitute to employee signature AD group. Substitute " + substitute.getName() + " " + substitute.getSurname() + " failed.");
						return new ResponseEntity<>("Vikaren kan ikke oprettes. Der skete en fejl under oprettelse af medarbejdersignatur.", HttpStatus.BAD_REQUEST);
					}
				}

				if (response.getStatus().equals(ADStatus.TIMEOUT)) {
					responseTimeout = true;
					failAssignmentAccount = account.getUsername();
				}
			}
		}

		substitute = substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);

		for (Workplace workplace : substitute.getWorkplaces()) {
			statisticService.save(workplace);
		}

		// handling ad groups
		if (config.getSyncADGroups().isEnabled()) {
			ADResponse groupStatus = substituteService.syncADGroups(substitute);
			if (!groupStatus.isSuccess() && !groupStatus.getStatus().equals(ADStatus.TIMEOUT)) {
				if (responseTimeout) {
					log.error("A timeout happend when associating AD account with userId " + failAssignmentAccount + " to substitute with id " + substitute.getId());
					log.error("Adding substitute to indirectly assigned AD groups. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ") failed.");
					
					return new ResponseEntity<>("Vikaren er oprettet,men er stadig igang med at få tildelt en AD konto. Vikaren er også stadig igang med at blive medlem af de inddirekte tildelte Active Directory grupper. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
				}
				else {
					log.warn("Adding substitute to indirectly assigned AD groups. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ") failed.");
					return new ResponseEntity<>("Vikaren er oprettet, men er stadig ved at blive medlem af de inddirekte tildelte Active Directory grupper. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
				}
			}
		}

		if (responseTimeout) {
			log.error("A timeout happend when associating AD account with userId " + failAssignmentAccount + " to substitute with id " + substitute.getId());

			return new ResponseEntity<>("Vikaren er oprettet, men er stadig igang med at få tildelt en AD konto. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Rediger vikar stamdata", args = { "dto" })
	@ResponseBody
	@PostMapping("/rest/substituteadmin/substitutes/edit/save")
	public ResponseEntity<?> saveEdit(@RequestBody SubstituteWithPlaceDTO dto) {
		Substitute substitute = substituteService.getById(dto.getId());

		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + dto.getId(), HttpStatus.BAD_REQUEST);
		}

		if (dto.getName().trim().length() == 0 || dto.getSurname().trim().length() == 0) {
			return new ResponseEntity<>("Felterne med * skal udfyldes", HttpStatus.BAD_REQUEST);
		}

		substitute.setName(dto.getName());
		substitute.setSurname(dto.getSurname());
		substitute.setEmail(dto.getEmail());
		substitute.setAgency(dto.getAgency());
		substitute.setPhone(dto.getPhone());
		substitute.setAssignEmployeeSignature(dto.isAssignEmployeeSignature());

		if (StringUtils.hasText(dto.getAuthorizationCode()) && !Objects.equals(dto.getAuthorizationCode(), substitute.getAuthorizationCode())) {
			ADResponse responseSetAuthorizationCode = webSocketService.setAuthorizationCode(substitute.getUsername(), dto.getAuthorizationCode());
			if (!responseSetAuthorizationCode.isSuccess() && !responseSetAuthorizationCode.getStatus().equals(ADStatus.TIMEOUT)) {
				log.error("Failed to set authorization code in AD for Substitute: " + substitute.getName() + " " + substitute.getSurname());
				return new ResponseEntity<>("Vikaren kan ikke redigeres. Autorisationskoden blev ikke indstillet i Active Directory.", HttpStatus.BAD_REQUEST);
			}

			substitute.setAuthorizationCode(dto.getAuthorizationCode());
			substitute.setAuthorizationCodeChecked(true);
		}

		// handling employee signature
		if (dto.isAssignEmployeeSignature()) {
			ADResponse responseEmployeeSignature = webSocketService.addToEmployeeSignatureGroup(substitute);
			if (!responseEmployeeSignature.isSuccess() && !responseEmployeeSignature.getStatus().equals(ADStatus.TIMEOUT)) {
				log.error("Failed to add substitute to employee signature AD group. Substitute " + substitute.getName() + " " + substitute.getSurname() + " failed.");
				return new ResponseEntity<>("Vikaren kan ikke redigeres. Der skete en fejl under oprettelse af medarbejdersignatur.", HttpStatus.BAD_REQUEST);
			}
		}

		substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Opret et eller flere nye arbejdssteder på vikar", args = { "dto" })
	@ResponseBody
	@PostMapping("/rest/substituteadmin/substitutes/edit/addworkplace")
	public ResponseEntity<?> addWorkplaces(@RequestBody SubstituteWithPlaceDTO dto) {
		Substitute substitute = substituteService.getById(dto.getId());
		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + dto.getId(), HttpStatus.BAD_REQUEST);
		}

		List<OrgUnit> orgUnits = getOrgUnitsFromUuids(dto.getOrgUnitUuids());
		if (orgUnits.isEmpty()) {
			return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
		}

		if (dto.getTitle() == null || dto.getStart() == null || dto.getStop() == null) {
			return new ResponseEntity<>("Felterne med * skal udfyldes", HttpStatus.BAD_REQUEST);
		}
		
		if (dto.getStart().isAfter(dto.getStop())) {
			return new ResponseEntity<>("Startdatoen kan ikke være efter stopdatoen", HttpStatus.BAD_REQUEST);
		}

		int maxDays = getMaxDays(orgUnits);
		LocalDate maxStop = dto.getStart().plusDays(maxDays);
		if (dto.getStop().isAfter(maxStop)) {
			return new ResponseEntity<>("Den maksimale stopdato er overskredet", HttpStatus.BAD_REQUEST);
		}
		
		if (dto.getTitle().equals("0") || dto.getTitle() == null) {
			return new ResponseEntity<>("Der skal vælges en arbejdstitel", HttpStatus.BAD_REQUEST);
		}

		// handle roles
		List<GlobalRole> globalRoles = new ArrayList<>();
		List<LocalRole> localRoles = new ArrayList<>();
		for (RoleDTO role : dto.getRoles()) {
			GlobalRole globalRole = null;
			LocalRole localRole = null;
			String type = "";

			if (role.isGlobal()) {
				globalRole = globalRoleService.getById(role.getId());
				type = "global";
			} else {
				localRole = localRoleService.getById(role.getId());
				type = "lokal";
			}

			if (globalRole == null && localRole == null) {
				return new ResponseEntity<>("Kunne ikke finde " + type + " rolle med id " + role.getId(), HttpStatus.BAD_REQUEST);
			}
			else if (globalRole != null) {
				globalRoles.add(globalRole);
			}
			else if (localRole != null) {
				localRoles.add(localRole);
			}
		}

		// create workplaces
		List<GlobalTitle> globalTitles = globalTitleService.getAll();
		boolean requireO365Licence = false;
		List<Workplace> newWorkplaces = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnits) {
			Workplace workplace = new Workplace();
			workplace.setStartDate(dto.getStart());
			workplace.setStopDate(dto.getStop());
			workplace.setOrgUnit(orgUnit);
			workplace.setMasterId(UUID.randomUUID().toString());
			workplace.setTitle(dto.getTitle());
			workplace.setRequireO365License(dto.isRequireO365License());
			workplace.setAssignedRoles(new ArrayList<>());

			List<WorkplaceAssignedRole> assignedRoles = new ArrayList<>();
			for (GlobalRole global : globalRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, global.getUserRole());
			}
			
			for (LocalRole local : localRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, local.getUserRole());
			}

			for (OrgUnitUserRoleMapping orgUnitAutomaticUserRole : orgUnit.getAutomaticUserRoles()) {
				createWorkplaceAssignedRole(workplace, assignedRoles, orgUnitAutomaticUserRole.getUserRole());
			}

			GlobalTitle globalTitle = globalTitles.stream().filter(g -> g.getTitle().equals(dto.getTitle())).findAny().orElse(null);
			if (globalTitle != null) {
				for (GlobalTitleUserRoleMapping titleAutomaticUserRole : globalTitle.getAutomaticUserRoles()) {
					createWorkplaceAssignedRole(workplace, assignedRoles, titleAutomaticUserRole.getUserRole());
				}
			}
			
			workplace.setAssignedRoles(assignedRoles);
			workplace.setSubstitute(substitute);
			substitute.getWorkplaces().add(workplace);
			
			if (workplace.isRequireO365License()) {
				requireO365Licence = true;
			}
			
			newWorkplaces.add(workplace);
		}

		ADResponse response = webSocketService.setExpire(substitute, config.getWebsockets().isCheckStatusWhenSetExpire());
		if (!response.isSuccess()) {
			if (response.getStatus().equals(ADStatus.TIMEOUT)) {
				log.warn("Timeout when setting expire on vikar with ID " + substitute.getId() + " assuming it went okay");
				substitute.setDisabledInAd(false);
			}
			else if (response.getStatus().equals(ADStatus.FAILURE) && config.getWebsockets().isCheckStatusWhenSetExpire()) {
				log.error("Failed to set expire on vikar with ID " + substitute.getId() + ". Account is closed, response = " + response.getStatus());
				return new ResponseEntity<>(response.getMessage(), HttpStatus.BAD_REQUEST);
			}
			else {
				log.error("Failed to set expire on vikar with ID " + substitute.getId() + ", response = " + response.getStatus());
			}
		}
		else {
			substitute.setDisabledInAd(false);
		}

		// the license might already be there, but we do not know for sure, so we force-update it
		if (requireO365Licence) {

			// add license
			ADResponse licenseResponse = webSocketService.updateLicense(substitute, true);
			if (licenseResponse.isSuccess()) {
				log.info("Added office license to " + substitute.getUsername());
				substitute.setHasO365License(true);
			}
			else {
				log.error("Failed to add license to " + substitute.getUsername() + " (" + licenseResponse.getStatus() + ") - " + licenseResponse.getMessage());
			}
		}

		if (substitute.getLatestStopDate() == null || dto.getStop().isAfter(substitute.getLatestStopDate())) {
			substitute.setLatestStopDate(dto.getStop());
		}
		
		substitute = substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);
		
		boolean reactivateUserNow = false;

		for (Iterator<Workplace> iterator = newWorkplaces.iterator(); iterator.hasNext(); ) {
			Workplace newWorkplace = iterator.next();
			for (Workplace workplace : substitute.getWorkplaces()) {

				// semi-sucky comparison (as we cannot compare on DB ID). So we COULD miss duplicates... but then we do not really care,
				// that just means our statistic will not contain the duplicate, and not mess up the statistic, so a win actually :)
				if (Objects.equals(newWorkplace.getOrgUnit().getUuid(), workplace.getOrgUnit().getUuid()) &&
					Objects.equals(newWorkplace.getTitle(), workplace.getTitle()) &&
					Objects.equals(newWorkplace.getStartDate(), workplace.getStartDate()) &&
					Objects.equals(newWorkplace.getStopDate(), workplace.getStopDate())) {

					if (newWorkplace.getStartDate().equals(LocalDate.now()) || newWorkplace.getStartDate().isBefore(LocalDate.now())) {
						reactivateUserNow = true;
					}
					
					statisticService.save(workplace);
				}
			}
		}
		
		if (reactivateUserNow && substitute.isDisabledInAd()) {
			if (webSocketService.enableADAccount(substitute.getUsername()).isSuccess()) {
				substitute.setDisabledInAd(false);
				substituteService.save(substitute);
			}
			else {
				log.warn("Failed to enable AD account for substite that should be activated: " + substitute.getUsername());
			}
		}

		// handling ad groups
		if (config.getSyncADGroups().isEnabled()) {
			ADResponse groupStatus = substituteService.syncADGroups(substitute);
			if (response.isSuccess() && !groupStatus.isSuccess() && !groupStatus.getStatus().equals(ADStatus.TIMEOUT)) {
				log.error("Adding substitute to indirectly assigned AD groups failed. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ").");
				return new ResponseEntity<>("Arbejdsstedet er oprettet, men er stadig ved at få synkroniseret de inddirekte tildelte AD grupper.", HttpStatus.OK);
			}
			else if (!response.isSuccess() && !groupStatus.isSuccess() && !groupStatus.getStatus().equals(ADStatus.TIMEOUT)) {
				log.error("Adding substitute to indirectly assigned AD groups failed. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ").");
				return new ResponseEntity<>("Arbejdsstedet er oprettet, men AD kontoen er muligvis ikke blevet reaktiveret korrekt, og er stadig ved at få synkroniseret de inddirekte tildelte AD grupper.", HttpStatus.OK);
			}
		}

		if (!response.isSuccess()) {
			return new ResponseEntity<>("Arbejdsstedet er oprettet, men AD kontoen er muligvis ikke blevet reaktiveret korrekt.", HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Opsig arbejdssted", args = { "substituteId", "id" })
	@PostMapping("/rest/substituteadmin/substitutes/{substituteId}/edit/workplaces/{id}/cancel")
	public ResponseEntity<?> cancelWorkplace(@PathVariable long substituteId, @PathVariable long id) {
		Substitute substitute = substituteService.getById(substituteId);

		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}

		Workplace workplace = substitute.getWorkplaces().stream().filter(w -> w.getId() == id).findAny().orElse(null);

		if (workplace == null) {
			return new ResponseEntity<>("Kunne ikke finde arbejdssted med id " + id, HttpStatus.BAD_REQUEST);
		}

		LocalDate yesterday = LocalDate.now().minusDays(1);
		workplace.setStopDate(yesterday);
		
		ADResponse response = webSocketService.setExpire(substitute, false);
		if (!response.isSuccess()) {
			log.error("Failed to set expire on vikar with ID " + substitute.getId());
		}
		else {
			substitute.setDisabledInAd(false);
		}
		
		substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);
		
		// update existing statistic with the new stop-date
		statisticService.save(workplace);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Rediger et arbejdssted stopdato eller om det skal have Office licens", args = { "substituteId", "id", "stop", "license" })
	@PostMapping("/rest/substituteadmin/substitutes/{substituteId}/edit/workplaces/{id}/edit")
	public ResponseEntity<?> addWorkplace(@PathVariable long substituteId, @PathVariable Long id, @RequestParam("stop") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stop, @RequestParam("license") boolean license) {
		Substitute substitute = substituteService.getById(substituteId);

		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}

		Workplace workplace = substitute.getWorkplaces().stream().filter(w -> w.getId() == id).findAny().orElse(null);

		if (workplace == null) {
			return new ResponseEntity<>("Kunne ikke finde arbejdssted med id " + id, HttpStatus.BAD_REQUEST);
		}

		if (stop == null) {
			return new ResponseEntity<>("Der skal være en stop dato", HttpStatus.BAD_REQUEST);
		}

		int maxDays = workplace.getOrgUnit().getMaxSubstituteWorkingDays();
		LocalDate maxStop = workplace.getStartDate().plusDays(maxDays);
		if (stop.isAfter(maxStop)) {
			return new ResponseEntity<>("Den maksimale stopdato er overskredet", HttpStatus.BAD_REQUEST);
		}

		ADResponse response = null;
		if (!stop.equals(workplace.getStopDate())) {
			workplace.setStopDate(stop);
			response = webSocketService.setExpire(substitute, false);

			if (response.getStatus().equals(ADStatus.OK) || response.getStatus().equals(ADStatus.NOOP)) {
				substitute.setDisabledInAd(false);
			}
		}
		
		workplace.setRequireO365License(license);
		substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);

		// update existing statistic with the new stop-date
		statisticService.save(workplace);
		
		return new ResponseEntity<>(response == null || response.isSuccess(), HttpStatus.OK);
	}

	// TODO: denne kode-stump skal erstattes af noget mere generelt
	@GetMapping("/rest/substituteadmin/substitutes/edit/resetPassword/{id}")
	public ResponseEntity<?> resetPassword(@PathVariable(name = "id") long substituteId) {
		if (!config.isPasswordChangeAllowed()) {
			return new ResponseEntity<>("Det er ikke tilladt at skifte kodeord.", HttpStatus.BAD_REQUEST);
		}

		Substitute substitute = substituteService.getById(substituteId);
		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}
		
		if (substitute.isUsernameFromSofd()) {
			return new ResponseEntity<>("Kan ikke skifte kodeord på vikar - kontoen er styret udenfor vikarportalen", HttpStatus.BAD_REQUEST);
		}

		String newPassword = passwordService.generatePassword();

		try {
			ADStatus adPasswordStatus = passwordChangeQueueService.attemptPasswordChange(substitute.getUsername(), newPassword);
			if (ADResponse.isCritical(adPasswordStatus)) {
				return new ResponseEntity<>("Der opstod en teknisk fejl ved nulstilling af adgangskode.", HttpStatus.BAD_REQUEST);
			}
			
			substitute.setLastPwdChange(LocalDateTime.now());
			substituteService.save(substitute);
		}
		catch (Exception e) {
			return new ResponseEntity<>("Der opstod en teknisk fejl ved nulstilling af adgangskode.", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(newPassword, HttpStatus.OK);
	}

	@PostMapping("/rest/substituteadmin/substitutes/list/{type}")
	public DataTablesOutput<SubstituteDTO> adminEventLogsDataTable(@PathVariable String type, @Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<SubstituteDTO> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		return convertToDTO(substituteDatatablesDao.findAll(input), type);
	}

	@AuditLogIntercepted(operation = "Slet vikar fra OS2vikar", args = { "substituteId"})
	@PostMapping("/rest/substituteadmin/substitutes/{substituteId}/delete")
	public ResponseEntity<?> deleteSubstitute(@PathVariable long substituteId) {
		Substitute substitute = substituteService.getById(substituteId);
		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}

		// we don't care what the response is. It might fail if the AD account is already deleted
		webSocketService.deleteADAccount(substitute);

		logInterceptor.substituteHolder.set(substitute);
		substituteService.delete(substitute);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Lås AD konto op", args = { "substituteId"})
	@PostMapping("/rest/substituteadmin/substitutes/{substituteId}/unlock")
	public ResponseEntity<?> unlockAccount(@PathVariable long substituteId) {
		Substitute substitute = substituteService.getById(substituteId);

		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}

		ADResponse response = webSocketService.unlockADAccount(substitute);
		if (!response.isSuccess() && !response.getStatus().equals(ADStatus.TIMEOUT)) {
			return new ResponseEntity<>("Første forsøg på at låse vikaren op fejlede. Prøver igen om lidt.", HttpStatus.BAD_REQUEST);
		}

		logInterceptor.substituteHolder.set(substitute);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@AuditLogIntercepted(operation = "Opfrisk konto i KMD Nexus", args = { "substituteId"})
	@PostMapping("/rest/substituteadmin/substitutes/{substituteId}/resetNexus")
	public ResponseEntity<?> resetNexusAccount(@PathVariable long substituteId) {
		Substitute substitute = substituteService.getById(substituteId);

		if (substitute == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + substituteId, HttpStatus.BAD_REQUEST);
		}

		boolean success = nexusSyncService.resetNexus(substitute.getUsername());

		logInterceptor.substituteHolder.set(substitute);

		return new ResponseEntity<>((success) ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
	}

	@AuditLogIntercepted(operation = "Kopier vikar med lukket konto og opret nyt arbejdssted", args = { "dto" })
	@ResponseBody
	@PostMapping("/rest/substituteadmin/substitutes/copy")
	public ResponseEntity<?> copySubstitute(@RequestBody SubstituteWithPlaceDTO dto, HttpServletResponse httpResponse) {
		Substitute toCopy = substituteService.getById(dto.getId());
		if (toCopy == null) {
			return new ResponseEntity<>("Kunne ikke finde vikar med id " + dto.getId(), HttpStatus.BAD_REQUEST);
		}

		// the checks bellow should never fail as it has been validated before - check in case someone has edited the javascript
		List<OrgUnit> orgUnits = getOrgUnitsFromUuids(dto.getOrgUnitUuids());
		if (orgUnits.isEmpty()) {
			return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
		}

		if (dto.getTitle() == null || dto.getStart() == null || dto.getStop() == null) {
			return new ResponseEntity<>("Felterne med * skal udfyldes", HttpStatus.BAD_REQUEST);
		}

		if (dto.getStart().isAfter(dto.getStop())) {
			return new ResponseEntity<>("Startdatoen kan ikke være efter stopdatoen", HttpStatus.BAD_REQUEST);
		}

		int maxDays = getMaxDays(orgUnits);
		LocalDate maxStop = dto.getStart().plusDays(maxDays);
		if (dto.getStop().isAfter(maxStop)) {
			return new ResponseEntity<>("Den maksimale stopdato er overskredet", HttpStatus.BAD_REQUEST);
		}

		if (dto.getTitle().equals("0") || dto.getTitle() == null) {
			return new ResponseEntity<>("Der skal vælges en arbejdstitel", HttpStatus.BAD_REQUEST);
		}

		// create substitute as copy of the old substitute
		Substitute substitute = new Substitute();
		substitute.setCpr(toCopy.getCpr());
		substitute.setName(toCopy.getName());
		substitute.setSurname(toCopy.getSurname());
		substitute.setEmail(toCopy.getEmail());
		substitute.setAgency(toCopy.getAgency());
		substitute.setPhone(toCopy.getPhone());
		substitute.setAssignEmployeeSignature(toCopy.isAssignEmployeeSignature());

		// handle roles
		List<GlobalRole> globalRoles = new ArrayList<>();
		List<LocalRole> localRoles = new ArrayList<>();
		for (RoleDTO role : dto.getRoles()) {
			GlobalRole globalRole = null;
			LocalRole localRole = null;
			String type = "";

			if (role.isGlobal()) {
				globalRole = globalRoleService.getById(role.getId());
				type = "global";
			} else {
				localRole = localRoleService.getById(role.getId());
				type = "lokal";
			}

			if (globalRole == null && localRole == null) {
				return new ResponseEntity<>("Kunne ikke finde " + type + " rolle med id " + role.getId(), HttpStatus.BAD_REQUEST);
			} else if (globalRole != null) {
				globalRoles.add(globalRole);
			} else if (localRole != null) {
				localRoles.add(localRole);
			}
		}

		// handle creation of workplaces
		List<GlobalTitle> globalTitles = globalTitleService.getAll();
		List<Workplace> workplaces = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnits) {
			Workplace workplace = new Workplace();
			workplace.setStartDate(dto.getStart());
			workplace.setStopDate(dto.getStop());
			workplace.setOrgUnit(orgUnit);
			workplace.setMasterId(UUID.randomUUID().toString());
			workplace.setTitle(dto.getTitle());
			workplace.setRequireO365License(dto.isRequireO365License());

			List<WorkplaceAssignedRole> assignedRoles = new ArrayList<>();
			for (GlobalRole global : globalRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, global.getUserRole());
			}

			for (LocalRole local : localRoles) {
				createWorkplaceAssignedRole(workplace, assignedRoles, local.getUserRole());
			}

			for (OrgUnitUserRoleMapping orgUnitAutomaticUserRole : orgUnit.getAutomaticUserRoles()) {
				createWorkplaceAssignedRole(workplace, assignedRoles, orgUnitAutomaticUserRole.getUserRole());
			}

			GlobalTitle globalTitle = globalTitles.stream().filter(g -> g.getTitle().equals(dto.getTitle())).findAny().orElse(null);
			if (globalTitle != null) {
				for (GlobalTitleUserRoleMapping titleAutomaticUserRole : globalTitle.getAutomaticUserRoles()) {
					createWorkplaceAssignedRole(workplace, assignedRoles, titleAutomaticUserRole.getUserRole());
				}
			}

			workplace.setAssignedRoles(assignedRoles);
			workplace.setSubstitute(substitute);
			workplaces.add(workplace);
		}

		substitute.setWorkplaces(workplaces);

		if (substitute.getLatestStopDate() == null || dto.getStop().isAfter(substitute.getLatestStopDate())) {
			substitute.setLatestStopDate(dto.getStop());
		}

		// handling AD account
		boolean responseTimeout = false;
		String failAssignmentAccount = "";
		if (dto.isUseSofdADUser() && StringUtils.hasLength(dto.getSofdADUserId())) {
			substitute.setUsername(dto.getSofdADUserId());
			substitute.setUsernameFromSofd(true);
		}
		else {
			ADAccountPool account = adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, dto.isRequireO365License()).stream().findFirst().orElse(null);
			if (account == null) {
				String word = dto.isRequireO365License()? "med" : "uden";

				return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere " + word + " Office licens i puljen.", HttpStatus.BAD_REQUEST);
			}

			ADResponse response = webSocketService.associateAccount(substitute, account.getUsername());
			if (response.isSuccess() || response.getStatus().equals(ADStatus.TIMEOUT)) {
				substitute.setUsername(account.getUsername());
				substitute.setHasO365License(dto.isRequireO365License());
				adAccountPoolService.delete(account);
			}
			else {
				account.setStatus(ADAccountPoolStatus.BROKEN);
				account.setStatusMessage(response.getMessage());
				adAccountPoolService.save(account);

				account = adAccountPoolService.getByStatusAndWithO365License(ADAccountPoolStatus.OK, dto.isRequireO365License()).stream().findFirst().orElse(null);
				if (account == null) {
					String word = dto.isRequireO365License()? "med" : "uden";
					return new ResponseEntity<>("Vikaren kan ikke oprettes. Der er ikke flere brugere " + word + " Office licens i puljen.", HttpStatus.BAD_REQUEST);
				}

				response = webSocketService.associateAccount(substitute, account.getUsername());
				if (response.isSuccess() || response.getStatus().equals(ADStatus.TIMEOUT)) {
					substitute.setUsername(account.getUsername());
					substitute.setHasO365License(dto.isRequireO365License());
					adAccountPoolService.delete(account);
				}
				else {
					account.setStatus(ADAccountPoolStatus.BROKEN);
					account.setStatusMessage(response.getMessage());
					adAccountPoolService.save(account);

					log.error("Second attempt of linking AD account to substitute " + substitute.getName() + " " + substitute.getSurname() + " failed.");
					return new ResponseEntity<>("Vikaren kan ikke oprettes. Der skete en fejl under tildelingen af AD konto.", HttpStatus.BAD_REQUEST);
				}

				// handling employee signature
				if (dto.isAssignEmployeeSignature()) {
					ADResponse responseEmployeeSignature = webSocketService.addToEmployeeSignatureGroup(substitute);
					if (!responseEmployeeSignature.isSuccess() && !responseEmployeeSignature.getStatus().equals(ADStatus.TIMEOUT)) {
						log.error("Failed to add substitute to employee signature AD group. Substitute " + substitute.getName() + " " + substitute.getSurname() + " failed.");
						return new ResponseEntity<>("Vikaren kan ikke oprettes. Der skete en fejl under oprettelse af medarbejdersignatur.", HttpStatus.BAD_REQUEST);
					}
				}

				if (response.getStatus().equals(ADStatus.TIMEOUT)) {
					responseTimeout = true;
					failAssignmentAccount = account.getUsername();
				}
			}
		}

		substitute = substituteService.save(substitute);
		logInterceptor.substituteHolder.set(substitute);
		
		for (Workplace workplace : substitute.getWorkplaces()) {
			statisticService.save(workplace);
		}

		// handling ad groups
		if (config.getSyncADGroups().isEnabled()) {
			ADResponse groupStatus = substituteService.syncADGroups(substitute);
			if (!groupStatus.isSuccess() && !groupStatus.getStatus().equals(ADStatus.TIMEOUT)) {
				if (responseTimeout) {
					log.error("A timeout happend when associating AD account with userId " + failAssignmentAccount + " to substitute with id " + substitute.getId());
					log.error("Adding substitute to indirectly assigned AD groups. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ") failed.");

					return new ResponseEntity<>("Vikaren er oprettet,men er stadig igang med at få tildelt en AD konto. Vikaren er også stadig igang med at blive medlem af de inddirekte tildelte Active Directory grupper. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
				}
				else {
					log.warn("Adding substitute to indirectly assigned AD groups. Substitute " + substitute.getName() + " " + substitute.getSurname() + "(" + substitute.getUsername() + ") failed.");
					return new ResponseEntity<>("Vikaren er oprettet, men er stadig ved at blive medlem af de inddirekte tildelte Active Directory grupper. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
				}
			}
		}

		if (responseTimeout) {
			log.error("A timeout happend when associating AD account with userId " + failAssignmentAccount + " to substitute with id " + substitute.getId());

			return new ResponseEntity<>("Vikaren er oprettet, men er stadig igang med at få tildelt en AD konto. Du bliver dirigeret videre til listen af vikarer om lidt.", HttpStatus.OK);
		}


		// delete old substitute
		webSocketService.deleteADAccount(toCopy);
		substituteService.delete(toCopy);

		logInterceptor.substituteHolder.set(substitute);

		return new ResponseEntity<>(substitute.getId(), HttpStatus.OK);
	}

	private DataTablesOutput<SubstituteDTO> convertToDTO(DataTablesOutput<Substitute> output, String type) {
		List<SubstituteDTO> data = output.getData().stream().map(s -> new SubstituteDTO(s, type.equals("mobile"), config.getSubstituteWorkplaceVisibilityTreshold())).toList();

		DataTablesOutput<SubstituteDTO> result = new DataTablesOutput<>();
		result.setData(data);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}

	private List<OrgUnit> getOrgUnitsFromUuids(String uuids) {
		List<String> uuidList = new ArrayList<>();
		if (uuids.contains(";")) {
			uuidList = Arrays.asList(uuids.split(";"));
		} else {
			uuidList.add(uuids);
		}

		return orgUnitService.getByUuids(uuidList);
	}

	private void createWorkplaceAssignedRole(Workplace workplace, List<WorkplaceAssignedRole> assignedRoles, UserRole userRole) {
		WorkplaceAssignedRole workplaceRole = new WorkplaceAssignedRole();
		workplaceRole.setWorkplace(workplace);
		workplaceRole.setSyncToBeAdded(true);
		workplaceRole.setUserRoleId(userRole.getUserRoleId());
		workplaceRole.setName(userRole.getName());
		assignedRoles.add(workplaceRole);
	}

	private int getMaxDays(List<OrgUnit> orgUnits) {
		int maxDays = Integer.MAX_VALUE;
		for (OrgUnit ou : orgUnits) {
			if (ou.getMaxSubstituteWorkingDays() < maxDays) {
				maxDays = ou.getMaxSubstituteWorkingDays();
			}
		}
		return maxDays;
	}

	@AuditLogIntercepted(operation = "Autorisationskode-opslag", args = { "name", "cpr" })
	@GetMapping("/rest/substituteadmin/substitutes/authorizationCodeLookup")
	public ResponseEntity<?> authorizationCodeLookup(@RequestParam("name") String name, @RequestParam("cpr") String cpr) {
		if (!config.isEnableAuthorizationCodes()) {
			log.warn("Tried to fetch authorization codes but the feature is disabled.");
			return new ResponseEntity<>("This feature is disabled", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		List<AuthorizationCode> codes = authorizationCodeService.getAuthorizationCodes(name, cpr);
		
		if (codes.isEmpty()) {
			return new ResponseEntity<>("Fandt ingen autorisationskoder", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(codes, HttpStatus.OK);
	}

}
