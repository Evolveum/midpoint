package com.evolveum.midpoint.repo.sql.data.common.id;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

/**
 * @author katka
 * @author lazyman
 */
public class RTriggerId implements Serializable {

    private Long ownerId;
    private String ownerOid;
    private String handlerUri;
    private XMLGregorianCalendar timestamp;

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public String getHandlerUri() {
        return handlerUri;
    }

    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTriggerId that = (RTriggerId) o;

        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

}
