package dk.digitalidentity.os2vikar.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.ADAccountPoolDao;
import dk.digitalidentity.os2vikar.dao.model.ADAccountPool;
import dk.digitalidentity.os2vikar.dao.model.enums.ADAccountPoolStatus;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ADAccountPoolService {

	@Autowired
	private ADAccountPoolDao adAccountPoolDao;
	
	@Autowired 
	private SettingsService settingsService;
	
	@Autowired
	private OS2VikarConfiguration config;
	
	@Autowired
	private WebSocketService webSocketService;
	
	public List<ADAccountPool> getAll() {
		return adAccountPoolDao.findAll();
	}
	
	public List<ADAccountPool> getByStatusAndWithO365License(ADAccountPoolStatus status, boolean withO365License) {
		return adAccountPoolDao.findByStatusAndWithO365License(status, withO365License);
	}
	
	public void delete(ADAccountPool adAccountPool) {
		adAccountPoolDao.delete(adAccountPool);
	}
	
	public ADAccountPool save(ADAccountPool adAccountPool) {
		return adAccountPoolDao.save(adAccountPool);
	}
	
	@Transactional
	public void create() {
		List<ADAccountPool> currentAccountsWithLicence = adAccountPoolDao.findByStatusAndWithO365License(ADAccountPoolStatus.OK, true);
		List<ADAccountPool> currentAccountsWithoutLicence = adAccountPoolDao.findByStatusAndWithO365License(ADAccountPoolStatus.OK, false);
		List<ADAccountPool> newAccounts = new ArrayList<>();

		long withLicenseCount = currentAccountsWithLicence.size();
		long withoutLicenseCount = currentAccountsWithoutLicence.size();
		long createNumberWithLicense = config.getPreCreateAD().getCreateNumberWithLicense();
		long createNumberWithoutLicense = config.getPreCreateAD().getCreateNumberWithoutLicense();
		
		if (withLicenseCount > createNumberWithLicense || withoutLicenseCount > createNumberWithoutLicense) {
			log.debug("More accounts in pool than necessary");
		}
		else if (withLicenseCount == createNumberWithLicense && withoutLicenseCount == createNumberWithoutLicense) {
			log.debug("Not creating AD accounts. Account pool has enough.");	
		}
		else {
			int nameCounter = settingsService.getADUsernameCount();
			
			try {
				if (config.getO365().isEnabled()) {
					while (withLicenseCount < createNumberWithLicense) {
						String username = "vik" + nameCounter;
						ADResponse response = webSocketService.createAccount(username, true);

						if (response.isSuccess()) {
							ADAccountPool account = new ADAccountPool();
							account.setStatus(ADAccountPoolStatus.OK);
							account.setUsername(username);
							account.setWithO365License(true);
							newAccounts.add(account);
							withLicenseCount++;

							// custom logging - sometimes the create went ok, but we still got a partial error message
							if (StringUtils.hasLength(response.getMessage())) {
								log.error("Partial create: " + response.getMessage());
							}
							
							// initial state should be disabled
							ADResponse lockResponse = webSocketService.disableADAccount(username);
							if (!lockResponse.isSuccess()) {
								log.warn("Failed to set initial state to disabled on: " + username);
							}
						}
						else {
							if (response.getStatus() == ADStatus.TIMEOUT) {
								log.warn("Failed to create AD account with license (" + response.getStatus() + ") - " + response.getMessage());
							}
							else {
								log.error("Failed to create AD account with license (" + response.getStatus() + ") - " + response.getMessage());
							}
							break;
						}

						nameCounter++;
					}
				}

				while (withoutLicenseCount < createNumberWithoutLicense) {
					String username = "vik" + nameCounter;
	
					ADResponse response = webSocketService.createAccount(username, false);
					if (response.isSuccess()) {
						ADAccountPool account = new ADAccountPool();
						account.setStatus(ADAccountPoolStatus.OK);
						account.setUsername(username);
						account.setWithO365License(false);
						newAccounts.add(account);
						withoutLicenseCount++;
						
						// initial state should be disabled
						ADResponse lockResponse = webSocketService.disableADAccount(username);
						if (!lockResponse.isSuccess()) {
							log.warn("Failed to set initial state to disabled on: " + username);
						}
					}
					else {
						if (response.getStatus() == ADStatus.TIMEOUT) {
							log.warn("Failed to create AD account without license (" + response.getStatus() + ") - " + response.getMessage());
						}
						else {
							log.error("Failed to create AD account without license (" + response.getStatus() + ") - " + response.getMessage());
						}
						break;
					}
	
					nameCounter++;
				}
			}
			catch (Exception ex) {
				log.error("Unexpected error during account creation", ex);
			}
			finally {
				settingsService.setADUsernameCount(nameCounter);
				adAccountPoolDao.saveAll(newAccounts);
			}
			
			log.info("Finished AD account create job. Created " + newAccounts.size() + " new accounts.");
		}
	}
}
