package dk.digitalidentity.os2vikar.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2vikar.config.Commands;
import dk.digitalidentity.os2vikar.config.modules.WebSocketsConfiguration;
import dk.digitalidentity.os2vikar.dao.model.WebsocketQueue;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import dk.digitalidentity.os2vikar.service.model.Request;
import dk.digitalidentity.os2vikar.service.model.RequestHolder;
import dk.digitalidentity.os2vikar.service.model.Response;
import dk.digitalidentity.os2vikar.service.model.ResponseHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableAsync(proxyTargetClass = true)
@Component
public class SocketHandler extends TextWebSocketHandler {
	private static SecureRandom random = new SecureRandom();
	private List<Session> sessions = new ArrayList<>();
	private Map<String, RequestHolder> requests = new ConcurrentHashMap<>();
	private Map<String, ResponseHolder> responses = new ConcurrentHashMap<>();
	private int timeoutCounter = 0; // not thread-safe, but also not critical that it is
	private int noWebsocketConnectionCounter = 0;
	
	@Autowired
	private WebSocketsConfiguration configuration;

	// lazy to resolve the circular reference issue at bootup
	@Lazy
	@Autowired
	private WebsocketQueueService websocketQueueService;
	
	// is called directly from the UI
	public boolean isConnectedToAD() {
		return !sessions.isEmpty();
	}

	@Async
	public Future<ADResponse> addToEmployeeSignatureGroup(String userId) throws InterruptedException {
		return internalAddToEmployeeSignatureGroup(userId, true);
	}

	@Async
	public Future<ADResponse> handleGroupMemberships(String userId, List<String> guids) throws InterruptedException {
		return internalHandleGroupMemberships(userId, guids.stream().collect(Collectors.joining(",")), true);
	}

	@Async
	public Future<ADResponse> addToEmployeeSignatureGroupWithoutRetry(String userId) throws InterruptedException {
		return internalAddToEmployeeSignatureGroup(userId, false);
	}

	@Async
	public Future<ADResponse> handleGroupMembershipsWithoutRetry(String userId, String payload) throws InterruptedException {
		return internalHandleGroupMemberships(userId, payload, false);
	}
	
	private Future<ADResponse> internalAddToEmployeeSignatureGroup(String userId, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.EMPLOYEE_SIGNATURE);
		request.setTarget(userId);
		request.setPayload("");

		ADResponse response = sendAndGet(request, "employeeSignature");

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	private Future<ADResponse> internalHandleGroupMemberships(String userId, String groupGuids, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.AD_GROUPS_SYNC);
		request.setTarget(userId);
		request.setPayload(groupGuids);

		// allow 30 seconds responsetime here (crazy, but it looks like updating in some municipalities takes very long)
		ADResponse response = sendAndGet(request, "handleADGroups", 300);

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> deleteAccount(String userId) throws InterruptedException {
		return internalDeleteAccount(userId, true);
	}
	
	@Async
	public Future<ADResponse> deleteAccountWithoutRetry(String userId) throws InterruptedException {
		return internalDeleteAccount(userId, false);
	}
	
	private Future<ADResponse> internalDeleteAccount(String userId, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.DELETE_ACCOUNT);
		request.setTarget(userId);
		request.setPayload("");

		ADResponse response = sendAndGet(request, "deleteAccount");

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> disableAccount(String userId) throws InterruptedException {
		return internalDisableAccount(userId, true);
	}
	
	@Async
	public Future<ADResponse> disableAccountWithoutRetry(String userId) throws InterruptedException {
		return internalDisableAccount(userId, false);
	}
	
	private Future<ADResponse> internalDisableAccount(String userId, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.DISABLE_ACCOUNT);
		request.setTarget(userId);
		request.setPayload("");

		ADResponse response = sendAndGet(request, "disableAccount");

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> setExpire(String userId, String tts, boolean checkStatus, String cpr) throws InterruptedException {
		return internalSetExpire(userId, tts, true, checkStatus, cpr);
	}

	@Async
	public Future<ADResponse> setExpireWithoutRetry(String userId, String tts) throws InterruptedException {
		return internalSetExpire(userId, tts, false, false, null);
	}

