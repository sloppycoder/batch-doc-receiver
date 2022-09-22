package org.vino9.lib.batchdocreceiver.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@ToString
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

    public void markProcessed() {
        this.setStatus(Status.PROCESSED);
    }
}

enum Status {PENDING, IN_PROGRESS, PROCESSED, REJECTED, SKIPPED}