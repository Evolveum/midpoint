/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface RelationResolver {
    // Must return parent-less values
    List<ObjectReferenceType> getApprovers(Collection<QName> relations);
}