	private Future<ADResponse> internalSetExpire(String userId, String tts, boolean retry, boolean checkStatus, String cpr) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.SET_EXPIRE);
		request.setTarget(userId);
		if (configuration.isCheckStatusWhenSetExpire()) {
			request.setPayload(tts + "," + checkStatus + "," + cpr);
		} else {
			request.setPayload(tts);
		}


		log.info("Set expire: userId=" + userId + ", tts=" + tts);

		ADResponse response = sendAndGet(request, "setExpire");

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> updateLicense(String userId, boolean shouldHaveLicense) throws InterruptedException {
		return internalUpdateLicense(userId, shouldHaveLicense, true);
	}
	
	@Async
	public Future<ADResponse> updateLicenseWithoutRetry(String userId, boolean shouldHaveLicense) throws InterruptedException {
		return internalUpdateLicense(userId, shouldHaveLicense, false);
	}
	
	private Future<ADResponse> internalUpdateLicense(String userId, boolean shouldHaveLicense, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.UPDATE_LICENSE);
		request.setTarget(userId);
		request.setPayload(shouldHaveLicense ? "true" : "false");

		ADResponse response = sendAndGet(request, "updateLicense");
		
		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> unlockAccount(String userId) throws InterruptedException {
		return internalUnlockAccount(userId, true);
	}

	@Async
	public Future<ADResponse> unlockAccountWithoutRetry(String userId) throws InterruptedException {
		return internalUnlockAccount(userId, false);
	}

	private Future<ADResponse> internalUnlockAccount(String userId, boolean retry) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.UNLOCK_ACCOUNT);
		request.setTarget(userId);
		request.setPayload("");

		ADResponse response = sendAndGet(request, "unlockAccount");

		// in case of NO_CONNECTION, we do not attempt to hide that information, so the caller still needs
		// to deal with that issue, even though we retry in 5 minutes to perform the same action. This also
		// means that this approach to retrying should only be done on idempotent operations, where a retry
		// would not have any ill effect
		if (retry && response.getStatus().equals(ADStatus.NO_CONNECTION)) {
			retryRequest(request);
		}

		return new AsyncResult<>(response);
	}

	@Async
	public Future<ADResponse> associateAccount(String userId, String name, String cpr, String tts) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.ASSOCIATE_ACCOUNT);
		request.setTarget(userId);
		request.setPayload(name + ":" + tts + ":" + cpr);

		log.info("Associate Account: name=" + name + ", tts=" + tts);
		
		ADResponse response = sendAndGet(request, "associateAccount", 450); // wait up to 45 seconds for a response, as this can take a while due to office-365

		return new AsyncResult<>(response);
	}
	
	@Async
	public Future<ADResponse> createAccount(String userId, boolean withLicense) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.CREATE_ACCOUNT);
		request.setTarget(userId);
		request.setPayload(withLicense ? "true" : "false");

		ADResponse response = sendAndGet(request, "createAccount");

		return new AsyncResult<>(response);
	}
	
	@Async
	public Future<ADResponse> setPassword(String userId, String password) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.SET_PASSWORD);
		request.setTarget(userId);
		request.setPayload(password);

		ADResponse response = sendAndGet(request, "setPassword");

		return new AsyncResult<>(response);
	}

	public void cleanupRequestResponse() {
		for (String key : requests.keySet()) {
			RequestHolder holder = requests.get(key);

			if (holder.getTts().isBefore(LocalDateTime.now())) {
				requests.remove(key);
			}
		}
		
		for (String key : responses.keySet()) {
			ResponseHolder holder = responses.get(key);

			if (holder.getTts().isBefore(LocalDateTime.now())) {
				responses.remove(key);
			}
		}
	}
	
	public void closeStaleSessions() {
		for (Session session : sessions) {
			if (session.getCleanupTimestamp().isBefore(LocalDateTime.now())) {
				try {
					log.info("Closing stale connection on websocket client - sessionID = " + session.getId());

					session.getSession().close();
				}
				catch (Exception ex) {
					log.warn("Failed to close connection - sessionID = " + session.getId(), ex);
				}
				
				// only close a single state connection per loop - do not want to end up with 0 connections during a single cleanup run
				break;
			}
		}
	}

	@Async
	public Future<ADResponse> setAuthorizationCode(String userId, String code) throws InterruptedException {
		Request request = new Request();
		request.setCommand(Commands.SET_AUTHORIZATION_CODE);
		request.setTarget(userId);
		request.setPayload(code);

		ADResponse response = sendAndGet(request, "setAuthorizationCode");

		return new AsyncResult<>(response);
	}


	//// INTERNAL METHODS ////
	
	@Override
	public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
		Session session = new Session(webSocketSession);

		synchronized (sessions) {
			sessions.add(session);
		}
		
		log.info("Connection established - sending AuthRequest - sessionID=" + session.getId());

		sendAuthenticateRequest(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {
		synchronized (sessions) {
			sessions.removeIf(s -> s.getSession().equals(webSocketSession));
		}

		log.info("After closing connection: " + sessions.size());
	}

	// Handle the response from the client
	@Override
	public void handleTextMessage(WebSocketSession webSocketSession, TextMessage textMessage) throws InterruptedException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Response message = mapper.readValue(textMessage.getPayload(), Response.class);

		RequestHolder holder = requests.get(message.getTransactionUuid());
		if (holder == null && Objects.equals(Commands.AD_GROUPS_SYNC, message.getCommand())) {
			log.warn("Got response for unknown request: " + message);
			return;
		}
		else if (holder == null) {
			log.error("Got response for unknown request: " + message);
			return;			
		}
		
		Request inResponseTo = holder.getRequest();
		if (!inResponseTo.validateEcho(message)) {
			log.error("Response does not echo correctly! request=" + inResponseTo + ", response=" + message);
			invalidateRequest(message.getTransactionUuid(), "Response did not correctly echo request");
			return;
		}

		if (!message.verify(configuration.getWebSocketKey())) {
			log.error("Got invalid hmac on " + message.getCommand() + " response: " + message);
			invalidateRequest(message.getTransactionUuid(), "Got invalid hmac on response");
			return;
		}

		switch (message.getCommand()) {
			case Commands.AUTHENTICATE:
				Optional<Session> clientSession = sessions.stream().filter(cs -> cs.getSession().equals(webSocketSession)).findAny();
				if (clientSession != null) {
					handleAuthenticateResponse(clientSession.get(), message, holder.getRequest());
				}
				else {
					log.error("We got a message from an unknown websocket client: " + message);
				}
				break;
			case Commands.SET_PASSWORD:
			case Commands.CREATE_ACCOUNT:
			case Commands.ASSOCIATE_ACCOUNT:
			case Commands.UPDATE_LICENSE:
			case Commands.SET_EXPIRE:
			case Commands.DELETE_ACCOUNT:
			case Commands.DISABLE_ACCOUNT:
			case Commands.EMPLOYEE_SIGNATURE:
			case Commands.AD_GROUPS_SYNC:
			case Commands.UNLOCK_ACCOUNT:
				handleResponse(message);
				break;
			default:
				log.error("Unknown command for message: " + message);
				break;
		}
	}

	private ADResponse sendAndGet(Request request, String operation) throws InterruptedException {
		return sendAndGet(request, operation, 50);
	}

	private ADResponse sendAndGet(Request request, String operation, long timeout) throws InterruptedException {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.FAILURE);

		Session session = getSession();
		if (session == null) {
			noWebsocketConnectionCounter++;
			if (noWebsocketConnectionCounter > 10) {
				log.error("Failed to get an authenticated WebSocket connection for " + operation);
			}
			else {
				log.warn("Failed to get an authenticated WebSocket connection for " + operation);
			}
			
			response.setMessage("Middleware: Ingen forbindelse til AD");
			response.setStatus(ADStatus.NO_CONNECTION);
			
			return response;
		}

		noWebsocketConnectionCounter = 0;
		
		try {
			request.sign(configuration.getWebSocketKey());
		}
		catch (Exception ex) {
			log.error("Failed to sign " + operation + " message", ex);
			response.setMessage("Middleware: sikkerhedsfejl i signering. " + ex.getMessage());

			return response;
		}

		String data = null;
		try {
			ObjectMapper mapper = new ObjectMapper();

			data = mapper.writeValueAsString(request);
		}
		catch (JsonProcessingException ex) {
			response.setMessage("Middleware: Serialiseringsfejl. " + ex.getMessage());
			response.setStatus(ADStatus.TECHNICAL_ERROR);

			return response;
		}
		
		TextMessage message = new TextMessage(data);
		try {
			requests.put(request.getTransactionUuid(), new RequestHolder(request));

			session.getSession().sendMessage(message);
		}
		catch (IOException ex) {
			log.error("Failed to send " + operation + " request", ex);

			response.setMessage("Middleware: Kunne ikke kontakte AD. " + ex.getMessage());
			response.setStatus(ADStatus.TECHNICAL_ERROR);

			return response;
		}
		
		ResponseHolder holder = getResponse(request.getTransactionUuid(), timeout);
		if (holder == null) {
			if (timeoutCounter >= 5 && !("handleADGroups".equals(operation))) {
				log.error("Timeout waiting for response on " + operation + " on transactionUuid " + request.getTransactionUuid());
			}
			else {
				log.warn("Timeout waiting for response on " + operation + " on transactionUuid " + request.getTransactionUuid());
			}

			response.setMessage("Middleware: timeout");
			response.setStatus(ADStatus.TIMEOUT);
			
			// handleADGroups almost always gives a timeout, so ignore those
			if (!Objects.equals(operation, "handleADGroups")) {
				timeoutCounter++;
			}

			return response;
		}
		
		timeoutCounter = 0;

		// do some centralized logging of errors
		response = holder.getResponse();
		if (!response.isSuccess()) {
			// if we got a timeout on create, the account might actually have been created successfully anyway
			if ("createAccount".equals(operation) && response.getMessage() != null && response.getMessage().contains("The object already exists")) {
				response.setStatus(ADStatus.OK);
			}
			else if ("deleteAccount".equals(operation) && response.getMessage() != null && response.getMessage().contains("Der findes ingen AD konto med bruger-id")) {
				response.setStatus(ADStatus.OK);
			}
			else if ("handleADGroups".equals(operation) && response.getMessage() != null && response.getMessage().contains("Kunne ikke finde bruger med brugernavn")) {
				response.setStatus(ADStatus.OK);
			}
			else {
				log.error("Operation " + operation + " failed with status " + response.getStatus() + " and message = " + response.getMessage());
			}
		}

		return response;
	}

	private ResponseHolder getResponse(String transactionUuid, long timeout) throws InterruptedException {
		int tries = 0;
		ResponseHolder holder = null;

		do {
			holder = responses.get(transactionUuid);
			
			if (holder != null) {
				break;
			}
			
			Thread.sleep(100);
			
			tries++;
		} while(tries <= timeout);

		return holder;
	}

	private void sendAuthenticateRequest(Session session) {
		Request request = new Request();
		request.setCommand(Commands.AUTHENTICATE);
		
		try {
			request.sign(configuration.getWebSocketKey());
		}
		catch (Exception ex) {
			log.error("Failed to sign authenticate message", ex);
			return;
		}

		String data = null;
		try {
			ObjectMapper mapper = new ObjectMapper();

			data = mapper.writeValueAsString(request);
		}
		catch (JsonProcessingException ex) {
			log.error("Cannot serialize authenticate request", ex);
			return;
		}

		TextMessage message = new TextMessage(data);
		try {
			requests.put(request.getTransactionUuid(), new RequestHolder(request));

			session.getSession().sendMessage(message);
		}
		catch (IOException ex) {
			log.error("Failed to send Authenticate request", ex);
		}
	}

	private void invalidateRequest(String transactionUuid, String message) {
		ADResponse response = new ADResponse();
		response.setStatus(ADStatus.TECHNICAL_ERROR);
		response.setMessage(message);

		ResponseHolder holder = new ResponseHolder(response);

		responses.put(transactionUuid, holder);
	}

	private void handleResponse(Response message) {
		ADResponse response = new ADResponse();
		response.setStatus(("true".equals(message.getStatus())) ? ADStatus.OK : ADStatus.FAILURE);
		response.setMessage(message.getMessage());

		ResponseHolder holder = new ResponseHolder(response);
		
		responses.put(message.getTransactionUuid(), holder);

		switch (message.getCommand()) {
			case Commands.SET_PASSWORD:
				log.info("Set password for " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.CREATE_ACCOUNT:
				log.info("Create account for " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.ASSOCIATE_ACCOUNT:
				log.info("associate account for " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.UPDATE_LICENSE:
				log.info("update license for " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.SET_EXPIRE:
				log.info("set expire for " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.DELETE_ACCOUNT:
				log.info("Delete account " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.DISABLE_ACCOUNT:
				log.info("Disable account " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.EMPLOYEE_SIGNATURE:
				log.info("Employee signature " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.AD_GROUPS_SYNC:
				log.info("AD groups sync " + message.getTarget() + ": " + message.getStatus());
				break;
			case Commands.UNLOCK_ACCOUNT:
				log.info("Unlock account " + message.getTarget() + ": " + message.getStatus());
				break;
			default:
				log.error("Unknown response command: " + message.getCommand());
				break;
		}
	}
	
	private void handleAuthenticateResponse(Session session, Response message, Request inResponseTo) {
		if (!message.verify(configuration.getWebSocketKey())) {
			log.error("Got invalid hmac on Authenticate response: " + message + " - sessionID = " + session.getId());

			// we keep the connection open on purpose, otherwise it will just reconnect immediately, spamming us
			return;
		}

		if ("true".equals(message.getStatus())) {
			session.setAuthenticated(true);

			log.info("Authenticated connection from client (version " + message.getClientVersion() + ") - sessionID = " + session.getId());
		}
		else {
			log.warn("Client refused to authenticate (version " + message.getClientVersion() + ") - sessionID = " + session.getId());
		}
	}

	private void retryRequest(Request request) {
		WebsocketQueue queue = new WebsocketQueue();
		queue.setCommand(request.getCommand());
		queue.setPayload(request.getPayload());
		queue.setTarget(request.getTarget());
		queue.setTts(LocalDateTime.now().plusMinutes(5));
		
		websocketQueueService.save(queue);
	}

	private Session getSession() {
		List<Session> matchingSessions = sessions.stream()
				.filter(s -> s.isAuthenticated())
				.collect(Collectors.toList());
		
		if (matchingSessions.size() == 0) {
			return null;
		}
		
		return matchingSessions.get(random.nextInt(matchingSessions.size()));
	}
}