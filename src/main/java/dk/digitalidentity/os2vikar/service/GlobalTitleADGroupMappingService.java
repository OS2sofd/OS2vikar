package dk.digitalidentity.os2vikar.service;

import dk.digitalidentity.os2vikar.dao.GlobalTitleADGroupMappingDao;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitleADGroupMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalTitleADGroupMappingService {

	@Autowired
	private GlobalTitleADGroupMappingDao globalTitleADGroupMappingDao;

	public List<GlobalTitleADGroupMapping> getAll() {
		return globalTitleADGroupMappingDao.findAll();
	}
}
