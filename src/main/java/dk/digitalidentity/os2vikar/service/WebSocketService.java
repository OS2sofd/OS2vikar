package dk.digitalidentity.os2vikar.service;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebSocketService {

	@Autowired
	private SocketHandler socketHandler;
	
	@Autowired
	private OS2VikarConfiguration configuration;

	public SocketHandler getSocketHandler() {
		return socketHandler;
	}

	public ADResponse setPassword(String userId, String password) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		try {
			Future<ADResponse> result = socketHandler.setPassword(userId, password);

			return result.get();
		}
		catch (Exception ex) {
			log.error("Failed to set password for " + userId, ex);

			response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
		}

		return response;
	}

	public ADResponse createAccount(String userId, boolean withLicense) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		try {
			Future<ADResponse> result = socketHandler.createAccount(userId, withLicense);

			response = result.get();
			
			// sometimes a previous create attempt failed due to timeout, and then a later call will get this message
			// back - so we just map it to a success instead to avoid issues
			if (response.getStatus().equals(ADStatus.FAILURE) && response.getMessage() != null && response.getMessage().contains("The object already exists")) {
				response.setStatus(ADStatus.OK);
				response.setMessage(null);
			}
		}
		catch (Exception ex) {
			log.error("Failed to create account for " + userId, ex);
			
			response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
		}
		
		return response;
	}
	
	public ADResponse associateAccount(Substitute substitute, String userId) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String tts = SubstituteService.getExpireTime(substitute);
		String name = substitute.getName() + " " + substitute.getSurname();
		String cpr = substitute.getCpr();
		
		if (configuration.getWebsockets().isEncodedCpr()) {
            long val = Long.parseLong(cpr);

            val++;
            val *= 33;
            
            cpr = Long.toString(val);
		}

		try {
			Future<ADResponse> result = socketHandler.associateAccount(userId, name, cpr, tts);

			return result.get();
		}
		catch (Exception ex) {
			log.error("Failed to associcate account for " + userId + " / " + substitute.getId(), ex);
			
			response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
		}
		
		return response;
	}
	
	public ADResponse updateLicense(Substitute substitute, boolean shouldHaveLicense) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}
		
		String userId = substitute.getUsername();

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());			
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.updateLicense(userId, shouldHaveLicense);
				
				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to update license for " + userId + " / " + shouldHaveLicense, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}
	
	public ADResponse setExpire(Substitute substitute, boolean checkStatus) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String userId = substitute.getUsername();
		String tts = SubstituteService.getExpireTime(substitute);
		
		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());			
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.setExpire(userId, tts, checkStatus, checkStatus ? substitute.getCpr() : null);
				
				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to update expireDate for " + userId + " / " + tts, ex);
				
				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}

	public ADResponse disableADAccount(Substitute substitute) {
		if (substitute.isUsernameFromSofd()) {
			ADResponse response = new ADResponse();
			response.setStatus(ADStatus.OK);

			return response;
		}

		return disableADAccount(substitute.getUsername());
	}
	
	public ADResponse enableADAccount(String userId) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: tom AD konto on enable?");
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.enableAccount(userId);

				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to enable AD account " + userId, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}
	
	public ADResponse disableADAccount(String userId) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: tom AD konto?");
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.disableAccount(userId);

				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to disable AD account " + userId, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}
	
	public ADResponse deleteADAccount(Substitute substitute) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String userId = substitute.getUsername();

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.deleteAccount(userId);

				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to delete AD account " + userId, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}

    public ADResponse addToEmployeeSignatureGroup(Substitute substitute) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String userId = substitute.getUsername();

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.addToEmployeeSignatureGroup(userId);

				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to add substitute " + userId + " to employee signature group", ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
    }

	public ADResponse handleGroupMemberships(Substitute substitute, List<String> guids) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd() || guids.isEmpty()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String userId = substitute.getUsername();

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.handleGroupMemberships(userId, guids);

				return result.get();
			}
			catch (Exception ex) {
				// not really an issue - most likely it is just a timeout because sync takes a long time (at least in some municipalities)
				log.warn("Failed to handle AD groups for substitute " + userId, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}

	public ADResponse unlockADAccount(Substitute substitute) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		if (substitute.isUsernameFromSofd()) {
			response.setStatus(ADStatus.OK);
			return response;
		}

		String userId = substitute.getUsername();

		if (!StringUtils.hasLength(userId)) {
			response.setMessage("Middleware: Ingen AD brugerkonto for vikar ID " + substitute.getId());
		}
		else {
			try {
				Future<ADResponse> result = socketHandler.unlockAccount(userId);

				return result.get();
			}
			catch (Exception ex) {
				log.error("Failed to unlock AD account " + userId, ex);

				response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
			}
		}

		return response;
	}

	public ADResponse setAuthorizationCode(String userId, String code) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		try {
			Future<ADResponse> result = socketHandler.setAuthorizationCode(userId, code);

			return result.get();
		}
		catch (Exception ex) {
			log.error("Failed to set password for " + userId, ex);

			response.setMessage("Middleware: Teknisk fejl. " + ex.getMessage());
		}

		return response;
	}
}
