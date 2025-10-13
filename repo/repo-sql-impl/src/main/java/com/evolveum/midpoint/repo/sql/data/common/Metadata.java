/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;

/**
 * @author lazyman
 */
public interface Metadata<T extends ObjectReference> {

    XMLGregorianCalendar getCreateTimestamp();

    void setCreateTimestamp(XMLGregorianCalendar calendar);

    RSimpleEmbeddedReference getCreatorRef();

    void setCreatorRef(RSimpleEmbeddedReference ref);

    Set<T> getCreateApproverRef();

    void setCreateApproverRef(Set<T> set);

    String getCreateChannel();

    void setCreateChannel(String channel);

    XMLGregorianCalendar getModifyTimestamp();

    void setModifyTimestamp(XMLGregorianCalendar calendar);

    RSimpleEmbeddedReference getModifierRef();

    void setModifierRef(RSimpleEmbeddedReference ref);

    Set<T> getModifyApproverRef();

    void setModifyApproverRef(Set<T> set);

    String getModifyChannel();

    void setModifyChannel(String channel);
}
