package dk.digitalidentity.os2vikar.controller.mvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.config.modules.ConstraintITSystem;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.os2vikar.controller.mvc.dto.OUTreeForm;
import dk.digitalidentity.os2vikar.controller.mvc.dto.RoleDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.TitleDTO;
import dk.digitalidentity.os2vikar.controller.mvc.dto.UserRoleDTO;
import dk.digitalidentity.os2vikar.dao.model.AuditLog;
import dk.digitalidentity.os2vikar.dao.model.CmsMessage;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.dao.model.UserRole;
import dk.digitalidentity.os2vikar.security.RequireSystemAdminAccess;
import dk.digitalidentity.os2vikar.service.ADGroupService;
import dk.digitalidentity.os2vikar.service.AuditLogService;
import dk.digitalidentity.os2vikar.service.CmsMessageBundle;
import dk.digitalidentity.os2vikar.service.CmsMessageService;
import dk.digitalidentity.os2vikar.service.GlobalRoleService;
import dk.digitalidentity.os2vikar.service.GlobalTitleService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import dk.digitalidentity.os2vikar.service.SettingsService;
import dk.digitalidentity.os2vikar.service.UserRoleService;

@Controller
@RequireSystemAdminAccess
public class SystemAdminController {
	
	@Autowired
	private GlobalTitleService globalTitleService;
	
	@Autowired 
	private GlobalRoleService globalRoleService;
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private UserRoleService userRoleService;

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private CmsMessageBundle cmsMessageBundle;

	@Autowired
	private CmsMessageService cmsMessageService;
	
	@Autowired
	private ADGroupService adGroupService;

	@Autowired
	private OS2VikarConfiguration configuration;


