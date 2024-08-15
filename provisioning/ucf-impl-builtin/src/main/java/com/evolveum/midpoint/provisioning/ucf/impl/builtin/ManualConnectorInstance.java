/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcherAware;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.cases.CaseState;
import com.evolveum.midpoint.schema.util.cases.ManualCaseUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerAware;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * @author Radovan Semancik
 */
@ManagedConnector(type = "ManualConnector", version = "1.0.0")
public class ManualConnectorInstance extends AbstractManualConnectorInstance implements RepositoryAware,
        CaseEventDispatcherAware, TaskManagerAware {

    private static final String OP_QUERY_CASE = ManualConnectorInstance.class.getName() + ".queryCase";
    private static final String OP_TEST = ManualConnectorInstance.class.getName() + ".test";

    private static final Trace LOGGER = TraceManager.getTrace(ManualConnectorInstance.class);

    private ManualConnectorConfiguration configuration;

    private RepositoryService repositoryService;
    private CaseEventDispatcher caseEventDispatcher;
    private TaskManager taskManager;

    private boolean connected = false;

    @VisibleForTesting
    private static int randomDelayRange = 0;

    private static final Random RND = new Random();

    private final Clock clock = new Clock();

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
    protected String createTicketAdd(PrismObject<? extends ShadowType> object, Task task, OperationResult result) throws SchemaException,
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
        PrismObject<CaseType> aCase = addCase(
                "create",
                ShadowUtil.getResourceOid(object.asObjectable()),
                shadowName,
                object.asObjectable(),
                objectDeltaType,
                task,
                result);
        return aCase.getOid();
    }

    @Override
    protected String createTicketModify(ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
            Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
        LOGGER.debug("Creating case to modify account {}:\n{}", identifiers, DebugUtil.debugDumpLazily(changes, 1));
        if (InternalsConfig.isSanityChecks()) {
            if (MiscUtil.hasDuplicates(changes)) {
                throw new SchemaException("Duplicated changes: " + changes);
            }
        }
        Collection<ItemDelta> changeDeltas = changes.stream()
                .filter(Objects::nonNull)
                .map(change -> ((PropertyModificationOperation) change).getPropertyDelta())
                .collect(Collectors.toList());
        ObjectDelta<? extends ShadowType> objectDelta = getPrismContext().deltaFactory().object()
                .createModifyDelta("", changeDeltas, ShadowType.class);
        ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);
        objectDeltaType.setOid(shadow.getOid());
        String shadowName = shadow.getName().toString();
        PrismObject<CaseType> aCase =
                addCase("modify", resourceOid, shadowName, shadow.asObjectable(), objectDeltaType, task, result);
        return aCase.getOid();
    }

    @Override
    protected String createTicketDelete(ResourceObjectDefinition objectDefinition, PrismObject<ShadowType> shadow,
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

    // Note that shadow may be without OID and even without name.
    private PrismObject<CaseType> addCase(
            String operation,
            String resourceOid,
            String shadowName,
            ShadowType shadow,
            ObjectDeltaType objectDelta,
            Task task,
            OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
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
            // object not found" with subsequent handling as such. (Although, recently, the ObjectNotFoundException
            // contains both class and OID of missing object -> but just to be sure.)
            throw new SystemException("Resource " + resourceOid + " couldn't be found", e);
        }
        ObjectReferenceType archetypeRef =
                ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_MANUAL_CASE.value(), ObjectTypes.ARCHETYPE);

        // @formatter:off
        CaseType aCase = new CaseType()
                .name(createCaseName(operation, shadowName, resource.getName().getOrig()))
                .state(SchemaConstants.CASE_STATE_CREATED) // Case opening process will be completed by CaseEngine
                .objectRef(resourceOid, ResourceType.COMPLEX_TYPE)
                .targetRef(createTargetRef(shadow, shadowName))
                .requestorRef(task != null ? task.getOwnerRef() : null)
                .beginManualProvisioningContext()
                    .beginPendingOperation()
                        .type(PendingOperationTypeType.MANUAL)
                        .delta(objectDelta)
                    .<ManualProvisioningContextType>end()
                    .schema(createCaseSchema(resource.asObjectable().getBusiness()))
                .<CaseType>end()
                .archetypeRef(archetypeRef.clone())
                .beginAssignment()
                    .targetRef(archetypeRef)
                .<CaseType>end()
                .beginMetadata()
                    .createTimestamp(clock.currentTimeXMLGregorianCalendar())
                .end();
        // @formatter:on

        // TODO: case payload
        // TODO: a lot of other things

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CREATING CASE:\n{}", aCase.debugDumpLazily(1));
        }

        repositoryService.addObject(aCase.asPrismObject(), null, result);

        // "Admitting" the case into case management: e.g. sending notifications, auditing the case creation, and so on
        caseEventDispatcher.dispatchCaseCreationEvent(aCase, task, result);
        return aCase.asPrismObject();
    }

    private static ObjectReferenceType createTargetRef(ShadowType shadow, String shadowName) {
        if (shadow.getOid() != null) {
            // Most probably in the repo -> no need to store in full.
            return ObjectTypeUtil.createObjectRef(shadow);
        } else if (shadow.getName() != null || shadowName == null) {
            return ObjectTypeUtil.createObjectRefWithFullObject(shadow);
        } else {
            // We need to provide a sensible name, to be shown in GUI (see MID-7977)
            ShadowType clone = shadow.clone();
            clone.setName(PolyStringType.fromOrig(shadowName));
            return ObjectTypeUtil.createObjectRefWithFullObject(clone);
        }
    }

    private SimpleCaseSchemaType createCaseSchema(@Nullable ResourceBusinessConfigurationType business) {
        SimpleCaseSchemaType schema = new SimpleCaseSchemaType();
        if (business != null) {
            schema.getAssigneeRef().addAll(
                    CloneUtil.cloneCollectionMembers(business.getOperatorRef()));
            schema.setDuration(business.getOperatorActionMaxDuration());
        }
        schema.setDefaultAssigneeName(configuration.getDefaultAssignee());
        return schema;
    }

    @Override
    public OperationResultStatus queryOperationStatus(String asynchronousOperationReference, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_QUERY_CASE);

        InternalMonitor.recordConnectorOperation("queryOperationStatus");

        PrismObject<CaseType> aCase;
        try {
            aCase = repositoryService.getObject(CaseType.class, asynchronousOperationReference, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        CaseType caseType = aCase.asObjectable();
        CaseState state = CaseState.of(caseType);

        // States "open" and "created" are the same from the factual point of view
        // They differ only in level of processing carried out by case manager (audit, notifications, etc).
        if (state.isCreated() || state.isOpen()) {
            result.recordSuccess();
            return OperationResultStatus.IN_PROGRESS;
        } else if (state.isClosing() || state.isClosed()) {
            String outcome = caseType.getOutcome();
            OperationResultStatus status = ManualCaseUtils.translateOutcomeToStatus(outcome);
            result.recordSuccess();
            return status;
        } else {
            SchemaException e = new SchemaException("Unknown case state " + state);
            result.recordFatalError(e);
            throw e;
        }
    }

    @Override
    protected void connect(OperationResult result) {
        if (connected && InternalsConfig.isSanityChecks()) {
            throw new IllegalStateException("Double connect in " + this);
        }
        connected = true;
        // Nothing else to do
    }

    private String getShadowIdentifier(Collection<? extends ResourceAttribute<?>> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return "";
        }
        try {
            // FIXME treat somehow this magic
            return ((ResourceAttribute<?>) identifiers.iterator().next()).getValue().getValue().toString();
        } catch (NullPointerException e) {
            return "";
        }
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_TEST);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, getClass());
        result.addContext("connector", getConnectorObject().toString());
        try {
            stateCheck(repositoryService != null, "No repository service");
            if (InternalsConfig.isSanityChecks()) {
                stateCheck(connected, "Attempt to test non-connected connector instance %s", this);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
        } finally {
            result.close();
        }
    }

    @Override
    public void testPartialConfiguration(OperationResult parentResult) {
        // no-op
    }

    @Override
    public @NotNull Collection<PrismProperty<?>> discoverConfiguration(OperationResult parentResult) {
        return Collections.emptySet();
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(OperationResult result) throws CommunicationException, GenericFrameworkException, ConfigurationException {
        return fetchCapabilities(result);
    }

    @Override
    public void disconnect(OperationResult parentResult) {
        connected = false;
    }

    @VisibleForTesting
    public static int getRandomDelayRange() {
        return randomDelayRange;
    }

    @VisibleForTesting
    public static void setRandomDelayRange(int randomDelayRange) {
        ManualConnectorInstance.randomDelayRange = randomDelayRange;
    }

}
