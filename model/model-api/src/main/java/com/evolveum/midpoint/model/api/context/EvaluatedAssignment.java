package com.evolveum.midpoint.model.api.context;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public interface EvaluatedAssignment<F extends FocusType> extends DebugDumpable {

	AssignmentType getAssignmentType();

	Collection<Authorization> getAuthorizations();
	
	DeltaSetTriple<? extends EvaluatedAbstractRole> getRoles();

	PrismObject<?> getTarget();

	boolean isValid();

}