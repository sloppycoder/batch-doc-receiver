package org.vino9.eipp.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
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
import org.apache.camel.component.jpa.Consumed;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@NamedQuery(name = "Presentment.findPendingItems", query = " from Presentment WHERE status = 0")
@JacksonXmlRootElement(namespace = "urn:eipp:presentment", localName = "presentment")
public class Presentment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;

    @JacksonXmlProperty(namespace = "urn:eipp:presentment", localName = "Name")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @JsonIgnore
    private String path;

    @Column(nullable = false)
    @JsonIgnore
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    @ToString.Exclude
    @JsonIgnore
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    @ToString.Exclude
    @JsonIgnore
    private LocalDateTime updatedAt;


    @Column(nullable = false)
    @ColumnDefault("0")
    @JsonIgnore
    private int attempts;

    @Consumed
    public void mark() {
        this.setStatus(Status.IN_PROGRESS);
    }

    public enum Status {PENDING, IN_PROGRESS, PROCESSED, REJECTED, SKIPPED}
}

