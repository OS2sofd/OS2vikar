package dk.digitalidentity.os2vikar.controller.mvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.config.RoleConstants;
import dk.digitalidentity.os2vikar.config.modules.ConstraintITSystem;
import dk.digitalidentity.os2vikar.controller.mvc.dto.RoleDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.SubstituteWithPlaceDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.WorkplaceDTO;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.dao.model.Statistic;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.security.RequireSubstituteAdminAccess;
import dk.digitalidentity.os2vikar.security.SecurityUtil;
import dk.digitalidentity.os2vikar.service.ADAccountPoolService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import dk.digitalidentity.os2vikar.service.StatisticService;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import dk.digitalidentity.os2vikar.service.xls.StatisticXlsView;

@Controller
@RequireSubstituteAdminAccess
public class SubstituteAdminController {
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired 
	private SubstituteService substituteService;
	
	@Autowired
	private ADAccountPoolService adAccountPoolService;

	@Autowired
	private OS2VikarConfiguration config;
	
	@Autowired
	private StatisticService statisticService;

	@GetMapping("/substituteadmin/substitutes")
	public String substituteadminList(Model model) {
		return "substitute_admin/substitutes";
	}
	
	@GetMapping("/substituteadmin/substitutes/fragments/desktop")
	public String desktop(Model model) {
		return "substitute_admin/fragments/substitutes_desktop_table :: substitutesDesktopTable";
	}
	
	@GetMapping("/substituteadmin/substitutes/fragments/mobile")
	public String mobile(Model model) {
		return "substitute_admin/fragments/substitutes_mobile_table :: substitutesMobileTable";
	}

	record WorkspaceOUListDTO (String uuid, String name, String systems) {}

	@GetMapping("/substituteadmin/substitutes/new")
	public String substituteadminNew(Model model) {
		List<ConstraintITSystem> configConstraintITSystems = config.getConstraintITSystems();
		List<OrgUnit> allowedOrgUnits = orgUnitService.getAllOrgUnitsUserIsAllowedToCreateSubstitutesOn();
		List<WorkspaceOUListDTO> finalAllowedOrgUnits = allowedOrgUnits
				.stream()
				.map(o -> new WorkspaceOUListDTO(o.getUuid(), o.getName(), findITSystemNames(o, configConstraintITSystems)))
				.collect(Collectors.toList());
		
		// empty substitute form
		SubstituteWithPlaceDTO substituteForm = new SubstituteWithPlaceDTO();
		substituteForm.setCpr("");
		substituteForm.setName("");
		substituteForm.setSurname("");
		substituteForm.setId((long)0);
		
		model.addAttribute("substituteForm", substituteForm);
		model.addAttribute("allowedOrgUnits", finalAllowedOrgUnits);
		
		return "substitute_admin/substitutes_new";
	}

	@GetMapping("/substituteadmin/substitutes/cpr/{cpr}")
	public String substituteByCpr(Model model, @PathVariable String cpr) {
		Substitute substitute = substituteService.getByCpr(cpr);
		if (substitute == null) {
			return "redirect:/error";
		}

		return "redirect:/substituteadmin/substitutes/" + substitute.getId() + "/edit";
	}

	@GetMapping("/substituteadmin/substitutes/{id}/edit")
	public String substituteadminEdit(Model model, @PathVariable Long id) {
		Substitute substitute = substituteService.getById(id);
		if (substitute == null) {
			return "redirect:/error";
		}

		SubstituteWithPlaceDTO substituteForm = new SubstituteWithPlaceDTO();
		substituteForm.setCpr((!SecurityUtil.hasRole(RoleConstants.SYSTEM_ADMIN)) ? SubstituteService.maskCpr(substitute.getCpr()) : substitute.getCpr());
		substituteForm.setName(substitute.getName());
		substituteForm.setSurname(substitute.getSurname());
		substituteForm.setId(substitute.getId());
		substituteForm.setEmail(substitute.getEmail());
		substituteForm.setPhone(substitute.getPhone());
		substituteForm.setAgency(substitute.getAgency());
		substituteForm.setLastPwdChange((substitute.getLastPwdChange() != null) ? substitute.getLastPwdChange().toLocalDate() : null);
		substituteForm.setUsername(substitute.getUsername() == null ? "" : substitute.getUsername());
		substituteForm.setAssignEmployeeSignature(substitute.isAssignEmployeeSignature());
		substituteForm.setAuthorizationCode(substitute.getAuthorizationCode());

		List<ConstraintITSystem> configConstraintITSystems = config.getConstraintITSystems();
		List<OrgUnit> allowedOrgUnits = orgUnitService.getAllOrgUnitsUserIsAllowedToCreateSubstitutesOn();
		List<WorkspaceOUListDTO> finalAllowedOrgUnits = allowedOrgUnits
				.stream()
				.map(o -> new WorkspaceOUListDTO(o.getUuid(), o.getName(), findITSystemNames(o, configConstraintITSystems)))
				.collect(Collectors.toList());
		
		model.addAttribute("substituteForm", substituteForm);
		model.addAttribute("allowedOrgUnits", finalAllowedOrgUnits);
		
		return "substitute_admin/substitutes_edit";
	}
	
