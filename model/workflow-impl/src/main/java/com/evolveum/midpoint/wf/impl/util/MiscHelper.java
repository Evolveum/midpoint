/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
@Component
public class MiscHelper {

    private static final Trace LOGGER = TraceManager.getTrace(MiscHelper.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;
	@Autowired private ModelInteractionService modelInteractionService;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	public PrismObject<UserType> getRequesterIfExists(CaseType aCase, OperationResult result) {
		if (aCase == null || aCase.getRequestorRef() == null) {
			return null;
		}
		ObjectReferenceType requesterRef = aCase.getRequestorRef();
		//noinspection unchecked
		return (PrismObject<UserType>) resolveAndStoreObjectReference(requesterRef, result);
	}

	public TypedValue<PrismObject> resolveTypedObjectReference(ObjectReferenceType ref, OperationResult result) {
		PrismObject resolvedObject = resolveObjectReference(ref, false, result);
		if (resolvedObject == null) {
			PrismObjectDefinition<ObjectType> def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
			return new TypedValue<>(null, def);
		} else {
			return new TypedValue<>(resolvedObject);
		}
	}

	public String getCompleteStageInfo(CaseType aCase) {
		return ApprovalContextUtil.getCompleteStageInfo(aCase);
	}

	public String getAnswerNice(CaseType aCase) {
		if (CaseTypeUtil.isApprovalCase(aCase)) {
			return ApprovalUtils.makeNiceFromUri(getOutcome(aCase));
		} else {
			return getOutcome(aCase);
		}
	}

	private String getOutcome(CaseType aCase) {
		return aCase.getApprovalContext() != null ? aCase.getOutcome() : null;
	}

	public List<ObjectReferenceType> getAssigneesAndDeputies(CaseWorkItemType workItem, Task task, OperationResult result)
			throws SchemaException {
		List<ObjectReferenceType> rv = new ArrayList<>();
		rv.addAll(workItem.getAssigneeRef());
		rv.addAll(modelInteractionService.getDeputyAssignees(workItem, task, result));
		return rv;
	}

	public List<CaseType> getSubcases(CaseType rootCase, OperationResult result) throws SchemaException {
		return getSubcases(rootCase.getOid(), result);
	}

	public List<CaseType> getSubcases(String oid, OperationResult result) throws SchemaException {
		return repositoryService.searchObjects(CaseType.class,
				prismContext.queryFor(CaseType.class)
					.item(CaseType.F_PARENT_REF).ref(oid)
					.build(),
				null,
				result)
				.stream()
					.map(o -> o.asObjectable())
					.collect(Collectors.toList());
	}

	public ModelContext getModelContext(CaseType aCase, Task task, OperationResult result) throws SchemaException,
			ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		LensContextType modelContextType = aCase.getModelContext();
		if (modelContextType == null) {
			return null;
		}
		return LensContext.fromLensContextType(modelContextType, prismContext, provisioningService, task, result);
	}

	public PrismObject resolveObjectReference(ObjectReferenceType ref, OperationResult result) {
		return resolveObjectReference(ref, false, result);
	}

	public PrismObject resolveAndStoreObjectReference(ObjectReferenceType ref, OperationResult result) {
		return resolveObjectReference(ref, true, result);
	}

	private PrismObject resolveObjectReference(ObjectReferenceType ref, boolean storeBack, OperationResult result) {
		if (ref == null) {
			return null;
		}
		if (ref.asReferenceValue().getObject() != null) {
			return ref.asReferenceValue().getObject();
		}
		try {
			PrismObject object = repositoryService.getObject((Class) prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()), ref.getOid(), null, result);
			if (storeBack) {
				ref.asReferenceValue().setObject(object);
			}
			return object;
		} catch (ObjectNotFoundException e) {
			// there should be a note in result by now
			LoggingUtils.logException(LOGGER, "Couldn't get reference {} details because it couldn't be found", e, ref);
			return null;
		} catch (SchemaException e) {
			// there should be a note in result by now
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get reference {} details due to schema exception", e, ref);
			return null;
		}
	}

	public ObjectReferenceType resolveObjectReferenceName(ObjectReferenceType ref, OperationResult result) {
		if (ref == null || ref.getTargetName() != null) {
			return ref;
		}
		PrismObject<?> object;
		if (ref.asReferenceValue().getObject() != null) {
			object = ref.asReferenceValue().getObject();
		} else {
			object = resolveObjectReference(ref, result);
			if (object == null) {
				return ref;
			}
		}
		ref = ref.clone();
		ref.setTargetName(PolyString.toPolyStringType(object.getName()));
		return ref;
	}

}
