package org.vino9.lib.batchdocreceiver.data;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@NamedQuery(name = "Document.findPendingDocuments", query = " from Document WHERE status = 0")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    @ToString.Exclude
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    @ToString.Exclude
    private LocalDateTime updatedAt;

    public void mark() {
        this.setStatus(Status.IN_PROGRESS);
    }

    public enum Status {PENDING, IN_PROGRESS, PROCESSED, REJECTED, SKIPPED}
}
