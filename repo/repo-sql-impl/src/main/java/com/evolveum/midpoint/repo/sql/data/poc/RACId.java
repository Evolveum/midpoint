package com.evolveum.midpoint.repo.sql.data.poc;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RACId implements Serializable {

    private String ownerOid;
    private Short id;

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RACId racId = (RACId) o;

        if (id != null ? !id.equals(racId.id) : racId.id != null) return false;
        if (ownerOid != null ? !ownerOid.equals(racId.ownerOid) : racId.ownerOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
