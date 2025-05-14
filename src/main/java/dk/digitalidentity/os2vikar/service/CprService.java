package dk.digitalidentity.os2vikar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.dto.CPRServiceLookupDTO;
import dk.digitalidentity.os2vikar.service.dto.CprLookupDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CprService {

	@Autowired
	private OS2VikarConfiguration config;

	public CprLookupDto cprLookup(String cpr) {
		if (!validCpr(cpr)) {
			return null;
		}

		if (!config.getCpr().isDev()) {
			var restTemplate = new RestTemplate();

			String cprResourceUrl = config.getCpr().getUrl();
			if (!cprResourceUrl.endsWith("/")) {
				cprResourceUrl += "/";
			}
			cprResourceUrl += "api/person?cpr=" + cpr + "&cvr=" + config.getCvr();

			try {
				ResponseEntity<CPRServiceLookupDTO> response = restTemplate.getForEntity(cprResourceUrl, CPRServiceLookupDTO.class);
				CPRServiceLookupDTO dto = response.getBody();
				CprLookupDto result = null;
				if (dto != null) {
					result = new CprLookupDto();
					// Name
					result.setName(dto.getFirstname());
					result.setSurname(dto.getLastname());
					result.setCpr(cpr);
				}

				return result;
			}
			catch (RestClientException ex) {
				log.warn("Failed to lookup: " + cpr.substring(0, 6) + "-XXXX", ex);

				return null;
			}
		}
		else {
			CprLookupDto result = new CprLookupDto();
			// Name
			result.setName("Hans");
			result.setSurname("Petersen");
			result.setCpr(cpr);

			return result;
		}
	}

	public boolean validCpr(String cpr) {
		if (cpr == null || cpr.length() != 10) {
			return false;
		}

		for (char c : cpr.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}

		int days = Integer.parseInt(cpr.substring(0, 2));
		int month = Integer.parseInt(cpr.substring(2, 4));

		if (days < 1 || days > 31) {
			return false;
		}

		if (month < 1 || month > 12) {
			return false;
		}

		return true;
	}
}