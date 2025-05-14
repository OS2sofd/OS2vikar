package dk.digitalidentity.os2vikar.config;

import dk.digitalidentity.os2vikar.config.modules.ConstraintITSystem;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.modules.Api;
import dk.digitalidentity.os2vikar.config.modules.Cpr;
import dk.digitalidentity.os2vikar.config.modules.Nexus;
import dk.digitalidentity.os2vikar.config.modules.O365;
import dk.digitalidentity.os2vikar.config.modules.Organisation;
import dk.digitalidentity.os2vikar.config.modules.PreCreateAD;
import dk.digitalidentity.os2vikar.config.modules.RoleCatalogConfiguration;
import dk.digitalidentity.os2vikar.config.modules.SofdConfiguration;
import dk.digitalidentity.os2vikar.config.modules.SyncADGroups;
import dk.digitalidentity.os2vikar.config.modules.WebSocketsConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "os2vikar")
public class OS2VikarConfiguration {
	private boolean dev = false;
	private String systemAdminRoleId;
	private String substituteAdminRoleId;
	private String constraintItSystemIdentifier = "http://os2vikar.dk/constraints/itsystem/1";
	private String constraintOrgUnitIdentifier = "http://sts.kombit.dk/constraints/orgenhed/1";
	
	private boolean scheduledJobsEnabled;
	private String cvr;
	private boolean doubleAccountsAllowed = true;
	private long passwordChangeSize = 8;
	private boolean employeeSignatureEnabled = false;
	private int defaultMaxDays = 10;
	private boolean enableAuthorizationCodes = false;
	
	// how many days after the workplace ends, do we disable the AD account
	private int disableDelay = 1;

	private boolean passwordChangeAllowed = false;
	private boolean passwordChangeAdminOnlyAllowed = false;
	private boolean passwordFromWordlist = false;
	private boolean passwordFromWordlistLowerCase = false;
	private boolean unlockAccountAllowed = false;
	private boolean deleteAccountAllowed = false;
	private boolean resetNexusUserAllowed = false;

	// how long do we keep closed workplaces in the UI
	private long substituteWorkplaceVisibilityTreshold = 3;
	
	private Cpr cpr = new Cpr();
	private Api api = new Api();
	private Organisation organisation = new Organisation();
	private RoleCatalogConfiguration rc = new RoleCatalogConfiguration();
	private SofdConfiguration sofd = new SofdConfiguration();
	private WebSocketsConfiguration websockets = new WebSocketsConfiguration();
	private PreCreateAD preCreateAD = new PreCreateAD();
	private Nexus nexus = new Nexus();
	private O365 o365 = new O365();
	private SyncADGroups syncADGroups = new SyncADGroups();
	private List<ConstraintITSystem> constraintITSystems = new ArrayList<>();
}