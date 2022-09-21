/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcherAware;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerAware;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Radovan Semancik
 *
 */
@ManagedConnector(type="ManualConnector", version="1.0.0")
public class ManualConnectorInstance extends AbstractManualConnectorInstance implements RepositoryAware,
        CaseEventDispatcherAware, TaskManagerAware {

    private static final String OPERATION_QUERY_CASE = ManualConnectorInstance.class.getName() + ".queryCase";

    private static final Trace LOGGER = TraceManager.getTrace(ManualConnectorInstance.class);

    private ManualConnectorConfiguration configuration;

    private RepositoryService repositoryService;
    private CaseEventDispatcher caseEventDispatcher;
    private TaskManager taskManager;

    private boolean connected = false;

    private static int randomDelayRange = 0;

    private static final String DEFAULT_OPERATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

    private static final Random RND = new Random();

    private Clock clock = new Clock();

    @ManagedConnectorConfiguration
    public ManualConnectorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ManualConnectorConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public void setDispatcher(CaseEventDispatcher dispatcher) {
        this.caseEventDispatcher = dispatcher;
    }

    @Override
    public CaseEventDispatcher getDispatcher() {
        return caseEventDispatcher;
    }

    @Override
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    protected String createTicketAdd(PrismObject<? extends ShadowType> object, Collection<Operation> additionalOperations,
            Task task, OperationResult result) throws SchemaException,
            ObjectAlreadyExistsException {
        LOGGER.debug("Creating case to add account\n{}", object.debugDump(1));
        ObjectDelta<? extends ShadowType> objectDelta = DeltaFactory.Object.createAddDelta(object);
        ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);
        String shadowName;
        if (object.getName() != null) {
            shadowName = object.getName().toString();
        } else {
            shadowName = getShadowIdentifier(ShadowUtil.getPrimaryIdentifiers(object));
        }
        PrismObject<CaseType> aCase = addCase("create", ShadowUtil.getResourceOid(object.asObjectable()),
                shadowName, object.asObjectable(), objectDeltaType, task, result);
        return aCase.getOid();
    }

    @Override
    protected String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
            Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
        LOGGER.debug("Creating case to modify account {}:\n{}", identifiers, DebugUtil.debugDump(changes, 1));
        if (InternalsConfig.isSanityChecks()) {
            if (MiscUtil.hasDuplicates(changes)) {
                throw new SchemaException("Duplicated changes: "+changes);
            }
        }
        Collection<ItemDelta> changeDeltas = changes.stream()
                .filter(change -> change != null)
                .map(change -> ((PropertyModificationOperation)change).getPropertyDelta())
                .collect(Collectors.toList());
        ObjectDelta<? extends ShadowType> objectDelta = getPrismContext().deltaFactory().object()
                .createModifyDelta("", changeDeltas, ShadowType.class);
        ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);
        objectDeltaType.setOid(shadow.getOid());
        String shadowName = shadow.getName().toString();
        PrismObject<CaseType> aCase =
                addCase("modify", resourceOid, shadowName,
                shadow.asObjectable(), objectDeltaType, task, result);
        return aCase.getOid();
    }

    @Override
    protected String createTicketDelete(ObjectClassComplexTypeDefinition objectClass, PrismObject<ShadowType> shadow,
            Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Task task, OperationResult result)
            throws SchemaException {
        LOGGER.debug("Creating case to delete account {}", identifiers);
        String shadowName = shadow.getName().toString();
        ObjectDeltaType objectDeltaType = new ObjectDeltaType();
        objectDeltaType.setChangeType(ChangeTypeType.DELETE);
        objectDeltaType.setObjectType(ShadowType.COMPLEX_TYPE);
        ItemDeltaType itemDeltaType = new ItemDeltaType();
        itemDeltaType.setPath(new ItemPathType(ItemPath.create("kind")));
        itemDeltaType.setModificationType(ModificationTypeType.DELETE);
        objectDeltaType.setOid(shadow.getOid());

        objectDeltaType.getItemDelta().add(itemDeltaType);
        PrismObject<CaseType> aCase;
        try {
            aCase = addCase("delete", resourceOid, shadowName, shadow.asObjectable(), objectDeltaType, task, result);
        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e.getMessage(), e);
        }
        return aCase.getOid();
    }

    private PolyStringType createCaseName(String operation, String shadowName, String resourceName) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(String.format("Request to %s '%s' on '%s'", operation, shadowName, resourceName));

        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey("ManualConnectorInstance.caseName");

        PolyStringTranslationArgumentType s = new PolyStringTranslationArgumentType();
        PolyStringTranslationType tr = new PolyStringTranslationType();
        tr.setKey("ManualConnectorInstance.operation." + operation);

        s.setTranslation(tr);
        translation.getArgument().add(s);

        addTranslationArgument(translation, shadowName);
        addTranslationArgument(translation, resourceName);

        poly.setTranslation(translation);

        return poly;
    }

    private void addTranslationArgument(PolyStringTranslationType translation, String value) {
        PolyStringTranslationArgumentType s = new PolyStringTranslationArgumentType();
        s.setValue(value);
        translation.getArgument().add(s);
    }

    private PrismObject<CaseType> addCase(
            String operation,
            String resourceOid,
            String shadowName,
            ShadowType shadow,
            ObjectDeltaType objectDelta,
            Task task,
            OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
        PrismObject<CaseType> aCase = getPrismContext().createObject(CaseType.class);
        CaseType caseType = aCase.asObjectable();

        if (randomDelayRange != 0) {
            int waitMillis = RND.nextInt(randomDelayRange);
            LOGGER.info("Manual connector waiting {} ms before creating the case", waitMillis);
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                LOGGER.error("Manual connector wait is interrupted");
            }
            LOGGER.info("Manual connector wait is over");
        }

        PrismObject<ResourceType> resource;
        try {
            resource = repositoryService.getObject(ResourceType.class, resourceOid, null, result);
        } catch (ObjectNotFoundException e) {
            // We do not signal this as ObjectNotFoundException as it could be misinterpreted as "shadow
            // object not found" with subsequent handling as such.
            throw new SystemException("Resource " + resourceOid + " couldn't be found", e);
        }
        ResourceBusinessConfigurationType businessConfiguration = resource.asObjectable().getBusiness();
        List<ObjectReferenceType> operators = new ArrayList<>();
        if (businessConfiguration != null) {
            operators.addAll(businessConfiguration.getOperatorRef());
        }
        if (operators.isEmpty() && configuration.getDefaultAssignee() != null) {
            ObjectQuery query = getPrismContext().queryFor(UserType.class)
                    .item(UserType.F_NAME).eqPoly(configuration.getDefaultAssignee()).matchingOrig()
                    .build();
            List<PrismObject<UserType>> defaultAssignees = repositoryService
                    .searchObjects(UserType.class, query, null, result);
            if (defaultAssignees.isEmpty()) {
                LOGGER.warn("Default assignee named '{}' was not found; using system-wide default instead.",
                        configuration.getDefaultAssignee());
            } else {
                assert defaultAssignees.size() == 1;
                operators.addAll(ObjectTypeUtil.objectListToReferences(defaultAssignees));
            }
        }
        if (operators.isEmpty()) {
            operators.add(new ObjectReferenceType().oid(DEFAULT_OPERATOR_OID).type(UserType.COMPLEX_TYPE));
        }

        String caseOid = OidUtil.generateOid();

        caseType.setOid(caseOid);
        caseType.setName(createCaseName(operation, shadowName, resource.getName().getOrig()));

        caseType.setState(SchemaConstants.CASE_STATE_CREATED);  // Case opening process will be completed by WorkflowEngine

        caseType.setObjectRef(new ObjectReferenceType().oid(resourceOid).type(ResourceType.COMPLEX_TYPE));
        caseType.setTargetRef(createTargetRef(shadow, shadowName, getPrismContext()));

        if (task != null) {
            if (isCaseOperationTask(task)) {
                try {
                    PrismObject<CaseType> originalCase = task.getObject(CaseType.class, result);
                    if (originalCase != null) {
                        caseType.setRequestorRef(originalCase.asObjectable().getRequestorRef());
                    }
                } catch (ObjectNotFoundException e) {
                    //ignore exception original case is null
                }
            } else {
                caseType.setRequestorRef(task.getOwnerRef());
            }
        }

        caseType.beginManualProvisioningContext()
                .beginPendingOperation()
                    .type(PendingOperationTypeType.MANUAL)
                    .delta(objectDelta)
                .<ManualProvisioningContextType>end()
                .end();

        ObjectReferenceType archetypeRef = ObjectTypeUtil
                .createObjectRef(SystemObjectsType.ARCHETYPE_MANUAL_CASE.value(), ObjectTypes.ARCHETYPE);
        caseType.getArchetypeRef().add(archetypeRef.clone());
        caseType.beginAssignment().targetRef(archetypeRef).end();

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        caseType.beginMetadata().setCreateTimestamp(now);

        XMLGregorianCalendar deadline;
        if (businessConfiguration != null && businessConfiguration.getOperatorActionMaxDuration() != null) {
            deadline = CloneUtil.clone(now);
            deadline.add(businessConfiguration.getOperatorActionMaxDuration());
        } else {
            deadline = null;
        }

        for (ObjectReferenceType operator : operators) {
            CaseWorkItemType workItem = new CaseWorkItemType(getPrismContext())
                    .originalAssigneeRef(operator.clone())
                    .assigneeRef(operator.clone())
                    .name(caseType.getName())
                    .createTimestamp(now)
                    .deadline(deadline);
            caseType.getWorkItem().add(workItem);
        }

        // TODO: case payload
        // TODO: a lot of other things

        // TODO: move to case-manager

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CREATING CASE:\n{}", aCase.debugDump(1));
        }

        repositoryService.addObject(aCase, null, result);

        // notifications
        caseEventDispatcher.dispatchCaseEvent(caseType, result);
        return aCase;
    }

    private boolean isCaseOperationTask(Task task) {
        return "http://midpoint.evolveum.com/xml/ns/public/workflow/operation-execution/handler-3".equals(task.getHandlerUri());
    }

    private static ObjectReferenceType createTargetRef(ShadowType shadow, String shadowName, PrismContext prismContext) {
        if (shadow.getOid() != null) {
            // Most probably in the repo -> no need to store in full.
            return ObjectTypeUtil.createObjectRef(shadow, prismContext);
        } else if (shadow.getName() != null || shadowName == null) {
            return ObjectTypeUtil.createObjectRefWithFullObject(shadow, prismContext);
        } else {
            // We need to provide a sensible name, to be shown in GUI (see MID-7977)
            ShadowType clone = shadow.clone();
            clone.setName(PolyStringType.fromOrig(shadowName));
            return ObjectTypeUtil.createObjectRefWithFullObject(clone, prismContext);
        }
    }

    @Override
    public OperationResultStatus queryOperationStatus(String asynchronousOperationReference, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_QUERY_CASE);

        InternalMonitor.recordConnectorOperation("queryOperationStatus");

        PrismObject<CaseType> acase;
        try {
            acase = repositoryService.getObject(CaseType.class, asynchronousOperationReference, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        CaseType caseType = acase.asObjectable();
        String state = caseType.getState();

        // States "open" and "created" are the same from the factual point of view
        // They differ only in level of processing carried out by workflow manager (audit, notifications, etc).
        if (QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_OPEN_QNAME, state)
                || QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_CREATED_QNAME, state)) {
            result.recordSuccess();
            return OperationResultStatus.IN_PROGRESS;
        } else if (QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_CLOSED_QNAME, state)
                || QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_CLOSING_QNAME, state)) {
            String outcome = caseType.getOutcome();
            OperationResultStatus status = translateOutcome(outcome);
            result.recordSuccess();
            return status;
        } else {
            SchemaException e = new SchemaException("Unknown case state "+state);
            result.recordFatalError(e);
            throw e;
        }

    }

    // see CompleteWorkItemsAction.getOutcome(..) method
    private OperationResultStatus translateOutcome(String outcome) {
        if (outcome == null) {
            return null;
        }
        for (OperationResultStatusType statusType : OperationResultStatusType.values()) {
            if (outcome.equals(statusType.value())) {
                return OperationResultStatus.parseStatusType(statusType);
            }
        }
        if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return OperationResultStatus.SUCCESS;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return OperationResultStatus.FATAL_ERROR;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP)) {
            // Better make this "unknown" than non-applicable. Non-applicable can be misinterpreted.
            return OperationResultStatus.UNKNOWN;
        }
        return OperationResultStatus.UNKNOWN;
   }

    @Override
    protected void connect(OperationResult result) {
        if (connected && InternalsConfig.isSanityChecks()) {
            throw new IllegalStateException("Double connect in "+this);
        }
        connected = true;
        // Nothing else to do
    }

    private String getShadowIdentifier(Collection<? extends ResourceAttribute<?>> identifiers){
        if (identifiers == null || identifiers.isEmpty()) {
            return "";
        }
        try {
            // FIXME treat somehow this magic
            return ((ResourceAttribute)identifiers.iterator().next()).getValue().getValue().toString();
        } catch (NullPointerException e){
            return "";
        }
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult connectionResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
        connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ManualConnectorInstance.class);
        connectionResult.addContext("connector", getConnectorObject().toString());

        if (repositoryService == null) {
            connectionResult.recordFatalError("No repository service");
            return;
        }

        if (!connected && InternalsConfig.isSanityChecks()) {
            throw new IllegalStateException("Attempt to test non-connected connector instance "+this);
        }

        connectionResult.recordSuccess();
    }

    @Override
    public void disconnect(OperationResult parentResult) {
        connected = false;
    }

    @SuppressWarnings("unused")
    public static int getRandomDelayRange() {
        return randomDelayRange;
    }

    public static void setRandomDelayRange(int randomDelayRange) {
        ManualConnectorInstance.randomDelayRange = randomDelayRange;
    }

}
