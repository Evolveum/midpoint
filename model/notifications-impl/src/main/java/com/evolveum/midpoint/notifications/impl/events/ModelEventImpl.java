/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

public class ModelEventImpl extends BaseEventImpl implements ModelEvent {

    private static final Trace LOGGER = TraceManager.getTrace(ModelEventImpl.class);

    @NotNull private final ModelContext<?> modelContext;
    @NotNull private final ModelElementContext<?> focusContext;

    public ModelEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ModelContext<?> modelContext) {
        super(lightweightIdentifierGenerator);
        this.modelContext = modelContext;
        this.focusContext = modelContext.getFocusContext();
    }

    @NotNull
    @Override
    public ModelContext<?> getModelContext() {
        return modelContext;
    }

    @NotNull
    @Override
    public ModelElementContext<?> getFocusContext() {
        return focusContext;
    }

    @Override
    public Collection<? extends ModelProjectionContext> getProjectionContexts() {
        return modelContext.getProjectionContexts();
    }

    @Override
    public List<? extends ObjectDeltaOperation> getFocusExecutedDeltas() {
        return getFocusContext().getExecutedDeltas();
    }

    @Override
    public List<ObjectDeltaOperation> getAllExecutedDeltas() {
        List<ObjectDeltaOperation> retval = new ArrayList<>(focusContext.getExecutedDeltas());
        for (Object o : modelContext.getProjectionContexts()) {
            ModelProjectionContext modelProjectionContext = (ModelProjectionContext) o;
            retval.addAll(modelProjectionContext.getExecutedDeltas());
        }
        return retval;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        boolean allSuccess = true, anySuccess = false, allFailure = true, anyFailure = false, anyInProgress = false;
        for (ObjectDeltaOperation objectDeltaOperation : getAllExecutedDeltas()) {
            if (objectDeltaOperation.getExecutionResult() != null) {
                switch (objectDeltaOperation.getExecutionResult().getStatus()) {
                    case SUCCESS:
                    case WARNING:
                    case HANDLED_ERROR:
                        anySuccess = true; allFailure = false;
                        break;
                    case FATAL_ERROR:
                    case PARTIAL_ERROR:
                        allSuccess = false; anyFailure = true;
                        break;
                    case IN_PROGRESS:
                        allSuccess = false; allFailure = false; anyInProgress = true;
                        break;
                    case NOT_APPLICABLE:
                        break;
                    case UNKNOWN:
                        allSuccess = false; allFailure = false;
                        break;
                    default: LOGGER.warn("Unknown execution result: {}", objectDeltaOperation.getExecutionResult().getStatus());
                }
            } else {
                allSuccess = false;
                allFailure = false;
                anyInProgress = true;
            }
        }

        switch (eventStatus) {
            case ALSO_SUCCESS: return anySuccess;
            case SUCCESS: return allSuccess;
            case FAILURE: return anyFailure;
            case ONLY_FAILURE: return allFailure;
            case IN_PROGRESS: return anyInProgress;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatus);
        }
    }

    // a bit of hack but ...
    @Override
    public ChangeType getChangeType() {
        if (isOperationType(EventOperationType.ADD)) {
            return ChangeType.ADD;
        } else if (isOperationType(EventOperationType.DELETE)) {
            return ChangeType.DELETE;
        } else {
            return ChangeType.MODIFY;
        }
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {

        // we consider an operation to be 'add' when there is 'add' delta among deltas
        // in a similar way with 'delete'
        //
        // alternatively, we could summarize deltas and then decide based on the type of summarized delta (would be a bit inefficient)

        for (Object o : getFocusExecutedDeltas()) {
            ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
            if (objectDeltaOperation.getObjectDelta().isAdd()) {
                return eventOperation == EventOperationType.ADD;
            } else if (objectDeltaOperation.getObjectDelta().isDelete()) {
                return eventOperation == EventOperationType.DELETE;
            }
        }
        return eventOperation == EventOperationType.MODIFY;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.MODEL_EVENT;
    }

    @Override
    public ObjectDelta<?> getFocusPrimaryDelta() {
        return focusContext.getPrimaryDelta();
    }

    @Override
    public ObjectDelta<?> getFocusSecondaryDelta() {
        return focusContext.getSecondaryDelta();
    }

    @Override
    public ObjectDelta<?> getFocusSummaryDelta() {
        return focusContext.getSummaryDelta();
    }

    @Override
    public List<ObjectDelta<AssignmentHolderType>> getFocusDeltas() {
        List<ObjectDelta<AssignmentHolderType>> retval = new ArrayList<>();
        Class c = modelContext.getFocusClass();
        if (c != null && AssignmentHolderType.class.isAssignableFrom(c)) {
            for (Object o : getFocusExecutedDeltas()) {
                ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
                //noinspection unchecked
                retval.add(objectDeltaOperation.getObjectDelta());
            }
        }
        return retval;
    }

    @Override
    public ObjectDelta<? extends AssignmentHolderType> getSummarizedFocusDeltas() throws SchemaException {
        return ObjectDeltaCollectionsUtil.summarize(getFocusDeltas());
    }

    @Override
    public boolean hasFocusOfType(Class<? extends AssignmentHolderType> clazz) {
        return focusContext.isOfType(clazz);
    }

    @Override
    public boolean hasFocusOfType(QName focusType) {
        PrismContainerDefinition<? extends AssignmentHolderType> pcd =
                getPrismContext().getSchemaRegistry().findContainerDefinitionByType(focusType);
        if (pcd != null) {
            Class<? extends AssignmentHolderType> expectedClass = pcd.getCompileTimeClass();
            if (expectedClass != null) {
                return hasFocusOfType(expectedClass);
            } else {
                LOGGER.warn("Couldn't find class for type {}", focusType);
                return false;
            }
        } else {
            LOGGER.warn("Couldn't find definition for type {}", focusType);
            return false;
        }
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return containsItem(getFocusDeltas(), itemPath);
    }

    @Override
    public boolean isUserRelated() {
        return hasFocusOfType(UserType.class);
    }

    @Override
    public String getFocusTypeName() {
        if (focusContext.getObjectTypeClass() == null) {
            return null;
        } else {
            String simpleName = getFocusContext().getObjectTypeClass().getSimpleName();
            return StringUtils.substringBeforeLast(simpleName, "Type");         // should usually work ;)
        }
    }

    @Override
    public boolean hasContentToShow(boolean watchAuxiliaryAttributes) {
        ObjectDelta<? extends ObjectType> summarizedDelta;
        try {
            summarizedDelta = getSummarizedFocusDeltas();
            if (summarizedDelta == null) {
                return false;
            } else if (summarizedDelta.isAdd() || summarizedDelta.isDelete()) {
                return true;
            } else if (getTextFormatter().containsVisibleModifiedItems(
                    summarizedDelta.getModifications(), false, watchAuxiliaryAttributes)) {
                return true;
            } else {
                LOGGER.trace("No relevant attributes in modify delta (watchAux={})", watchAuxiliaryAttributes);
                return false;
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unable to check if there's content to show; focus context = {}", t, focusContext.debugDump());
            return false;
        }
    }

    @Override
    public String getContentAsFormattedList(boolean showAuxiliaryAttributes, Task task, OperationResult result) {
        try {
            ObjectDelta<? extends ObjectType> summarizedDelta = getSummarizedFocusDeltas();
            if (summarizedDelta == null) {
                return ""; // should not happen
            } else if (summarizedDelta.isAdd() || summarizedDelta.isModify()) {
                if (task == null) {
                    return getTextFormatter().formatObjectModificationDelta(summarizedDelta, false,
                            showAuxiliaryAttributes);
                }
                return getTextFormatter().formatObjectModificationDelta(summarizedDelta, false, showAuxiliaryAttributes,
                        task, result);
            } else {
                return "";
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unable to determine the focus change; focus context = {}", t, focusContext.debugDump());
            return("(unable to determine the change because of schema exception: " + t.getMessage() + ")\n");
        }
    }

    public String getFocusPassword() {
        List<ObjectDelta<AssignmentHolderType>> focusDeltas = getFocusDeltas();
        if (focusDeltas.isEmpty()) {
            LOGGER.trace("getFocusPasswordFromEvent: No user deltas in event");
            return null;
        }
        if (!hasFocusOfType(FocusType.class)) {
            LOGGER.trace("getFocusPasswordFromEvent: Not a FocusType context");
            return null;
        }
        //noinspection unchecked,rawtypes
        String passwordFromDeltas = getPasswordFromDeltas((List) focusDeltas);
        if (passwordFromDeltas != null) {
            LOGGER.trace("getFocusPasswordFromEvent: Found password in user executed delta(s)");
            return passwordFromDeltas;
        }

        //noinspection unchecked
        ObjectDelta<FocusType> focusDelta = (ObjectDelta) getFocusSummaryDelta();
        if (focusDelta == null) {
            LOGGER.trace("getFocusPasswordFromEvent: No password in executed delta(s) and no primary/secondary deltas");
            return null;
        } else {
            String password = getPasswordFromDeltas(singletonList(focusDelta));
            if (password != null) {
                LOGGER.trace("getFocusPasswordFromEvent: Found password in user summary delta, continuing");
                return password;
            } else {
                LOGGER.trace("getFocusPasswordFromEvent: Summary delta present but no password there.");
                return null;
            }
        }
    }

    private String getPasswordFromDeltas(List<ObjectDelta<? extends FocusType>> deltas) {
        try {
            return getMidpointFunctions().getPlaintextUserPasswordFromDeltas(deltas);
        } catch (EncryptionException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decrypt password from user deltas: {}", e, DebugUtil.debugDump(deltas));
            return null;
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToString(sb, "modelContext", modelContext, indent + 1);
        return sb.toString();
    }
}
