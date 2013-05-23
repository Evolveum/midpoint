package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

public class RTriggerId implements Serializable{

	   private String ownerOid;
	    private Long ownerId;

	    public Long getOwnerId() {
	        return ownerId;
	    }

	    public void setOwnerId(Long ownerId) {
	        this.ownerId = ownerId;
	    }

	    public String getOwnerOid() {
	        return ownerOid;
	    }

	    public void setOwnerOid(String ownerOid) {
	        this.ownerOid = ownerOid;
	    }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;

	        RTriggerId that = (RTriggerId) o;

	        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
	        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;

	        return true;
	    }

	    @Override
	    public int hashCode() {
	        int result = ownerOid != null ? ownerOid.hashCode() : 0;
	        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
	        return result;
	    }
	
}
