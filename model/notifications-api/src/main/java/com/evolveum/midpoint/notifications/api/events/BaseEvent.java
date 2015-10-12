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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class BaseEvent implements Event {

    private LightweightIdentifier id;               // randomly generated event ID
    private SimpleObjectRef requester;              // who requested this operation (null if unknown)

    // about who is this operation (null if unknown);
    // - for model notifications, this is the focus, (usually a user but may be e.g. role or other kind of object)
    // - for account notifications, this is the account owner,
    // - for workflow notifications, this is the workflow process instance object
    // - for certification notifications, this is the campaign owner or reviewer (depending on the kind of event)

    private SimpleObjectRef requestee;

    private String channel;

    public BaseEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        id = lightweightIdentifierGenerator.generate();
    }

    public LightweightIdentifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ",requester=" + requester +
                ",requestee=" + requestee +
                '}';
    }

    abstract public boolean isStatusType(EventStatusType eventStatusType);
    abstract public boolean isOperationType(EventOperationType eventOperationType);
    abstract public boolean isCategoryType(EventCategoryType eventCategoryType);

    public boolean isAccountRelated() {
        return isCategoryType(EventCategoryType.RESOURCE_OBJECT_EVENT);
    }

    public boolean isUserRelated() {
        return false;             // overriden in ModelEvent
    }

    public boolean isWorkItemRelated() {
        return isCategoryType(EventCategoryType.WORK_ITEM_EVENT);
    }

    public boolean isWorkflowProcessRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_PROCESS_EVENT);
    }

    public boolean isWorkflowRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_EVENT);
    }

    public boolean isCertCampaignStageRelated() {
        return isCategoryType(EventCategoryType.CERT_CAMPAIGN_STAGE_EVENT);
    }

    public boolean isAdd() {
        return isOperationType(EventOperationType.ADD);
    }

    public boolean isModify() {
        return isOperationType(EventOperationType.MODIFY);
    }

    public boolean isDelete() {
        return isOperationType(EventOperationType.DELETE);
    }

    public boolean isSuccess() {
        return isStatusType(EventStatusType.SUCCESS);
    }

    public boolean isAlsoSuccess() {
        return isStatusType(EventStatusType.ALSO_SUCCESS);
    }

    public boolean isFailure() {
        return isStatusType(EventStatusType.FAILURE);
    }

    public boolean isOnlyFailure() {
        return isStatusType(EventStatusType.ONLY_FAILURE);
    }

    public boolean isInProgress() {
        return isStatusType(EventStatusType.IN_PROGRESS);
    }

    // requester

    public SimpleObjectRef getRequester() {
        return requester;
    }

    public String getRequesterOid() {
        return requester.getOid();
    }

    public void setRequester(SimpleObjectRef requester) {
        this.requester = requester;
    }

    // requestee

    public SimpleObjectRef getRequestee() {
        return requestee;
    }

    public String getRequesteeOid() {
        return requestee.getOid();
    }

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

    public void createExpressionVariables(Map<QName, Object> variables, OperationResult result) {
        variables.put(SchemaConstants.C_EVENT, this);
        variables.put(SchemaConstants.C_REQUESTER, requester != null ? requester.resolveObjectType(result) : null);
        variables.put(SchemaConstants.C_REQUESTEE, requestee != null ? requestee.resolveObjectType(result) : null);
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
}
