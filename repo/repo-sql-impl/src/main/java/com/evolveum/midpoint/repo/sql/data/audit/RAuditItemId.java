package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;

public class RAuditItemId implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private Long recordId;
	private String changedItemPath;
    
    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
    
    public String getChangedItemPath() {
		return changedItemPath;
	}
    
    public void setChangedItemPath(String changedItemPath) {
		this.changedItemPath = changedItemPath;
	}

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAuditItemId that = (RAuditItemId) o;

        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;
        if (changedItemPath != null ? !changedItemPath.equals(that.changedItemPath) : that.changedItemPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (changedItemPath != null ? changedItemPath.hashCode() : 0);
        return result;
    }

}
