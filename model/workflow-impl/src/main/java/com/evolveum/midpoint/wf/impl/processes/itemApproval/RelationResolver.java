package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectReferenceType;

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
