package dk.digitalidentity.os2vikar.config;

public class Commands {
	public static final String AUTHENTICATE = "AUTHENTICATE";
	public static final String SET_PASSWORD = "SET_PASSWORD";
	public static final String CREATE_ACCOUNT = "CREATE_ACCOUNT";
	public static final String ASSOCIATE_ACCOUNT = "ASSOCIATE_ACCOUNT";
	public static final String UPDATE_LICENSE = "UPDATE_LICENSE";
	public static final String SET_EXPIRE = "SET_EXPIRE";
	public static final String DELETE_ACCOUNT = "DELETE_ACCOUNT";
	public static final String DISABLE_ACCOUNT = "DISABLE_ACCOUNT";
	public static final String EMPLOYEE_SIGNATURE = "EMPLOYEE_SIGNATURE";
	public static final String AD_GROUPS_SYNC = "AD_GROUPS_SYNC";
	public static final String UNLOCK_ACCOUNT = "UNLOCK_ACCOUNT";
	public static final String SET_AUTHORIZATION_CODE = "SET_AUTHORIZATION_CODE";
	
	private Commands() {
		throw new IllegalStateException("Utility class");
	}
}
