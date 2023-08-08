/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.util.ApplicationContextHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Base implementation of Event that contains the common functionality.
 */
public abstract class BaseEventImpl implements Event, DebugDumpable, ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(BaseEventImpl.class);

    @NotNull private final LightweightIdentifier id;

    private SimpleObjectRef requester;

    private SimpleObjectRef requestee;

    private transient MidpointFunctions midpointFunctions;
    private transient TextFormatter textFormatter;
    private transient PrismContext prismContext;

    private String channel;

    BaseEventImpl() {
        this(ApplicationContextHolder.getBean(LightweightIdentifierGenerator.class));
    }

    BaseEventImpl(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        id = lightweightIdentifierGenerator.generate();
    }

    @NotNull
    public LightweightIdentifier getId() {
        return id;
    }

    boolean changeTypeMatchesOperationType(ChangeType changeType, EventOperationType eventOperationType) {
        return switch (eventOperationType) {
            case ADD -> changeType == ChangeType.ADD;
            case MODIFY -> changeType == ChangeType.MODIFY;
            case DELETE -> changeType == ChangeType.DELETE;
        };
    }

    abstract public boolean isCategoryType(EventCategoryType eventCategory);

    public boolean isUserRelated() {
        return false; // overridden in ModelEvent
    }

    public SimpleObjectRef getRequester() {
        return requester;
    }

    // TODO make requester final and remove this method
    public void setRequester(SimpleObjectRef requester) {
        this.requester = requester;
    }

    public SimpleObjectRef getRequestee() {
        return requestee;
    }

    // TODO we need the operation result parent here
    @Nullable
    private ObjectType resolveObject(SimpleObjectRef ref) {
        if (ref == null) {
            return null;
        }
        return ref.resolveObjectType(new OperationResult(BaseEventImpl.class + ".resolveObject"), true);
    }

    @Override
    public ObjectType getRequesteeObject() {
        return resolveObject(requestee);
    }

    @SuppressWarnings("WeakerAccess")
    public ObjectType getRequesterObject() {
        return resolveObject(requester);
    }

    @Override
    public PolyStringType getRequesteeDisplayName() {
        return getDisplayName(getRequesteeObject());
    }

    @SuppressWarnings("unused")
    public PolyStringType getRequesterDisplayName() {
        return getDisplayName(getRequesterObject());
    }

    @Nullable
    private PolyStringType getDisplayName(ObjectType object) {
        if (object == null) {
            return null;
        }
        if (object instanceof UserType) {
            return ((UserType) object).getFullName();
        } else if (object instanceof AbstractRoleType) {
            return ((AbstractRoleType) object).getDisplayName();
        } else {
            return object.getName();
        }
    }

    @Nullable
    private PolyStringType getName(ObjectType object) {
        return object != null ? object.getName() : null;
    }

    @SuppressWarnings("unused")
    public PolyStringType getRequesteeName() {
        return getName(getRequesteeObject());
    }

    @SuppressWarnings("unused")
    public PolyStringType getRequesterName() {
        return getName(getRequesterObject());
    }

    public void setRequestee(SimpleObjectRef requestee) {
        this.requestee = requestee;
    }

    public void createVariablesMap(VariablesMap variables, OperationResult result) {
        variables.put(ExpressionConstants.VAR_EVENT, this, Event.class);
        variables.put(ExpressionConstants.VAR_REQUESTER, resolveTypedObject(requester, false, result));
        variables.put(ExpressionConstants.VAR_REQUESTEE, resolveTypedObject(requestee, true, result));
    }

    TypedValue<ObjectType> resolveTypedObject(SimpleObjectRef ref, boolean allowNotFound, OperationResult result) {
        ObjectType resolved = ref != null ? ref.resolveObjectType(result, allowNotFound) : null;
        if (resolved != null) {
            return new TypedValue<>(resolved, resolved.asPrismObject().getDefinition());
        } else {
            PrismObjectDefinition<ObjectType> def = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
            return new TypedValue<>(null, def);
        }
    }

    // Finding items in deltas/objects
    // this is similar to delta.hasItemDelta but much, much more relaxed (we completely ignore ID path segments and we take sub-paths into account)
    //
    // Very experimental implementation. Needs a bit of time to clean up and test adequately.
    @SuppressWarnings("WeakerAccess")
    public boolean containsItem(ObjectDelta<?> delta, ItemPath itemPath) {
        if (delta.getChangeType() == ChangeType.ADD) {
            return containsItem(delta.getObjectToAdd(), itemPath);
        } else //noinspection SimplifiableIfStatement
            if (delta.getChangeType() == ChangeType.MODIFY) {
                return containsItemInModifications(delta.getModifications(), itemPath);
            } else {
                return false;
            }
    }

    private boolean containsItemInModifications(Collection<? extends ItemDelta<?, ?>> modifications, ItemPath itemPath) {
        for (ItemDelta<?, ?> itemDelta : modifications) {
            if (containsItem(itemDelta, itemPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsItem(ItemDelta<?, ?> itemDelta, ItemPath itemPath) {
        ItemPath namesOnlyPathTested = itemPath.namedSegmentsOnly();
        ItemPath namesOnlyPathInDelta = itemDelta.getPath().namedSegmentsOnly();
        if (namesOnlyPathTested.isSubPathOrEquivalent(namesOnlyPathInDelta)) {
            return true;
        }
        // however, we can add/delete whole container (containing part of the path)
        // e.g. we can test for activation/administrativeStatus, and the delta is:
        // ADD activation VALUE (administrativeStatus=ENABLED)
        if (!namesOnlyPathInDelta.isSubPath(namesOnlyPathTested)) {
            return false;
        }
        // for ADD values we know
        // for REPLACE values we know - for values being added, but NOT for values being left behind
        // for DELETE we have a problem if we are deleting "by ID" - we just don't know if the value being deleted contains the path in question or not

        ItemPath remainder = namesOnlyPathTested.remainder(namesOnlyPathInDelta);
        return containsItemInValues(itemDelta.getValuesToAdd(), remainder) ||
                containsItemInValues(itemDelta.getValuesToReplace(), remainder) ||
                containsItemInValues(itemDelta.getValuesToDelete(), remainder);
    }

    // remainder contains only named segments and is not empty
    private boolean containsItemInValues(Collection<?> values, ItemPath remainder) {
        if (values == null) {
            return false;
        }
        for (Object value : values) {
            if (value instanceof PrismContainerValue) {     // we do not want to look inside references nor primitive values
                if (containsItem((PrismContainerValue<?>) value, remainder)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean containsItem(List<ObjectDelta<AssignmentHolderType>> deltas, ItemPath itemPath) {
        for (ObjectDelta<?> objectDelta : deltas) {
            if (containsItem(objectDelta, itemPath)) {
                return true;
            }
        }
        return false;
    }

    // itemPath is empty or starts with named item path segment
    private boolean containsItem(PrismContainer<?> container, ItemPath itemPath) {
        if (container.size() == 0) {
            return false;           // there is a container, but no values
        }
        if (itemPath.isEmpty()) {
            return true;
        }
        for (PrismContainerValue<?> o : container.getValues()) {
            if (containsItem(o, itemPath)) {
                return true;
            }
        }
        return false;
    }

    // path starts with named item path segment
    private boolean containsItem(PrismContainerValue<?> prismContainerValue, ItemPath itemPath) {
        ItemName first = ItemPath.toName(itemPath.first());
        Item<?, ?> item = prismContainerValue.findItem(first);
        if (item == null) {
            return false;
        }
        ItemPath pathTail = stripFirstIds(itemPath);
        if (item instanceof PrismContainer) {
            return containsItem((PrismContainer<?>) item, pathTail);
        } else if (item instanceof PrismReference) {
            return pathTail.isEmpty();      // we do not want to look inside references
        } else //noinspection SimplifiableIfStatement
            if (item instanceof PrismProperty) {
                return pathTail.isEmpty();      // ...neither inside atomic values
            } else {
                return false;                   // should not occur anyway
            }
    }

    private ItemPath stripFirstIds(ItemPath itemPath) {
        while (!itemPath.isEmpty() && itemPath.startsWithId()) {
            itemPath = itemPath.rest();
        }
        return itemPath;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String getStatusAsText() {
        if (isSuccess()) {
            return "SUCCESS";
        } else if (isOnlyFailure()) {
            return "FAILURE";
        } else if (isFailure()) {
            return "PARTIAL FAILURE";
        } else if (isInProgress()) {
            return "IN PROGRESS";
        } else {
            return "UNKNOWN";
        }
    }

    protected void debugDumpCommon(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "id", getId(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "requester", getRequester(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "requestee", getRequestee(), indent + 1);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this.getClass().getSimpleName()).append("(").append(getId()).append(")");
    }

    MidpointFunctions getMidpointFunctions() {
        if (midpointFunctions == null) {
            midpointFunctions = ApplicationContextHolder.getBean(MidpointFunctions.class);
        }
        return midpointFunctions;
    }

    private ModelService getModelService() {
        return ApplicationContextHolder.getBean(ModelService.class);
    }

    PrismContext getPrismContext() {
        if (prismContext == null) {
            prismContext = ApplicationContextHolder.getBean(PrismContext.class);
        }
        return prismContext;
    }

    TextFormatter getTextFormatter() {
        if (textFormatter == null) {
            textFormatter = ApplicationContextHolder.getBean(TextFormatter.class);
        }
        return textFormatter;
    }

    /**
     * As {@link MidpointFunctions#resolveReferenceIfExists(ObjectReferenceType)} but a bit more intelligent
     * (e.g. it stores resolved object right in the reference).
     */
    @Nullable ObjectType resolveReferenceIfExists(@Nullable ObjectReferenceType reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getObject() != null) {
            return (ObjectType) reference.getObject().asObjectable();
        }
        QName type = reference.getType(); // TODO what about implicitly specified types, like in resourceRef?
        PrismObjectDefinition<ObjectType> objectDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(reference.getType());
        try {
            if (objectDefinition == null) {
                throw new SchemaException("No definition for type " + type);
            }
            Collection<SelectorOptions<GetOperationOptions>> options = SchemaService.get().getOperationOptionsBuilder()
                    .executionPhase()
                    .allowNotFound(true)
                    .build();
            PrismObject<ObjectType> object = getModelService().getObject(
                    objectDefinition.getCompileTimeClass(),
                    reference.getOid(),
                    options,
                    getCurrentTask(),
                    getCurrentResult());
            if (!reference.asReferenceValue().isImmutable()) {
                reference.asReferenceValue().setObject(object);
            }
            return object.asObjectable();
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't resolve object from reference: {}", e, reference);
            return null;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object from reference: {}", e, reference);
            return null;
        }
    }

    @Nullable PolyStringType getNameFromReference(@Nullable ObjectReferenceType reference) {
        if (reference == null) {
            return null;
        } else if (reference.getTargetName() != null) {
            return reference.getTargetName();
        } else if (reference.getObject() != null) {
            return reference.getObject().asObjectable().getName();
        } else {
            ObjectType resolved = resolveReferenceIfExists(reference);
            if (resolved != null) {
                return resolved.getName();
            } else {
                return PolyStringType.fromOrig(
                        reference.getOid()); // At least something
            }
        }
    }

    protected @Nullable PolyStringType getDisplayNameFromReference(@Nullable ObjectReferenceType reference) {
        if (reference == null) {
            return null;
        } else if (reference.getObject() != null) {
            return getDisplayName((ObjectType) reference.getObject().asObjectable());
        } else {
            ObjectType resolved = resolveReferenceIfExists(reference);
            if (resolved != null) {
                return getDisplayName(resolved);
            } else {
                return PolyStringType.fromOrig(
                        reference.getOid()); // At least something
            }
        }
    }

    private Task getCurrentTask() {
        return getMidpointFunctions().getCurrentTask();
    }

    private OperationResult getCurrentResult() {
        return getMidpointFunctions().getCurrentResult();
    }

    public void setRequesterAndRequesteeAsTaskOwner(@NotNull Task task, @NotNull OperationResult result) {
        PrismObject<? extends FocusType> ownerObject = task.getOwner(result);
        if (ownerObject != null) {
            FocusType owner = ownerObject.asObjectable();
            setRequester(new SimpleObjectRefImpl(owner));
            setRequestee(new SimpleObjectRefImpl(owner));
        } else {
            LOGGER.debug("No owner for {}, therefore no requester and requestee will be set for event {}", task, getId());
        }
    }

    @Override
    public String toString() {
        return toStringPrefix() + '}';
    }

    /**
     * Prefix for {@link #toString()} after which other attributes can be added; final `}` needs be added too.
     * Useful for event subclasses.
     */
    @NotNull
    protected String toStringPrefix() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ",requester=" + requester +
                ",requestee=" + requestee;
    }
}
