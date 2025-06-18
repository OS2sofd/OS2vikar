package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.dao.StatisticDao;
import dk.digitalidentity.os2vikar.dao.model.Statistic;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StatisticService {

	@Autowired
	private StatisticDao statisticDao;
	
	@Autowired
	private SubstituteService substituteService;
	
	@Autowired
	private StatisticService self;

	// migrate existing workplaces into statistics
	// can be deleted once everyone has been updated to at least july 2025 version
	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		self.migrate();
	}
	
	@Transactional
	public void migrate() {
		List<Statistic> existing = statisticDao.findAll();
		if (existing.size() == 0) {
			List<Substitute> substitutes = substituteService.getAll();

			for (Substitute substitute : substitutes) {
				for (Workplace workplace : substitute.getWorkplaces()) {
					save(workplace);
				}
			}
		}
	}
	
	public void save(Workplace workplace) {
		if (workplace.getId() == 0) {
			log.error("Called with workplace without ID!");
			return;
		}

		Statistic statistic = statisticDao.findByWorkplaceId(workplace.getId());
		if (statistic == null) {
			statistic = new Statistic();
			statistic.setOrgunitName(workplace.getOrgUnit().getName());
			statistic.setOrgunitUuid(workplace.getOrgUnit().getUuid());
			statistic.setStartDate(workplace.getStartDate());
			statistic.setSubstituteName(workplace.getSubstitute().getName());
			statistic.setSubstituteUserId(workplace.getSubstitute().getUsername());
			statistic.setWorkplaceId(workplace.getId());
		}
		
		statistic.setStopDate(workplace.getStopDate());
		
		// cancelling is done by setting the stop-date 1 day in the past, which MIGHT mean that it was never created in the first place
		if (workplace.getStopDate().isBefore(workplace.getStartDate())) {
			statisticDao.delete(statistic);
		}
		else {
			statisticDao.save(statistic);
		}
	}

	public List<Statistic> getAll() {
		return statisticDao.findAll();
	}

	@Transactional
	public void cleanUp() {
		log.info("Started statistic clean up task");
		
		LocalDate cutoff = LocalDate.now().minusMonths(13);
		long deletedCount = statisticDao.deleteByStartDateBefore(cutoff);

		log.info("Finished statistic clean up task. " + deletedCount + " statistics was deleted.");
	}
}
