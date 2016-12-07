package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author pmederly
 */
@FunctionalInterface
public interface RelationResolver {
	List<ObjectReferenceType> getApprovers(List<QName> relations);
}
