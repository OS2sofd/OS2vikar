package dk.digitalidentity.os2vikar.service;

import dk.digitalidentity.os2vikar.service.dto.DeleteAfterPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.dao.SettingsDao;
import dk.digitalidentity.os2vikar.dao.model.Setting;

@Service
public class SettingsService {
	private static final String AD_USERNAME_COUNTER = "ADUsernameCounter";
	public static final String DELETE_ACCOUNT_IN_AD = "DeleteAccountInAD";
	public static final String DELETE_SUBSTITUTE_AFTER = "DeleteSubstituteAfter";

	@Autowired
	private SettingsDao settingsDao;
	
	public int getADUsernameCount() {
		Setting setting = settingsDao.findByKey(AD_USERNAME_COUNTER);
		if (setting == null || !StringUtils.hasLength(setting.getValue())) {
			return 1000;
		}
		
		int count = 1000;
		try {
			count = Integer.parseInt(setting.getValue());
		}
		catch (Exception ex) {
			; // ignore
		}

		return count;
	}
	
	public void setADUsernameCount(int count) {
		Setting setting = settingsDao.findByKey(AD_USERNAME_COUNTER);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(AD_USERNAME_COUNTER);
		}
		
		setting.setValue(Integer.toString(count));
		settingsDao.save(setting);
	}

	public boolean getBooleanWithDefault(String key, boolean defaultValue) {
		Setting setting = settingsDao.findByKey(key);
		if (setting != null) {
			return Boolean.parseBoolean(setting.getValue());
		}

		return defaultValue;
	}

	public String getKeyWithDefault(String key, String defaultValue) {
		Setting setting = settingsDao.findByKey(key);
		if (setting != null) {
			return setting.getValue();
		}

		return defaultValue;
	}

	public DeleteAfterPeriod getDeleteSubstituteAfter() {
		Setting setting = settingsDao.findByKey(DELETE_SUBSTITUTE_AFTER);
		if (setting != null) {
			return DeleteAfterPeriod.valueOf(setting.getValue());
		}

		return DeleteAfterPeriod.FIVE_YEARS;
	}

	public void setBooleanValueForKey(String key, boolean enabled) {
		Setting setting = settingsDao.findByKey(key);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(key);
		}

		setting.setValue(Boolean.toString(enabled));
		settingsDao.save(setting);
	}

	public void setValueForKey(String key, String value) {
		Setting setting = settingsDao.findByKey(key);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(key);
		}

		setting.setValue(value);
		settingsDao.save(setting);
	}

	public void setDeleteSubstituteAfter(DeleteAfterPeriod deleteSubstituteAfter) {
		Setting setting = settingsDao.findByKey(DELETE_SUBSTITUTE_AFTER);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(DELETE_SUBSTITUTE_AFTER);
		}

		setting.setValue(deleteSubstituteAfter.toString());
		settingsDao.save(setting);
	}
}
