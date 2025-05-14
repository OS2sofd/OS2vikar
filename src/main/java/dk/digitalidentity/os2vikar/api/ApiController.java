package dk.digitalidentity.os2vikar.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2vikar.api.dto.SubstituteApiDTO;
import dk.digitalidentity.os2vikar.api.dto.SubstituteUpdateApiDTO;
import dk.digitalidentity.os2vikar.api.dto.WorkplaceApiDTO;
import dk.digitalidentity.os2vikar.dao.model.ADGroup;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.security.RequireApiAccess;
import dk.digitalidentity.os2vikar.service.ADGroupService;
import dk.digitalidentity.os2vikar.service.GlobalTitleService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import dk.digitalidentity.os2vikar.service.SubstituteService;

@RequireApiAccess
@RestController
public class ApiController {

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private GlobalTitleService globalTitleService;
	
	@Autowired 
	private ADGroupService adGroupService;
		
	// used by sofd-vikar integration
	@GetMapping("/api/substitute")
	public ResponseEntity<?> getSubstitute(@RequestParam(name = "offset", required = false, defaultValue = "") String offset) {
		List<SubstituteApiDTO> result = new ArrayList<>();

		List<Substitute> substitutes = null;
		if (StringUtils.hasLength(offset)) {
			substitutes = substituteService.getByLastUpdatedAfter(LocalDateTime.parse(offset));
		}
		else {
			substitutes = substituteService.getAll();
		}

		for (Substitute substitute : substitutes) {
			SubstituteApiDTO substituteDTO = new SubstituteApiDTO();
			substituteDTO.setFirstname(substitute.getName());
			substituteDTO.setSurname(substitute.getSurname());
			substituteDTO.setCpr(substitute.getCpr());
			substituteDTO.setUuid(substitute.getUuid());
			substituteDTO.setUsername(substitute.getUsername());
			substituteDTO.setLastUpdated(substitute.getLastUpdated().toString());
			List<WorkplaceApiDTO> workplaces = new ArrayList<>();

			for (Workplace workplace : substitute.getWorkplaces()) {
				WorkplaceApiDTO workplaceDTO = new WorkplaceApiDTO();
				workplaceDTO.setMasterId(workplace.getMasterId());
				workplaceDTO.setOrgUnit(workplace.getOrgUnit().getUuid());
				workplaceDTO.setStartDate(workplace.getStartDate());
				workplaceDTO.setStopDate(workplace.getStopDate());
				workplaceDTO.setTitle(workplace.getTitle());
				workplaces.add(workplaceDTO);
			}

			substituteDTO.setWorkplaces(workplaces);

			result.add(substituteDTO);
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	// callback from SOFD Core with the UUID after creation in SOFD
	@PostMapping(value = "/api/substitute/{cpr}")
	public ResponseEntity<?> updateSubstitute(@RequestBody SubstituteUpdateApiDTO substituteUpdateDto, @PathVariable("cpr") String cpr) {
		Substitute substitute = substituteService.getByCpr(cpr);

		if (substitute == null) {
			return new ResponseEntity<>("Der findes ikke en vikar med dette personnummer.", HttpStatus.NOT_FOUND);
		}

		if (!StringUtils.hasLength(substitute.getUuid())) {
			substitute.setUuid(substituteUpdateDto.getUuid());
			substituteService.save(substitute);
		}

		return new ResponseEntity<>("Vikaren er opdateret", HttpStatus.OK);
	}
	
	private record TitlesOut (String orgUnitUuid, Set<String> titles) { }
	
	// used by sofd-vikar to sync titles back into "managed titles" on OUs in SOFD
	@GetMapping("/api/titles")
	public ResponseEntity<?> getAllTitles() {
		List<String> globalTitles = globalTitleService.getAll().stream().map(t -> t.getTitle()).collect(Collectors.toList());
		List<OrgUnit> orgUnits = orgUnitService.getWithSubstitutesAllowed();
		List<TitlesOut> result = new ArrayList<>();
		
		for (int i = 0; i < orgUnits.size(); i++) {
			OrgUnit orgUnit = orgUnits.get(i);
			
			List<String> localTitles = orgUnit.getLocalTitles().stream().map(t -> t.getTitle()).collect(Collectors.toList());
			
			Set<String> titles = new HashSet<>(globalTitles);
			titles.addAll(localTitles);
			
			TitlesOut titlesOut = new TitlesOut(orgUnit.getUuid(), titles);
			result.add(titlesOut);
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	// used by the local agent to sync AD groups
	record ADGroupDTO (String name, String objectGuid) {}
	@PostMapping(value = "/api/adgroups")
	public ResponseEntity<HttpStatus> updateSubstitute(@RequestBody List<ADGroupDTO> groups) {
		List<ADGroup> allFromDB = adGroupService.getAll();
		
		for (ADGroupDTO dto : groups) {
			ADGroup match = allFromDB.stream().filter(g -> g.getObjectGuid().equals(dto.objectGuid())).findFirst().orElse(null);
			
			if (match == null) {
				
				// create
				ADGroup newGroup = new ADGroup();
				newGroup.setName(dto.name());
				newGroup.setObjectGuid(dto.objectGuid());
				adGroupService.save(newGroup);
			} else {
				
				// update
				if (!Objects.equals(match.getName(), dto.name())) {
					match.setName(dto.name());
					adGroupService.save(match);
				}
			}
		}
		
		// delete
		List<String> objectGuidsToKeep = groups.stream().map(g -> g.objectGuid()).collect(Collectors.toList());
		List<ADGroup> toDelete = allFromDB.stream().filter(g -> !objectGuidsToKeep.contains(g.getObjectGuid())).collect(Collectors.toList());
		adGroupService.deleteAll(toDelete);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
