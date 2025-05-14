package dk.digitalidentity.os2vikar.service;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
public class SofdService {

    @Autowired
    private OS2VikarConfiguration config;

    @Autowired
    private RestTemplate restTemplate;

    public record InSofdResult(boolean inSofd, boolean hasADUser, String ADUsername) {}
    private record SofdPerson(Set<SofdAffiliation> affiliations, Set<SofdUser> users){}
    private record SofdAffiliation(String masterId, LocalDate startDate, LocalDate stopDate){}
    private record SofdUser(String userType, boolean prime, String userId){}
    public InSofdResult isInSofd(String cpr) {
    	if (!config.getSofd().isEnableLookup()) {
    		return new InSofdResult(false, false, null);
    	}
    	
        HttpEntity<String> request = new HttpEntity<>(getHeaders(config.getSofd().getApiKey()));
        String query = "/api/v2/persons/byCpr/" + cpr;

        try {
            ResponseEntity<SofdPerson> response = restTemplate.exchange(config.getSofd().getBaseUrl() + query, HttpMethod.GET, request, SofdPerson.class);
            if (!response.getStatusCode().equals(HttpStatus.OK) || response.getBody() == null) {
                log.warn("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx, response=" + response.getBody());
                return null;
            }

            boolean hasActiveSofdAffiliation = false;
            SofdPerson person = response.getBody();

            long affiliationCount = person.affiliations().stream().filter(a -> !a.masterId().equals(config.getSofd().getVikarMasterId()) && active(a)).count();
            if (affiliationCount > 0) {
                hasActiveSofdAffiliation = true;
            }

            SofdUser primeUser = person.users().stream().filter(u -> u.userType().equals("ACTIVE_DIRECTORY") && u.prime()).findAny().orElse(null);

            return new InSofdResult(hasActiveSofdAffiliation, primeUser != null, primeUser != null ? primeUser.userId() : null);
        }
        catch (Exception e) {
        	if (e instanceof HttpClientErrorException) {
        		HttpClientErrorException ex = (HttpClientErrorException) e;
        		
        		if (ex.getStatusCode().is4xxClientError() || ex.getStatusCode().is5xxServerError()) {
        			if (ex.getRawStatusCode() == 404) {
        				return new InSofdResult(false, false, null);
        			}
        				
       				log.error("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx", ex);
        		}
        		else {
            		log.warn("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx with code: ", ex.getStatusCode());
        		}
        	}
        	else {
        		log.error("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx", e);
        	}
        	
            return null;
        }
    }

    private boolean active(SofdAffiliation affiliation) {
        if (!notActiveYet(affiliation) && !notActiveAnymore(affiliation)) {
            return true;
        }

        return false;
    }

    private boolean notActiveYet(SofdAffiliation affiliation) {
        return (affiliation.startDate() != null && (affiliation.startDate().isAfter(LocalDate.now()) || affiliation.startDate().equals(LocalDate.now())));
    }

    private boolean notActiveAnymore(SofdAffiliation affiliation) {
        return (affiliation.stopDate() != null && (affiliation.stopDate().isBefore(LocalDate.now()) || affiliation.stopDate().equals(LocalDate.now())));
    }

    private HttpHeaders getHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", apiKey);
        headers.add("Content-Type", "application/json");

        return headers;
    }
}
