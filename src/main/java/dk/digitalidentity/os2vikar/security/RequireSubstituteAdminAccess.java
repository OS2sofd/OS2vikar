package dk.digitalidentity.os2vikar.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.access.prepost.PreAuthorize;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_SUBSTITUTE_ADMIN')")
public @interface RequireSubstituteAdminAccess {
	
}