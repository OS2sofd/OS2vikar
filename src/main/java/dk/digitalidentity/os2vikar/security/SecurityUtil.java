package dk.digitalidentity.os2vikar.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dk.digitalidentity.os2vikar.config.RoleConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority.Constraint;
import dk.digitalidentity.samlmodule.model.TokenUser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityUtil {
	private static final String SYSTEM_USERID = "system";

	public static String getCvr() {
		String cvr = null;

		if (isUserLoggedIn()) {
			cvr = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails()).getCvr();
		}
		
		return cvr;
	}

	public static String getUser() {
		String uuid = null;
		String name = null;

		if (isUserLoggedIn()) {
			String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			String[] split = principal.split(",");
			
			name = split[2].replace("CN=", "");
			uuid = split[split.length-1].replace("Serial=", "");
			
			if (uuid.length() != 36) {
				log.warn("UUID for logged in user is invalid: " + uuid);
			}
		}

		return name + " (" + uuid + ")";
	}
	
	@SuppressWarnings("unchecked")
	public static Set<String> getOrgUnitUuidsFromConstraint(String identifier) {
		Set<String> orgUnitUuids = new HashSet<>();

		if (isUserLoggedIn()) {
			for (SamlGrantedAuthority authority : (List<SamlGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				
				if (Objects.equals(authority.getAuthority(), RoleConstants.SUBSTITUTE_ADMIN)) {
					Constraint constraint = authority.getConstraints().stream().filter(c -> Objects.equals(c.getConstraintType(), identifier)).findFirst().orElse(null);
					if (constraint != null) {
						orgUnitUuids.addAll(Arrays.asList(constraint.getConstraintValue().split(",")));
					}
				}
			}
		}

		return orgUnitUuids;
	}

	@SuppressWarnings("unchecked")
	public static Set<String> getITSystemConstraint(String identifier) {
		Set<String> itSystems = new HashSet<>();

		if (isUserLoggedIn()) {
			for (SamlGrantedAuthority authority : (List<SamlGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				if (Objects.equals(authority.getAuthority(), RoleConstants.SUBSTITUTE_ADMIN)) {
					Constraint constraint = authority.getConstraints().stream().filter(c -> Objects.equals(c.getConstraintType(), identifier)).findFirst().orElse(null);
					if (constraint != null) {
						itSystems.add(constraint.getConstraintValue());
					}
				}
			}
		}

		return itSystems;
	}
	
	public static boolean isUserLoggedIn() {
		return SecurityContextHolder.getContext().getAuthentication() != null
				&& SecurityContextHolder.getContext().getAuthentication().getDetails() != null
				&& SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof TokenUser;
	}
	
	
	@SuppressWarnings("unchecked")
	public static boolean hasRole(String role) {
        boolean hasRole = false;
        if (isUserLoggedIn()) {
            for (SamlGrantedAuthority authority : (List<SamlGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
                if (Objects.equals(authority.getAuthority(), role)) {
                    hasRole = true;
                    break;
                }
            }
        }

        return hasRole;
    }

	public static String getUserIP() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "0.0.0.0";
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        return request.getRemoteAddr();
    }
	
	public static void loginSystemAccount() {
		List<SamlGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority(RoleConstants.ROLE_API, null, null));
		TokenUser tokenUser = TokenUser.builder()
				.cvr("N/A")
				.authorities(authorities)
				.build();

		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(SYSTEM_USERID, "N/A", tokenUser.getAuthorities());
		token.setDetails(tokenUser);
		SecurityContextHolder.getContext().setAuthentication(token);
	}
}
