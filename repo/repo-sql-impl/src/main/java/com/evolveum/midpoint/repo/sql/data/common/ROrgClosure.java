package com.evolveum.midpoint.repo.sql.data.common;

import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_org_closure")
public class ROrgClosure implements Serializable {

    //todo how about FK keys?
    private String ancestor;
    private String descendant;
    private int depth;

    @Id
    @Index(name = "iAncestor")
    public String getAncestor() {
        return ancestor;
    }

    @Id
    @Index(name = "iDescendant")
    public String getDescendant() {
        return descendant;
    }

    public int getDepth() {
        return depth;
    }

    public void setAncestor(String ancestor) {
        this.ancestor = ancestor;
    }

    public void setDescendant(String descendant) {
        this.descendant = descendant;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROrgClosure that = (ROrgClosure) o;

        if (depth != that.depth) return false;
        if (ancestor != null ? !ancestor.equals(that.ancestor) : that.ancestor != null) return false;
        if (descendant != null ? !descendant.equals(that.descendant) : that.descendant != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ancestor != null ? ancestor.hashCode() : 0;
        result = 31 * result + (descendant != null ? descendant.hashCode() : 0);
        result = 31 * result + depth;
        return result;
    }
}
