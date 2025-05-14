package dk.digitalidentity.os2vikar.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.config.RoleConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;

@Component
public class RolePostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private OS2VikarConfiguration config;
	
	@Override
	public void process(TokenUser tokenUser) {
		String cvr = tokenUser.getCvr();
		if (!cvr.equals(config.getCvr())) {
			throw new UsernameNotFoundException("Ukendt CVR: " + cvr);
		}

		List<SamlGrantedAuthority> newAuthorities = new ArrayList<>();

		String substituteAdminRole = "ROLE_" + config.getSubstituteAdminRoleId();
		String systemAdminRole = "ROLE_" + config.getSystemAdminRoleId();

		for (Iterator<? extends SamlGrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			SamlGrantedAuthority grantedAuthority = iterator.next();

			if (substituteAdminRole.equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.SUBSTITUTE_ADMIN, grantedAuthority.getConstraints(), grantedAuthority.getScope()));
			}
			else if (systemAdminRole.equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.SYSTEM_ADMIN, grantedAuthority.getConstraints(), grantedAuthority.getScope()));
			}
		}

		if (config.isDev()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.SUBSTITUTE_ADMIN, new ArrayList<>(), null));
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.SYSTEM_ADMIN, new ArrayList<>(), null));
		}

		if (newAuthorities.isEmpty()) {
			throw new UsernameNotFoundException("Ingen tildelte roller");
		}

		tokenUser.setAuthorities(newAuthorities);
	}
}
