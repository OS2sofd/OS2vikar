package dk.digitalidentity.os2vikar.datatables.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "view_global_roles")
public class GlobalRoleDatatable {
    @Id
    @Column
    private long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String itSystem;

    @Column
    private boolean checked;
}