/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author pmederly
 */
@FunctionalInterface
public interface RelationResolver {
    // Must return parent-less values
    List<ObjectReferenceType> getApprovers(Collection<QName> relations);
}
