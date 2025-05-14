package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.Setting;

public interface SettingsDao extends JpaRepository<Setting, Long>{
	Setting findByKey(String key);
}
