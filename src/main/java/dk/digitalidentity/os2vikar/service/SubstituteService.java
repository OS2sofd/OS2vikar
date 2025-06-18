package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.SubstituteDao;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitleADGroupMapping;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.dao.model.WorkplaceAssignedRole;
import dk.digitalidentity.os2vikar.service.dto.DeleteAfterPeriod;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubstituteService {

	@Autowired
	private SubstituteDao substituteDao;
	
	@Autowired
	private RoleCatalogueService roleCatalogService;
	
	@Autowired
	private WebSocketService webSocketService;

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private GlobalTitleADGroupMappingService globalTitleADGroupMappingService;

	@Autowired
	private OS2VikarConfiguration configuration;

	public List<Substitute> getAll() {
		return substituteDao.findAll();
	}
	
	public List<Substitute> getWithUncheckedAuthorizationCodes() {
		return substituteDao.findByAuthorizationCodeCheckedFalse();
	}

	public List<Substitute> getByLastUpdatedAfter(LocalDateTime tts) {
		return substituteDao.findByLastUpdatedAfter(tts);
	}
	
	public Substitute getById(long id) {
		return substituteDao.findByid(id);
	}

	public void delete(Substitute substitute) {
		substituteDao.delete(substitute);
	}
	
	public Substitute save(Substitute substitute) {
		// @UpdateTimestamp does not work when saving workplaces, so
		substitute.setLastUpdated(LocalDateTime.now());

		return substituteDao.save(substitute);
	}
	
	public Substitute getByCpr(String cpr) {
		return substituteDao.findByCpr(cpr);
	}

	public static String getExpireTime(Substitute substitute) {
		LocalDate stopDate = LocalDate.now();

		for (Workplace workplace : substitute.getWorkplaces()) {
			if (workplace.getStopDate() != null && workplace.getStopDate().isAfter(stopDate)) {
				stopDate = workplace.getStopDate();
			}
		}
		
		return stopDate.toString();
	}
	
	public static String maskCpr(String cpr) {
		if (cpr.length() == 10) {
			cpr = cpr.substring(0, cpr.length() - 4) + "-XXXX";
		}
		
		return cpr;
	}

	public ADResponse syncADGroups(Substitute substitute) {
		List<GlobalTitleADGroupMapping> titleMappings = globalTitleADGroupMappingService.getAll();
		return syncADGroups(substitute, titleMappings);
	}

	public ADResponse syncADGroups(Substitute substitute, List<GlobalTitleADGroupMapping> titleMappings) {
		Set<String> guids = new HashSet<>();
		for (Workplace workplace : substitute.getWorkplaces()) {
			if (workplace.getStopDate().isBefore(LocalDate.now())) {

				// skip if stopped
				continue;
			}

			guids.addAll(workplace.getOrgUnit().getAdGroups().stream().map(g -> g.getAdGroup().getObjectGuid()).collect(Collectors.toList()));

			List<GlobalTitleADGroupMapping> mappingsForTitle = titleMappings.stream().filter(m -> m.getGlobalTitle().getTitle().equals(workplace.getTitle())).collect(Collectors.toList());
			guids.addAll(mappingsForTitle.stream().map(m -> m.getAdGroup().getObjectGuid()).collect(Collectors.toList()));
		}

		return webSocketService.handleGroupMemberships(substitute, new ArrayList<>(guids));
	}

	@Transactional
	public void sync() {
		log.debug("Started syncing substitute roles to the role catalog");

		int counterDeleted = 0;
		int counterAdded = 0;
		
		for (Substitute substitute : substituteDao.findAll()) {
			if (!StringUtils.hasLength(substitute.getUsername())) {
				continue;
			}
			
			Set<Long> currentActiveUserRoleIds = new HashSet<>(); 
			Set<WorkplaceAssignedRole> newActiveRoles = new HashSet<>();
			Set<WorkplaceAssignedRole> rolesToDelete = new HashSet<>();
			LocalDate now = LocalDate.now();

			for (Workplace workplace : substitute.getWorkplaces()) {
				if (workplace.getStopDate().isBefore(now)) {
					// if workplace stopped
					for (WorkplaceAssignedRole role : workplace.getAssignedRoles()) {
						if (role.isSyncRemoved()) {
							continue;
						}
						
						rolesToDelete.add(role);
					}
				}
				else if (workplace.getStartDate().isBefore(now) || workplace.getStartDate().isEqual(now)) {
					// if workplace active
					for (WorkplaceAssignedRole role : workplace.getAssignedRoles()) {
						if (role.isSyncToBeAdded()) {
							newActiveRoles.add(role);
						}
						else {
							currentActiveUserRoleIds.add(role.getUserRoleId());
						}
					}
				}
			}
			
			for (WorkplaceAssignedRole role : newActiveRoles) {
				long userRoleId = role.getUserRoleId();

				if (currentActiveUserRoleIds.contains(userRoleId)) {
					continue; 
				}
				
				boolean success = roleCatalogService.assignUserRoleToUser(userRoleId, substitute.getUsername());
				if (success) {
					role.setSyncToBeAdded(false);
					currentActiveUserRoleIds.add(userRoleId);
					counterAdded++;
					auditLogService.logSystem("Automatisk tilføjelse af rettighed", "Rettigheden " + role.getName() + " blev tilføjet, fordi arbejdsstedet " + role.getWorkplace().getOrgUnit().getName() + " starter " + role.getWorkplace().getStartDate().toString(), role.getWorkplace().getSubstitute());
				}
			}
			
			for (WorkplaceAssignedRole role : rolesToDelete) {
				long userRoleId = role.getUserRoleId();
				if (currentActiveUserRoleIds.contains(userRoleId)) {
					continue; 
				}
				
				boolean success = roleCatalogService.deassignUserRoleToUser(userRoleId, substitute.getUsername());
				if (success) {
					role.setSyncRemoved(true);
					counterDeleted++;
					auditLogService.logSystem("Automatisk fjernelse af rettighed", "Rettigheden " + role.getName() + " blev fjernet, fordi arbejdsstedet " + role.getWorkplace().getOrgUnit().getName() + " er ophørt " + role.getWorkplace().getStopDate().toString(), role.getWorkplace().getSubstitute());
				}
			}
			
			substituteDao.save(substitute);
		}
		
		if (counterAdded > 0 || counterDeleted > 0) {
			log.info("Finished syncing substitute roles to the role catalog. Added " + counterAdded + " roles and deleted " + counterDeleted + " roles.");
		}
	}

	@Transactional
	public void activate() {
		List<Substitute> substitutes = substituteDao.findByDisabledInAdTrue();
		
		for (Substitute substitute : substitutes) {
			if (substitute.isUsernameFromSofd() || !StringUtils.hasLength(substitute.getUsername())) {
				continue;
			}

			// e.g. if disableDelay = 1
			// if today is 2025-06-18, then any workplace with a stopDate of 2025-06-16 or earlier will be counted
			int stoppedWorkplaces = (int) substitute.getWorkplaces().stream()
					.filter(w -> w.getStopDate().isBefore(LocalDate.now().minusDays(configuration.getDisableDelay())))
					.count();

			// e.g. if workplaceActiveTresholdDays = 2
			// if today is 2025-06-18, then any workplace with a startDate of 2025-06-21 or later will be counted
			int notYetActiveWorkplaces = (int) substitute.getWorkplaces().stream()
					.filter(w -> w.getStartDate().isAfter(LocalDate.now().plusDays(configuration.getWorkplaceActiveTresholdDays())))
					.count();

			if (substitute.getWorkplaces().size() > (stoppedWorkplaces + notYetActiveWorkplaces)) {
				log.info("Enabling AD account for substitute: " + substitute.getUsername());
				auditLogService.logSystem("Vikar enabled i AD grundet kommende arbejssted", "Vikaren har kommende arbejdssteder", substitute);

				ADResponse result = webSocketService.enableADAccount(substitute.getUsername());
				if (result.isSuccess()) {
					substitute.setDisabledInAd(false);
				}

				substituteDao.save(substitute);
			}
		}
	}
	
	@Transactional
	public void cleanUp() {
		log.info("Started substitute clean up task");

		/// delete all accounts that have been inactive for more than the configured period
		
		List<Substitute> toDelete = new ArrayList<>();
		LocalDate inactiveSinceDate = LocalDate.now().minusYears(5);
		String dateString = "five years ago";
		String danishDateString = "fem år";

		if (settingsService.getDeleteSubstituteAfter().equals(DeleteAfterPeriod.ONE_YEAR)) {
			inactiveSinceDate = LocalDate.now().minusYears(1);
			dateString = "one year ago";
			danishDateString = "et år";
		}
		else if (settingsService.getDeleteSubstituteAfter().equals(DeleteAfterPeriod.THREE_YEARS)) {
			inactiveSinceDate = LocalDate.now().minusYears(3);
			dateString = "three years ago";
			danishDateString = "tre år";
		}
		else if (settingsService.getDeleteSubstituteAfter().equals(DeleteAfterPeriod.SIX_MONTHS)) {
			inactiveSinceDate = LocalDate.now().minusMonths(6);
			dateString = "six months ago";
			danishDateString = "seks måneder";
		}
		else if (settingsService.getDeleteSubstituteAfter().equals(DeleteAfterPeriod.THREE_MONTHS)) {
			inactiveSinceDate = LocalDate.now().minusMonths(3);
			dateString = "three months ago";
			danishDateString = "tre måneder";
		}
		else if (settingsService.getDeleteSubstituteAfter().equals(DeleteAfterPeriod.FIVE_DAYS)) {
			inactiveSinceDate = LocalDate.now().minusDays(5);
			dateString = "five days ago";
			danishDateString = "5 dage";
		}

		List<Substitute> substitutes = substituteDao.findAll();
		for (Iterator<Substitute> iterator = substitutes.iterator(); iterator.hasNext();) {
			Substitute substitute = iterator.next();

			// skip those where we do not now the last stopDate
			if (substitute.getLatestStopDate() == null) {
				continue;
			}
			
			String uuid = substitute.getUuid();

			if (substitute.getLatestStopDate().isBefore(inactiveSinceDate)) {
				log.info("Deleting substitute with uuid " + uuid + ". Latest stopDate was " + dateString + ".");
				auditLogService.logSystem("Vikar slettet grundet inaktivitet", "Vikaren har ikke haft et aktivt arbejdssted i " + danishDateString, substitute);

				if (settingsService.getBooleanWithDefault(SettingsService.DELETE_ACCOUNT_IN_AD, true)) {
					webSocketService.deleteADAccount(substitute);
					iterator.remove();
				}
				
				toDelete.add(substitute);

				continue;
			}
		}
		
		substituteDao.deleteAll(toDelete);

		// disable all accounts without any workplaces that have an active AD account
		for (Substitute substitute : substitutes) {
			if (substitute.isDisabledInAd() || substitute.isUsernameFromSofd() || !StringUtils.hasLength(substitute.getUsername())) {
				continue;
			}

			// e.g. if disableDelay = 1
			// if today is 2025-06-18, then any workplace with a stopDate of 2025-06-16 or earlier will be counted
			int stoppedWorkplaces = (int) substitute.getWorkplaces().stream()
					.filter(w -> w.getStopDate().isBefore(LocalDate.now().minusDays(configuration.getDisableDelay())))
					.count();

			// e.g. if workplaceActiveTresholdDays = 2
			// if today is 2025-06-18, then any workplace with a startDate of 2025-06-21 or later will be counted
			int notYetActiveWorkplaces = (int) substitute.getWorkplaces().stream()
					.filter(w -> w.getStartDate().isAfter(LocalDate.now().plusDays(configuration.getWorkplaceActiveTresholdDays())))
					.count();

			if (substitute.getWorkplaces().isEmpty() || stoppedWorkplaces == (substitute.getWorkplaces().size() - notYetActiveWorkplaces)) {
				log.info("Disabling AD account for substitute: " + substitute.getUsername());
				auditLogService.logSystem("Vikar disabled i AD grundet inaktivitet", "Vikaren har ingen aktive arbejdssteder", substitute);

				ADResponse result = webSocketService.disableADAccount(substitute);
				if (result.isSuccess()) {
					substitute.setDisabledInAd(true);
				}

				substituteDao.save(substitute);
			}
		}

		
		log.info("Finished substitute clean up task");
	}

	@Transactional
	public void syncOfficeLicenseTask() {
		for (Substitute substitute : substituteDao.findAll()) {
			boolean shouldHaveLicense = false;
			long activeWorkplacesWithLicenseRequiredCount = substitute.getWorkplaces().stream().filter(w -> !w.getStopDate().isBefore(LocalDate.now()) && w.isRequireO365License()).count();
			shouldHaveLicense = activeWorkplacesWithLicenseRequiredCount > 0;
			
			if (shouldHaveLicense && !substitute.isHasO365License()) {
				// add license
				ADResponse response = webSocketService.updateLicense(substitute, true);
				if (response.isSuccess()) {
					log.info("Adding office license to " + substitute.getUsername());
					substitute.setHasO365License(true);
					substituteDao.save(substitute);
					auditLogService.logSystem("Tilføjede office licens til vikar", "", substitute);
				}
				else {
					log.error("Failed to add license to " + substitute.getUsername() + " (" + response.getStatus() + ") - " + response.getMessage());
				}
			}
			else if (!shouldHaveLicense && substitute.isHasO365License()) {
				// remove license
				ADResponse response = webSocketService.updateLicense(substitute, false);
				if (response.isSuccess()) {
					log.info("Removing office license from " + substitute.getUsername());
					
					substitute.setHasO365License(false);
					substituteDao.save(substitute);
					auditLogService.logSystem("Fjernede office licens fra vikar", "", substitute);
				}
				else {
					log.error("Failed to remove license from " + substitute.getUsername() + " (" + response.getStatus() + ") - " + response.getMessage());
				}
			}
		}
	}
	
	@Transactional
	public void cleanUpWorkplaces() {
		log.info("Started workplace clean up job");
		
		LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
		int count = 0;

		for (Substitute substitute : substituteDao.findAll()) {
			List<Workplace> toDelete = substitute.getWorkplaces().stream().filter(w -> w.getStopDate().isBefore(oneMonthAgo)).collect(Collectors.toList());

			count += toDelete.size();
			if (!toDelete.isEmpty()) {
				substitute.getWorkplaces().removeAll(toDelete);
				substituteDao.save(substitute);
			}
		}

		log.info("Finished workplace clean up job. Deleted " + count + " workplaces.");
	}

	@Transactional
	public void syncAllADGroups() {
		List<GlobalTitleADGroupMapping> titleMappings = globalTitleADGroupMappingService.getAll();
		
		for (Substitute substitute : getAll()) {
			if (substitute.isUsernameFromSofd() || substitute.getUsername() == null) {
				continue;
			}
			
			ADResponse result = syncADGroups(substitute, titleMappings);

			if (!result.isSuccess()) {
				log.warn("Nightly synchronisation of AD groups failed for substitute with userId " + substitute.getUsername() + " and uuid " + substitute.getUuid());
			}
		}
	}
}
