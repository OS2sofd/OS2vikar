package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.FKOrganisationService;
import dk.digitalidentity.os2vikar.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class UpdateOrgUnitsTask {

    @Autowired
	private FKOrganisationService fkOrganisationService;

    @Autowired
    private OS2VikarConfiguration configuration;

    @Autowired
    private OrgUnitService orgUnitService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
    	if (orgUnitService.getAll().size() == 0) {
    		log.warn("No organisation - loading a full organisation!");

    		try {
    			fkOrganisationService.updateOrgUnits();
    		}
    		catch (Exception ex) {
    			log.error("Failed to load organisation", ex);
    		}
    	}
    }

    @Scheduled(cron = "${cron.fkorg:0 #{new java.util.Random().nextInt(55)} 13 * * ?}")
    public void execute() throws Exception {
		if (!configuration.isScheduledJobsEnabled() || !configuration.getOrganisation().isEnabled()) {
			return;
		}

		fkOrganisationService.updateOrgUnits();
    }
}