	@GetMapping("/systemadmin/orgunits")
	public String systemadmin(Model model) {
		List<OUTreeForm> orgUnits = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnitService.getAll()) {
			List<String> systemBadgeNames = new ArrayList<>();
			for (ConstraintITSystem constraintITSystem : configuration.getConstraintITSystems()) {
				if (orgUnit.getItSystems().contains(constraintITSystem.getIdentifier())) {
					systemBadgeNames.add(constraintITSystem.getName());
				}
			}
			orgUnits.add(new OUTreeForm(orgUnit, true, systemBadgeNames));
		}
		model.addAttribute("orgUnits", orgUnits);
		return "system_admin/org_units";
	}

	record ITSystemOrgUnitDTO (String name, String identifier, boolean checked) {}
	record ADGroupOrgUnitDTO (String name, String guid, boolean checked) {}
	@GetMapping(path = {"/systemadmin/orgunits/description/{uuid}"})
	public String detail(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		
		if (orgUnit == null) {
			return "redirect: /error";
		}
		
		model.addAttribute("orgUnit", orgUnit);
		
		List<TitleDTO> titles = new ArrayList<>();
		titles.addAll(orgUnit.getLocalTitles().stream().map(t -> new TitleDTO(t)).collect(Collectors.toList()));
		titles.addAll(globalTitleService.getAll().stream().map(t -> new TitleDTO(t)).collect(Collectors.toList()));
		
		model.addAttribute("titles", titles);
		
		List<RoleDTO> roles = new ArrayList<>();
		roles.addAll(orgUnit.getLocalRoles().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList()));
		roles.addAll(globalRoleService.getAll().stream().map(r -> new RoleDTO(r)).collect(Collectors.toList()));
		
		List<Long> checkedGlobalUserRoleIds = globalRoleService.getAll().stream().map(r -> r.getUserRole().getId()).collect(Collectors.toList());
		List<Long> checkedLocalUserRoleIds = orgUnit.getLocalRoles().stream().map(r -> r.getUserRole().getId()).collect(Collectors.toList());
		List<UserRoleDTO> dtos = new ArrayList<>();
		
		for (UserRole userRole : userRoleService.findAll()) {
			UserRoleDTO dto = new UserRoleDTO();
			dto.setId(userRole.getId());
			dto.setName(userRole.getName());
			dto.setDescription(userRole.getDescription());
			dto.setItSystem(userRole.getItSystem().getName());
			dto.setChecked(checkedGlobalUserRoleIds.contains(userRole.getId()) || checkedLocalUserRoleIds.contains(userRole.getId()));
			dto.setGlobal(checkedGlobalUserRoleIds.contains(userRole.getId()));
			dtos.add(dto);
		}
		
		model.addAttribute("roles", dtos);

		if (orgUnit.getAdGroups() == null || orgUnit.getAdGroups().isEmpty()) {
			model.addAttribute("adGroups", adGroupService.getAll().stream().map(g -> new ADGroupOrgUnitDTO(g.getName(), g.getObjectGuid(), false)).collect(Collectors.toList()));
		} else {
			List<String> assignedObjectGuids = orgUnit.getAdGroups().stream().map(g -> g.getAdGroup().getObjectGuid()).collect(Collectors.toList());
			model.addAttribute("adGroups", adGroupService.getAll().stream().map(g -> new ADGroupOrgUnitDTO(g.getName(), g.getObjectGuid(), assignedObjectGuids.contains(g.getObjectGuid()))).collect(Collectors.toList()));
		}

		List<ITSystemOrgUnitDTO> systems = new ArrayList<>();
		for (ConstraintITSystem constraintITSystem : configuration.getConstraintITSystems()) {
			systems.add(new ITSystemOrgUnitDTO(constraintITSystem.getName(), constraintITSystem.getIdentifier(), orgUnit.getItSystems().contains(constraintITSystem.getIdentifier())));
		}
		model.addAttribute("systems", systems);

		List<Long> checkedUserRoles = orgUnit.getAutomaticUserRoles().stream().map(r -> r.getUserRole().getId()).collect(Collectors.toList());
		List<UserRoleDTO> automaticUserRoles = new ArrayList<>();
		for (UserRole userRole : userRoleService.findAll()) {
			UserRoleDTO dto = new UserRoleDTO();
			dto.setId(userRole.getId());
			dto.setName(userRole.getName());
			dto.setDescription(userRole.getDescription());
			dto.setItSystem(userRole.getItSystem().getName());
			dto.setChecked(checkedUserRoles.contains(userRole.getId()));
			automaticUserRoles.add(dto);
		}

		model.addAttribute("userRoles", automaticUserRoles);

		return "system_admin/fragments/ou_description :: ouDescription";
	}

	record TitleListDTO (long id, String title, String objectGuids, String identifiers, String userRoleIds) {}
	@GetMapping("/systemadmin/titles")
	public String titles(Model model) {
		List<GlobalTitle> titles = globalTitleService.getAll();
		
		model.addAttribute("titles", titles.stream().map(t -> new TitleListDTO(t.getId(), t.getTitle(), t.getAdGroups().stream().map(g -> g.getAdGroup().getObjectGuid()).collect(Collectors.joining(",")), String.join(",", t.getItSystems()), t.getAutomaticUserRoles().stream().map(g -> String.valueOf(g.getUserRole().getId())).collect(Collectors.joining(",")))).collect(Collectors.toList()));
		model.addAttribute("adGroups", adGroupService.getAll());
		model.addAttribute("userRoles", userRoleService.findAll());
		model.addAttribute("systems", configuration.getConstraintITSystems());

		return "system_admin/titles";
	}
	
	@GetMapping("/systemadmin/roles")
	public String roles(Model model) {
		List<Long> checkedUserRoleIds = globalRoleService.getAll().stream().map(r -> r.getUserRole().getId()).collect(Collectors.toList());
		List<UserRoleDTO> dtos = new ArrayList<>();
		
		for (UserRole userRole : userRoleService.findAll()) {
			UserRoleDTO dto = new UserRoleDTO();
			dto.setId(userRole.getId());
			dto.setName(userRole.getName());
			dto.setDescription(userRole.getDescription());
			dto.setItSystem(userRole.getItSystem().getName());
			dto.setChecked(checkedUserRoleIds.contains(userRole.getId()));

			dtos.add(dto);
		}

		model.addAttribute("userRoles", dtos);
		
		return "system_admin/roles";
	}
	
	@GetMapping("/systemadmin/auditlog")
	public String log(Model model) {
		return "system_admin/log";
	}

	@GetMapping("/systemadmin/auditlog/{id}/details")
	public String logDetails(Model model, @PathVariable long id) {
		AuditLog log = auditLogService.getById(id);
		if (log == null) {
			return "redirect: /error";
		}

		model.addAttribute("log", log);

		return "system_admin/log_details";
	}

	@GetMapping("/systemadmin/settings")
	public String settings(Model model) {

		model.addAttribute("deleteAccountInAD", settingsService.getBooleanWithDefault(SettingsService.DELETE_ACCOUNT_IN_AD, true));
		model.addAttribute("deleteSubstituteAfter", settingsService.getDeleteSubstituteAfter());
		model.addAttribute("cmsMessages", cmsMessageBundle.getAll());

		return "system_admin/settings";
	}

	record CmsMessageDTO(String description, String key, String value) {}
	@GetMapping("/systemadmin/cms/edit")
	public String editCms(Model model, @RequestParam("key") String key) {
		CmsMessageDTO dto = new CmsMessageDTO(cmsMessageBundle.getDescription(key), key, cmsMessageBundle.getText(key, true));
		model.addAttribute("cmsMessage", dto);

		return "system_admin/cms-edit";
	}

	@PostMapping("/systemadmin/cms/edit")
	public String saveCms(Model model, CmsMessageDTO cmsMessageDTO) {
		if (cmsMessageDTO.value().length() > 65536) {
			CmsMessageDTO dto = new CmsMessageDTO(cmsMessageBundle.getDescription(cmsMessageDTO.key()), cmsMessageDTO.key(), cmsMessageDTO.value());
			model.addAttribute("cmsMessage", dto);
			model.addAttribute("showError", true);

			return "system_admin/cms-edit";
		}

		PolicyFactory policy = new HtmlPolicyBuilder()
				.allowCommonBlockElements()
				.allowCommonInlineFormattingElements()
				.allowElements("a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "target").onElements("a")
				.allowAttributes("class", "style", "id", "name").globally()
				.toFactory();
		String safeHTML = policy.sanitize(cmsMessageDTO.value());

		CmsMessage cms = cmsMessageService.getByCmsKey(cmsMessageDTO.key());
		if (cms == null) {
			cms = new CmsMessage();
			cms.setCmsKey(cmsMessageDTO.key());
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(cms);

		return "redirect:/systemadmin/settings";
	}
}
