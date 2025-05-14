package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dk.digitalidentity.os2vikar.dao.model.AuthorizationCode;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.service.model.ArrayOfHealthProfessional;
import dk.digitalidentity.os2vikar.service.model.HealthProfessional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthorizationCodeService {
	
	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private WebSocketService websocketService;

	@Transactional
	public void assignAuthorizationCodes() {
		for (Substitute substitute : substituteService.getWithUncheckedAuthorizationCodes()) {
			List<AuthorizationCode> result = getAuthorizationCodes(substitute.getName(), substitute.getCpr());
			
			if (!result.isEmpty()) {
				AuthorizationCode code = result.stream().filter(r -> r.isPrime()).findFirst().orElse(null);
				if (code != null) {
					substitute.setAuthorizationCode(code.getCode());

					websocketService.setAuthorizationCode(substitute.getUsername(), code.getCode());
				}
			}

			substitute.setAuthorizationCodeChecked(true);
			substituteService.save(substitute);
		}
	}
	
	public List<AuthorizationCode> getAuthorizationCodes(String name, String cpr) {
		String day = cpr.substring(0, 2);
		String month = cpr.substring(2, 4);
		String yearString = cpr.substring(4, 6);
		int year = Integer.parseInt(yearString);
		
		switch (cpr.charAt(6)) {
			case '0', '1', '2', '3' :
				yearString = "19" + yearString;
				break;
			case '4', '9' :
				if (year <= 36) {
					yearString = "20" + yearString;
				}
				else {
					yearString = "19" + yearString;
				}
				break;
			case '5', '6', '7', '8' :
				if (year <= 57) {
					yearString = "20" + yearString;
				}
				else {
					yearString = "18" + yearString;
				}
				break;
			default :
				return Collections.emptyList();
		}
		
		String date = yearString + "-" + month + "-" + day;
		
		// verify it is an actual CPR (robots and stuff will be skipped here)
		try {
			LocalDate.parse(date);
		}
		catch (Exception ex) {
			return Collections.emptyList();
		}
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = getHeaders();
		String url = "https://autregwebservice.sst.dk/autregservice.asmx/GetHealthProfessionals?name=" + name + "&authorizationId=\"\"&birthdayFrom=" + date + "&birthdayTo=" + date + "&authorizationDateFrom=1950-01-01&authorizationDateTo=2030-01-01&professionGroup=\"\"&specialityName=\"\"&authorizationStatus=NotSpecified&seventyFiveYearsRule=NotSpecified";

		HttpEntity<String> request = new HttpEntity<>(headers);
		
		ArrayOfHealthProfessional authResponse = null;
		String body = null;

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				body = response.getBody();
				if (body == null) {
					log.warn("Failed to fetch authorization code for person with name " + name + ". Body is null");
					return Collections.emptyList();
				}
				
				XmlMapper xmlMapper = XmlMapper.builder()
												.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
												.defaultUseWrapper(false)
												.addModule(new JavaTimeModule())
												.build();
				try {
					authResponse = xmlMapper.readValue(body, ArrayOfHealthProfessional.class);
				}
				catch (Exception ex) {
					log.error("Failed to fetch authorization code for person with name " + name + ". Could not parse xml", ex);
					return Collections.emptyList();
				}
			}
		}
		catch (HttpClientErrorException ex) {
			log.warn("Failed to fetch authorization code for person with uuid " + name + ". Error: " + ex.getResponseBodyAsString(), ex);
			return Collections.emptyList();
		}
		
		if (authResponse.getHealthProfessionals() == null || authResponse.getHealthProfessionals().isEmpty()) {
			return Collections.emptyList();
		}
		
		List<AuthorizationCode> sortedAndValidAuthorizationCodes = authResponse.getHealthProfessionals().stream()
				.filter(HealthProfessional::isAuthorizationValid)
				.sorted(Comparator.comparing(HealthProfessional::getAuthorizationDate).reversed())
				.map(this::toAuthorizationCode)
				.toList();
		
		// all authCodes are invalid
		if (sortedAndValidAuthorizationCodes.isEmpty()) {
			return Collections.emptyList();
		}

		// flag the first as prime
		sortedAndValidAuthorizationCodes.get(0).setPrime(true);
		
		return sortedAndValidAuthorizationCodes;
	}

	private AuthorizationCode toAuthorizationCode(HealthProfessional healthProfessional) {
		AuthorizationCode authorizationCode = new AuthorizationCode();
		authorizationCode.setCode(healthProfessional.getAuthorizationID());
		authorizationCode.setName(healthProfessional.getProfessionCodeName());
		authorizationCode.setPrime(false);
		
		return authorizationCode;
	}
	
	private static HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "text/xml; charset=utf-8");
		headers.add("Accept", "text/xml");

		return headers;
	}
}
