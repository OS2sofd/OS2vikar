package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.Substitute;

public interface SubstituteDao extends JpaRepository<Substitute, Long>{
	Substitute findByid(long id);
	Substitute findByCpr(String cpr);
	List<Substitute> findByLastUpdatedAfter(LocalDateTime tts);
	List<Substitute> findByAuthorizationCodeCheckedFalse();
}
