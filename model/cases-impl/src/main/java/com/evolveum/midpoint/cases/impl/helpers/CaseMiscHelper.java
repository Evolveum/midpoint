/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.exception.SystemException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * TODO Deduplicate with MiscHelper in workflow-impl!
 *
 * TODO clean up this mess
 */
@Component
public class CaseMiscHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CaseMiscHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private Clock clock;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public PrismObject<? extends FocusType> getRequesterIfExists(CaseType aCase, OperationResult result) {
        //noinspection unchecked
        return (PrismObject<? extends FocusType>) resolveAndStoreObjectReference(
                aCase != null ? aCase.getRequestorRef() : null,
                result);
    }

    private TypedValue<PrismObject<?>> resolveTypedObjectReference(ObjectReferenceType ref, OperationResult result) {
        PrismObject<?> resolvedObject = resolveObjectReference(ref, false, result);
        if (resolvedObject == null) {
            PrismObjectDefinition<ObjectType> def =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
            return new TypedValue<>(null, def);
        } else {
            return new TypedValue<>(resolvedObject);
        }
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

    private PrismObject<?> resolveAndStoreObjectReference(ObjectReferenceType ref, OperationResult result) {
        return resolveObjectReference(ref, true, result);
    }

    private PrismObject<?> resolveObjectReference(ObjectReferenceType ref, boolean storeBack, OperationResult result) {
        if (ref == null) {
            return null;
        }
        if (ref.asReferenceValue().getObject() != null) {
            return ref.asReferenceValue().getObject();
        }
        try {
            PrismObject<?> object = repositoryService.getObject(
                    prismContext.getSchemaRegistry().getCompileTimeClass(ref.getType()),
                    ref.getOid(), null, result);
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

    public void closeCaseInRepository(CaseType aCase, OperationResult result) throws ObjectNotFoundException {
        try {
            List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
                    .item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
                    .item(CaseType.F_CLOSE_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                    .asItemDeltas();
            repositoryService.modifyObject(CaseType.class, aCase.getOid(), modifications, result);
            LOGGER.debug("Marked case {} as closed", aCase);
        } catch (SchemaException | ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception while closing a case: " + e.getMessage(), e);
        }
    }

    public VariablesMap getDefaultVariables(CaseType aCase, ApprovalContextType wfContext, String requestChannel,
            OperationResult result) throws SchemaException {

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_REQUESTER, resolveTypedObjectReference(aCase.getRequestorRef(), result));
        variables.put(ExpressionConstants.VAR_OBJECT, resolveTypedObjectReference(aCase.getObjectRef(), result));
        // might be null
        variables.put(ExpressionConstants.VAR_TARGET, resolveTypedObjectReference(aCase.getTargetRef(), result));
        variables.put(ExpressionConstants.VAR_CHANNEL, requestChannel, String.class);
        variables.put(ExpressionConstants.VAR_THE_CASE, aCase, List.class);

        // FIXME these are specific for approvals
        variables.put(ExpressionConstants.VAR_OBJECT_DELTA, getFocusPrimaryDelta(wfContext), ObjectDelta.class);
        variables.put(ExpressionConstants.VAR_APPROVAL_CONTEXT, wfContext, ApprovalContextType.class);

        // todo other variables?

        return variables;
    }

    private ObjectDelta<?> getFocusPrimaryDelta(ApprovalContextType actx) throws SchemaException {
        ObjectDeltaType objectDeltaType = getFocusPrimaryObjectDeltaType(actx);
        return objectDeltaType != null ? DeltaConvertor.createObjectDelta(objectDeltaType, prismContext) : null;
    }

    // mayBeNull=false means that the corresponding variable must be present (not that focus must be non-null)
    // TODO: review/correct this!
    private ObjectDeltaType getFocusPrimaryObjectDeltaType(ApprovalContextType actx) {
        ObjectTreeDeltasType deltas = getObjectTreeDeltaType(actx);
        return deltas != null ? deltas.getFocusPrimaryDelta() : null;
    }

    private ObjectTreeDeltasType getObjectTreeDeltaType(ApprovalContextType actx) {
        return actx != null ? actx.getDeltasToApprove() : null;
    }
}
