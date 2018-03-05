/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.NotificationFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class BaseEvent implements Event, DebugDumpable, ShortDumpable {

    private LightweightIdentifier id;               // randomly generated event ID
    private SimpleObjectRef requester;              // who requested this operation (null if unknown)

	/**
	 * If needed, we can prescribe the handler that should process this event. It is recommended only for ad-hoc situations.
	 * A better is to define handlers in system configuration.
	 */
	protected final EventHandlerType adHocHandler;

	private transient NotificationFunctions notificationFunctions;	// needs not be set when creating an event ... it is set in NotificationManager

    // about who is this operation (null if unknown);
    // - for model notifications, this is the focus, (usually a user but may be e.g. role or other kind of object)
    // - for account notifications, this is the account owner,
    // - for workflow notifications, this is the workflow process instance object
    // - for certification notifications, this is the campaign owner or reviewer (depending on the kind of event)

    private SimpleObjectRef requestee;

    private String channel;

    public BaseEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        this(lightweightIdentifierGenerator, null);
    }

	public BaseEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, EventHandlerType adHocHandler) {
		id = lightweightIdentifierGenerator.generate();
		this.adHocHandler = adHocHandler;
	}

	@Override
    public LightweightIdentifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ",requester=" + requester +
                ",requestee=" + requestee +
                '}';
    }

    @Override
    abstract public boolean isStatusType(EventStatusType eventStatusType);
    @Override
    abstract public boolean isOperationType(EventOperationType eventOperationType);
    @Override
    abstract public boolean isCategoryType(EventCategoryType eventCategoryType);

    @Override
    public boolean isAccountRelated() {
        return isCategoryType(EventCategoryType.RESOURCE_OBJECT_EVENT);
    }

    @Override
    public boolean isUserRelated() {
        return false;             // overriden in ModelEvent
    }

    @Override
    public boolean isWorkItemRelated() {
        return isCategoryType(EventCategoryType.WORK_ITEM_EVENT);
    }

    @Override
    public boolean isWorkflowProcessRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_PROCESS_EVENT);
    }

    @Override
    public boolean isWorkflowRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_EVENT);
    }

	@Override
	public boolean isPolicyRuleRelated() {
		return isCategoryType(EventCategoryType.POLICY_RULE_EVENT);
	}

	public boolean isCertCampaignStageRelated() {
        return isCategoryType(EventCategoryType.CERT_CAMPAIGN_STAGE_EVENT);
    }

    @Override
    public boolean isAdd() {
        return isOperationType(EventOperationType.ADD);
    }

    @Override
    public boolean isModify() {
        return isOperationType(EventOperationType.MODIFY);
    }

    @Override
    public boolean isDelete() {
        return isOperationType(EventOperationType.DELETE);
    }

    @Override
    public boolean isSuccess() {
        return isStatusType(EventStatusType.SUCCESS);
    }

    @Override
    public boolean isAlsoSuccess() {
        return isStatusType(EventStatusType.ALSO_SUCCESS);
    }

    @Override
    public boolean isFailure() {
        return isStatusType(EventStatusType.FAILURE);
    }

    @Override
    public boolean isOnlyFailure() {
        return isStatusType(EventStatusType.ONLY_FAILURE);
    }

    @Override
    public boolean isInProgress() {
        return isStatusType(EventStatusType.IN_PROGRESS);
    }

    // requester

    @Override
    public SimpleObjectRef getRequester() {
        return requester;
    }

    @Override
    public String getRequesterOid() {
        return requester.getOid();
    }

    @Override
    public void setRequester(SimpleObjectRef requester) {
        this.requester = requester;
    }

    // requestee

    @Override
    public SimpleObjectRef getRequestee() {
        return requestee;
    }

    @Override
    public String getRequesteeOid() {
        return requestee.getOid();
    }

	public ObjectType getRequesteeObject() {
		if (requestee == null) {
			return null;
		}
		return requestee.resolveObjectType(new OperationResult(BaseEvent.class + ".getRequesteeObject"), true);
	}

	public PolyStringType getRequesteeDisplayName() {
		if (requestee == null) {
			return null;
		}
		ObjectType requesteeObject = getRequesteeObject();
		if (requesteeObject == null) {
			return null;
		}
		if (requesteeObject instanceof UserType) {
			return ((UserType) requesteeObject).getFullName();
		} else if (requesteeObject instanceof AbstractRoleType) {
			return ((AbstractRoleType) requesteeObject).getDisplayName();
		} else {
			return requesteeObject.getName();
		}
	}

	public PolyStringType getRequesteeName() {
		if (requestee == null) {
			return null;
		}
		ObjectType requesteeObject = getRequesteeObject();
		return requesteeObject != null ? requesteeObject.getName() : null;
	}

    @Override
    public void setRequestee(SimpleObjectRef requestee) {
        this.requestee = requestee;
    }

    boolean changeTypeMatchesOperationType(ChangeType changeType, EventOperationType eventOperationType) {
        switch (eventOperationType) {
            case ADD: return changeType == ChangeType.ADD;
            case MODIFY: return changeType == ChangeType.MODIFY;
            case DELETE: return changeType == ChangeType.DELETE;
            default: throw new IllegalStateException("Unexpected EventOperationType: " + eventOperationType);
        }
    }

    @Override
    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        variables.put(SchemaConstants.C_EVENT, this);
        variables.put(SchemaConstants.C_REQUESTER, requester != null ? requester.resolveObjectType(result, false) : null);
        variables.put(SchemaConstants.C_REQUESTEE, requestee != null ? requestee.resolveObjectType(result, true) : null);
    }

    // Finding items in deltas/objects
    // this is similar to delta.hasItemDelta but much, much more relaxed (we completely ignore ID path segments and we take subpaths into account)
    //
    // Very experimental implementation. Needs a bit of time to clean up and test adequately.
    public <O extends ObjectType> boolean containsItem(ObjectDelta<O> delta, ItemPath itemPath) {
        if (delta.getChangeType() == ChangeType.ADD) {
            return containsItem(delta.getObjectToAdd(), itemPath);
        } else if (delta.getChangeType() == ChangeType.MODIFY) {
            return containsItemInModifications(delta.getModifications(), itemPath);
        } else {
            return false;
        }
    }

    private boolean containsItemInModifications(Collection<? extends ItemDelta> modifications, ItemPath itemPath) {
        for (ItemDelta itemDelta : modifications) {
            if (containsItem(itemDelta, itemPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsItem(ItemDelta itemDelta, ItemPath itemPath) {
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
    private boolean containsItemInValues(Collection<PrismValue> values, ItemPath remainder) {
        if (values == null) {
            return false;
        }
        for (PrismValue value : values) {
            if (value instanceof PrismContainerValue) {     // we do not want to look inside references nor primitive values
                if (containsItem((PrismContainerValue) value, remainder)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsItem(List<ObjectDelta<FocusType>> deltas, ItemPath itemPath) {
        for (ObjectDelta objectDelta : deltas) {
            if (containsItem(objectDelta, itemPath)) {
                return true;
            }
        }
        return false;
    }

    // itemPath is empty or starts with named item path segment
    private boolean containsItem(PrismContainer container, ItemPath itemPath) {
        if (container.size() == 0) {
            return false;           // there is a container, but no values
        }
        if (itemPath.isEmpty()) {
            return true;
        }
        for (Object o : container.getValues()) {
            if (containsItem((PrismContainerValue) o, itemPath)) {
                return true;
            }
        }
        return false;
    }

    // path starts with named item path segment
    private boolean containsItem(PrismContainerValue prismContainerValue, ItemPath itemPath) {
        QName first = ((NameItemPathSegment) itemPath.first()).getName();
        Item item = prismContainerValue.findItem(first);
        if (item == null) {
            return false;
        }
        ItemPath pathTail = pathTail(itemPath);
        if (item instanceof PrismContainer) {
            return containsItem((PrismContainer) item, pathTail);
        } else if (item instanceof PrismReference) {
            return pathTail.isEmpty();      // we do not want to look inside references
        } else if (item instanceof PrismProperty) {
            return pathTail.isEmpty();      // ...neither inside atomic values
        } else {
            return false;                   // should not occur anyway
        }
    }

    private ItemPath pathTail(ItemPath itemPath) {
        while (!itemPath.isEmpty() && itemPath.first() instanceof IdItemPathSegment) {
            itemPath = itemPath.tail();
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

	public NotificationFunctions getNotificationFunctions() {
		return notificationFunctions;
	}

	public void setNotificationFunctions(NotificationFunctions notificationFunctions) {
		this.notificationFunctions = notificationFunctions;
	}

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

	@Override
	public EventHandlerType getAdHocHandler() {
		return adHocHandler;
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
}
