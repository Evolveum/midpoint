package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Set;

/**
 * @author lazyman
 */
public interface Metadata<T extends ObjectReference> {

    XMLGregorianCalendar getCreateTimestamp();

    void setCreateTimestamp(XMLGregorianCalendar calendar);

    REmbeddedReference getCreatorRef();

    void setCreatorRef(REmbeddedReference ref);

    Set<T> getCreateApproverRef();

    void setCreateApproverRef(Set<T> set);

    String getCreateChannel();

    void setCreateChannel(String channel);

    XMLGregorianCalendar getModifyTimestamp();

    void setModifyTimestamp(XMLGregorianCalendar calendar);

    REmbeddedReference getModifierRef();

    void setModifierRef(REmbeddedReference ref);

    Set<T> getModifyApproverRef();

    void setModifyApproverRef(Set<T> set);

    String getModifyChannel();

    void setModifyChannel(String channel);
}
