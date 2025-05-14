package dk.digitalidentity.os2vikar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NexusSyncService {
	private static final String APPLICATION_JSON = "application/json";

	@Autowired
	private OS2VikarConfiguration configuration;

	public boolean resetNexus(String username) {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = getHeaders();

		HttpEntity<String> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(configuration.getNexus().getSyncUrl() + username + "/" + configuration.getCvr(), HttpMethod.PUT, request, String.class);
			if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.NOT_FOUND) {
				log.warn("Failed to reset user in Nexus - " + username + " http status " + response.getStatusCodeValue());
				return false;
			}
		}
		catch (HttpClientErrorException ex) {
			log.warn("Failed to reset user in Nexus - " + username, ex);

			if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
				return false;
			}
		}
		
		return true;
	}
	
	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getNexus().getSyncApiKey());
		headers.add("Content-Type", APPLICATION_JSON);
		headers.add("Accept", APPLICATION_JSON);
		
		return headers;
	}
}