	@GetMapping("/substituteadmin/substitutes/{id}/fragments/workplaces")
	public String workplacesTableFragment(Model model, @PathVariable Long id) {
		Substitute substitute = substituteService.getById(id);
		if (substitute == null) {
			return "redirect:/error";
		}
		
		List<WorkplaceDTO> workplaceDTOs = new ArrayList<>();
		for (Workplace workplace : substitute.getWorkplaces()) {
			WorkplaceDTO workplaceDTO = new WorkplaceDTO();
			workplaceDTO.setOrgUnit(workplace.getOrgUnit());
			workplaceDTO.setStart(workplace.getStartDate());
			workplaceDTO.setStop(workplace.getStopDate());
			workplaceDTO.setTitle(workplace.getTitle());
			workplaceDTO.setId(workplace.getId());
			workplaceDTO.setCanBeExtended(!workplace.getStopDate().isBefore(LocalDate.now()));
			
			List<RoleDTO> roles = workplace.getAssignedRoles().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList());
			workplaceDTO.setRoles(roles);
			
			workplaceDTOs.add(workplaceDTO);
		}
		
		model.addAttribute("workplaces", workplaceDTOs);
		
		return "substitute_admin/fragments/workplace_table :: workplaceTable";
	}
	
	@GetMapping("/substituteadmin/substitutes/{sid}/fragments/workplaces/{id}/edit")
	public String workplacesEditFragment(Model model, @PathVariable(name = "sid") long substituteId, @PathVariable Long id) {
		Substitute substitute = substituteService.getById(substituteId);
		if (substitute == null) {
			return "redirect:/error";
		}
		
		Workplace workplace = substitute.getWorkplaces().stream().filter(w -> w.getId() == id).findAny().orElse(null);
		
		if (workplace == null) {
			return "redirect:/error";
		}  

		WorkplaceDTO dto = new WorkplaceDTO();
		dto.setOrgUnit(workplace.getOrgUnit());
		dto.setStart(workplace.getStartDate());
		dto.setStop(workplace.getStopDate());
		dto.setTitle(workplace.getTitle());
		dto.setId(workplace.getId());
		dto.setCanBeExtended(workplace.getStopDate().isAfter(LocalDate.now()));
		dto.setRequireO365License(workplace.isRequireO365License());
		
		List<RoleDTO> roles = workplace.getAssignedRoles().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList());
		dto.setRoles(roles);
		
		model.addAttribute("workplace", dto);
		
		return "substitute_admin/fragments/substitutes_edit_workplace :: substituteEditWorkplace";
	}
	
	@GetMapping("/substituteadmin/userpool")
	public String getUserpool(Model model) {
		model.addAttribute("users", adAccountPoolService.getAll());
		
		return "substitute_admin/users";
	}
	
	@GetMapping("/substituteadmin/download/statistic")
	public ModelAndView downloadOrgUnits(HttpServletResponse response) {
		List<Statistic> statistics = statisticService.getAll();

		Map<String, Object> model = new HashMap<>();
		model.put("statistics", statistics);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"statistic.xlsx\"");

		return new ModelAndView(new StatisticXlsView(), model);
	}


	private String findITSystemNames(OrgUnit orgUnit, List<ConstraintITSystem> configConstraintITSystems) {
		List<String> names = new ArrayList<>();
		for (ConstraintITSystem constraintITSystem : configConstraintITSystems) {
			if (orgUnit.getItSystems().contains(constraintITSystem.getIdentifier())) {
				names.add(constraintITSystem.getName());
			}
		}

		return String.join(", ", names);
	}
}
