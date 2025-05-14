package dk.digitalidentity.os2vikar.service;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.os2vikar.dao.model.CmsMessage;
import dk.digitalidentity.os2vikar.service.dto.CmsMessageListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CmsMessageBundle {

	@Autowired
	private CmsMessageService cmsMessageService;
	
	@Autowired
	private CmsMessageSource messageSource;
	
	public String getText(String key) {
		return getText(key, false);
	}
	
	public String getText(String key, boolean bypassCache) {
		String value = null;

		if (bypassCache) {
			CmsMessage cmsMessage = cmsMessageService.getByCmsKey(key);
			if (cmsMessage != null) {
				value = cmsMessage.getCmsValue();
			}
		}
		else {
			value = cmsMessageService.getCmsMap().get(key);
		}

		if (value == null) {
			// This accesses both the messages.properties in the ui and idp project depending on where the
			// method is called from. Means that the texts are in both
			value = messageSource.getMessage(key, null, "", null);
		}

		return value;
	}
	
	public List<CmsMessageListDTO> getAll() {
		List<CmsMessageListDTO> all = new ArrayList<>();

		all.add(new CmsMessageListDTO("cms.userguide.text", getDescription("cms.userguide.text")));
		all.add(new CmsMessageListDTO("cms.passwordChangeGuide.text", getDescription("cms.passwordChangeGuide.text")));
		all.add(new CmsMessageListDTO("cms.orgunits.helptext", getDescription("cms.orgunits.helptext")));
		all.add(new CmsMessageListDTO("cms.orgunits.titles.helptext", getDescription("cms.orgunits.titles.helptext")));
		all.add(new CmsMessageListDTO("cms.orgunits.roles.helptext", getDescription("cms.orgunits.roles.helptext")));
		all.add(new CmsMessageListDTO("cms.titles.helptext", getDescription("cms.titles.helptext")));
		all.add(new CmsMessageListDTO("cms.roles.helptext", getDescription("cms.roles.helptext")));
		all.add(new CmsMessageListDTO("cms.substitutes.helptext", getDescription("cms.substitutes.helptext")));
		all.add(new CmsMessageListDTO("cms.accountpool.helptext", getDescription("cms.accountpool.helptext")));
		all.add(new CmsMessageListDTO("cms.settings.helptext", getDescription("cms.settings.helptext")));
		all.add(new CmsMessageListDTO("cms.log.helptext", getDescription("cms.log.helptext")));
		all.add(new CmsMessageListDTO("cms.workplace.wait.helptext", getDescription("cms.workplace.wait.helptext")));
		all.add(new CmsMessageListDTO("cms.orgunits.ad_groups.helptext", getDescription("cms.orgunits.ad_groups.helptext")));
		all.add(new CmsMessageListDTO("cms.orgunits.it_systems.helptext", getDescription("cms.orgunits.it_systems.helptext")));
		all.add(new CmsMessageListDTO("cms.titles.ad_groups.helptext", getDescription("cms.titles.ad_groups.helptext")));
		all.add(new CmsMessageListDTO("cms.titles.userroles.helptext", getDescription("cms.titles.userroles.helptext")));
		all.add(new CmsMessageListDTO("cms.orgunits.userroles.helptext", getDescription("cms.orgunits.userroles.helptext")));

		return all;
	}
	
	public String getDescription(String key) {
		switch(key) {
			case "cms.userguide.text":
				return "Den tekst der står i brugervejledningen.";
			case "cms.passwordChangeGuide.text":
				return "Den tekst der vises i guiden til at skifte kodeord.";
			case "cms.orgunits.helptext":
				return "Den korte hjælpetekst der står øverst på enhedssiden.";
			case "cms.orgunits.titles.helptext":
				return "Den korte hjælpetekst der står under fanen arbejdstitler på enhedssiden. Fanen vises når der er valgt en enhed, der kan have vikarer.";
			case "cms.orgunits.roles.helptext":
				return "Den korte hjælpetekst der står under fanen rettigheder på enhedssiden. Fanen vises når der er valgt en enhed, der kan have vikarer.";
			case "cms.titles.helptext":
				return "Den hjælpetekst der står arbejdstitelsiden.";
			case "cms.roles.helptext":
				return "Den hjælpetekst der står rettighedssiden.";
			case "cms.substitutes.helptext":
				return "Den hjælpetekst der står på vikarsiden.";
			case "cms.accountpool.helptext":
				return "Den hjælpetekst der står på siden med puljen af brugerkonti.";
			case "cms.settings.helptext":
				return "Den hjælpetekst der står på opsætningssiden.";
			case "cms.log.helptext":
				return "Den hjælpetekst der står på logsiden.";
			case "cms.workplace.wait.helptext":
				return "Den hjælpetekst der forklarer, at der kan gå noget tid før vikaren får tildelt en fagrolle ved oprettelse af arbejdssted.";
			case "cms.orgunits.ad_groups.helptext":
				return "Den korte hjælpetekst der står under fanen Active Directory grupper på enhedssiden. Fanen vises, når der er valgt en enhed, der kan have vikarer.";
			case "cms.titles.ad_groups.helptext":
				return  "Den korte hjælpetekst der står når man opretter eller redigerer en arbejdstitel.";
			case "cms.orgunits.it_systems.helptext":
				return  "Den korte hjælpetekst der står under fanen IT-systemer på enhedssiden. Fanen vises, når der er valgt en enhed, der kan have vikarer.";
			case "cms.titles.it_systems.helptext":
				return  "Den korte hjælpetekst der står ved IT-systemer, når man opretter eller redigerer en arbejdstitel.";
			case "cms.titles.userroles.helptext":
				return "Den korte hjælpetekst der står ved automatiske rolletildelinger, når man opretter eller redigerer en arbejdstitel.";
			case "cms.orgunits.userroles.helptext":
				return "Den korte hjælpetekst der står under fanen automatiske rolletildelinger på enhedssiden. Fanen vises, når der er valgt en enhed, der kan have vikarer.";

			default:
				log.error("Key does not have a description: " + key);
				return "";
		}
	}
}
