package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.ADGroupDao;
import dk.digitalidentity.os2vikar.dao.model.ADGroup;

@Service
public class ADGroupService {

	@Autowired
	private ADGroupDao adGroupDao;
	
	public List<ADGroup> getAll() {
		return adGroupDao.findAll();
	}
	
	public ADGroup getByObjectGuid(String objectGuid) {
		return adGroupDao.findByObjectGuid(objectGuid);
	}
	
	public ADGroup save(ADGroup adGroup) {
		return adGroupDao.save(adGroup);
	}
	
	public void deleteAll(List<ADGroup> toDelete) {
		adGroupDao.deleteAll(toDelete);
	}
}
