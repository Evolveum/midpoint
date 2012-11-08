package com.evolveum.midpoint.provisioning.consistency.api;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;

public abstract class ErrorHandler {
	
	public static enum FailedOperation{
		ADD, DELETE, MODIFY, GET;
	}
	
	public abstract <T extends ResourceObjectShadowType> T handleError(T shadow, FailedOperation op, Exception ex, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException;

	
	protected <T extends ResourceObjectShadowType> Collection<ItemDelta> createAttemptModification(T shadow,
			Collection<ItemDelta> modifications) {

		if (modifications == null) {
			modifications = new ArrayList<ItemDelta>();
		}
		PropertyDelta attemptDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
				ResourceObjectShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
		modifications.add(attemptDelta);
		return modifications;
	}

	protected Integer getAttemptNumber(ResourceObjectShadowType shadow) {
		Integer attemptNumber = (shadow.getAttemptNumber() == null ? 0 : shadow.getAttemptNumber()+1);
		return attemptNumber;
	}
}
