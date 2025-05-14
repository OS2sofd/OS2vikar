package dk.digitalidentity.os2vikar.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PasswordService {
	private static List<String> lines = new ArrayList<>();
	private static final SecureRandom random = new SecureRandom();
	private static final char[] lowercase_1 = "aeuy".toCharArray();
	private static final char[] lowercase_2 = "bcdfghkmnpqrstvwxz".toCharArray();
	private static final char[] uppercase = "BCDFGHJKMNPRSTVWXZ".toCharArray();
	private static final char[] numbers = "23456789".toCharArray();
	private static final char[] symbols = "$?!@#%&".toCharArray();

	@Autowired
	private OS2VikarConfiguration configuration;
	
	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		InputStreamReader isr = null;
		BufferedReader reader = null;

		try {
			isr = new InputStreamReader(ResourceUtils.getURL("classpath:kodeord.dat").openStream());
			reader = new BufferedReader(isr);

			lines = reader.lines().collect(Collectors.toList());
		}
		catch (Exception ex) {
			log.error("Failed to load passwords", ex);
		}
		finally {
			try {
				if (isr != null) {
					isr.close();
				}
				
				if (reader != null) {
					reader.close();
				}
			}
			catch (Exception ex) {
				; // ignore*
			}
		}

		log.info("Read " + lines.size() + " passwords from kodeord.dat");
	}
	
	public String generatePassword() {
		if (configuration.isPasswordFromWordlist() && lines.size() > 0) {
			try {
				return generatePasswordFromWordlist(configuration.isPasswordFromWordlistLowerCase());
			}
			catch (Exception ex) {
				log.warn("Failed to generate password from file, fallback to random password", ex);
				return generatePasswordFromCharacters();
			}
		}
		
		return generatePasswordFromCharacters();
	}

	public String generatePasswordFromCharacters() {
		StringBuilder password = new StringBuilder();

		password.append(uppercase[random.nextInt(uppercase.length)]);
		password.append(lowercase_1[random.nextInt(lowercase_1.length)]);
		password.append(lowercase_2[random.nextInt(lowercase_2.length)]);
		password.append(lowercase_1[random.nextInt(lowercase_1.length)]);
		password.append(lowercase_2[random.nextInt(lowercase_2.length)]);
		
		// allow for larger passwords if needed
		if (configuration.getPasswordChangeSize() > 8) {
			for (int i = 8; i < configuration.getPasswordChangeSize(); i++) {
				password.append(lowercase_2[random.nextInt(lowercase_2.length)]);				
			}
		}

		password.append(symbols[random.nextInt(symbols.length)]);
		password.append(numbers[random.nextInt(numbers.length)]);
		password.append(numbers[random.nextInt(numbers.length)]);

		return password.toString();
	}

	public String generatePasswordFromWordlist(boolean lowerCase) throws IOException {
		StringBuilder password = new StringBuilder();
		long minimumSize = 20;

		if (configuration.getPasswordChangeSize() > minimumSize) {
			minimumSize = configuration.getPasswordChangeSize();
		}

		// generate password		
		while (password.length() < minimumSize) {
			password.append(lines.get(random.nextInt(lines.size())));
		}
		
		return (lowerCase) ? password.toString().toLowerCase() : password.toString();
	}
}
