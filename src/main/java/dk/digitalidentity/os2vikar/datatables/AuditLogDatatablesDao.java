package dk.digitalidentity.os2vikar.datatables;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import dk.digitalidentity.os2vikar.datatables.model.AuditLogDatatable;

public interface AuditLogDatatablesDao extends DataTablesRepository<AuditLogDatatable, Long> {

}
