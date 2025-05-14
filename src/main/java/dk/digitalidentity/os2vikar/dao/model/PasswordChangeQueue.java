package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import dk.digitalidentity.os2vikar.dao.model.enums.ReplicationStatus;

@Entity(name = "password_change_queue")
@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String samaccountName;

    @Column
    private String password;

    @Column
    @CreationTimestamp
    private LocalDateTime tts;

    @Column
    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    @Column
    private String message;

    public PasswordChangeQueue(String samaccountName, String newPassword) {
    	this.password = newPassword;
    	this.samaccountName = samaccountName;
    	this.status = ReplicationStatus.WAITING_FOR_REPLICATION;
    }
}
