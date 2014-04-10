package com.evolveum.midpoint.repo.sql.data.poc;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
public class RAR extends RAO {

    private Boolean requestable;
    private Set<RAA> assignments;

    public Boolean getRequestable() {
        return requestable;
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAA> getAssignments() {
        return assignments;
    }

    public void setAssignments(Set<RAA> assignments) {
        this.assignments = assignments;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }
}
