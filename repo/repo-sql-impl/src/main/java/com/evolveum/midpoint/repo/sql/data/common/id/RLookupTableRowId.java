package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class RLookupTableRowId implements Serializable {

    private String ownerOid;
    private String key;

    public String getOwnerOid() {
        return ownerOid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RLookupTableRowId that = (RLookupTableRowId) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
