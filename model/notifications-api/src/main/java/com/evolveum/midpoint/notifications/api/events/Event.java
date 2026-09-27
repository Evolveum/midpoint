/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.Safe;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Notification event that should be propagated, filtered, externalized (typically to ascii or html), and send out.
 */
@SuppressWarnings("unused") // Event methods are often called from notification expressions.
@Safe
public interface Event extends DebugDumpable, ShortDumpable, Serializable {

    /**
     * Randomly generated event ID. It is immutable.
     */
    @Safe
    @NotNull LightweightIdentifier getId();

    /**
     * Entity that requested the operation that resulted in the event being generated.
     * May be null if unknown.
     */
    @Safe
    SimpleObjectRef getRequester();

    /**
     * @return OID of the requester
     */
    @Safe
    default String getRequesterOid() {
        return getRequester() != null ? getRequester().getOid() : null;
    }

    /**
     * Entity that is the object of this event or the "owner" of the object of this event.
     * Typically it is the user that we'd like to send the notification to.
     *
     * For example:
     * - for model notifications: the focal object
     * - for resource object (account) notifications: resource object (account) owner
     * - workflow notifications: the object of the approval case
     * - certification notifications: campaign owner or reviewer (depending on the kind of event)
     *
     * May be null if unknown.
     */
    @Safe
    SimpleObjectRef getRequestee();

    /**
     * @return true if requestee is of give type
     */
    @Safe
    default boolean requesteeIs(Class<?> type) {
        ObjectType requesteeObject = getRequesteeObject();
        return requesteeObject != null && type.isAssignableFrom(requesteeObject.getClass());
    }

    @Safe
    default boolean requesteeIsUser() {
        return requesteeIs(UserType.class);
    }

    /**
     * @return resolved requestee object (or null)
     */
    @Safe
    ObjectType getRequesteeObject();

    /**
     * @return display name of the requestee (or null)
     */
    @Safe
    PolyStringType getRequesteeDisplayName();

    /**
     * @return OID of the requestee
     */
    @Safe
    default String getRequesteeOid() {
        return getRequestee() != null ? getRequestee().getOid() : null;
    }

    /**
     * @return true if the status of the operation that caused this event corresponds to the specified one
     */
    @Safe
    boolean isStatusType(EventStatusType eventStatus);

    /**
     * @return true if the type of the operation that caused this event corresponds to the specified one
     */
    @Safe
    boolean isOperationType(EventOperationType eventOperation);

    /**
     * @return true if the category of the event matches the specified one
     */
    @Safe
    boolean isCategoryType(EventCategoryType eventCategory);

    /**
     * @return true if the object of the event is of UserType
     *
     * Currently applies only to ModelEvent and CustomEvent.
     * TODO specify semantics of this method more precisely; see also MID-4598
     */
    @Safe
    boolean isUserRelated();

    @Safe
    @Deprecated // Remove in 4.2
    default boolean isAccountRelated() {
        return isCategoryType(EventCategoryType.RESOURCE_OBJECT_EVENT);
    }

    @Safe
    default boolean isWorkItemRelated() {
        return isCategoryType(EventCategoryType.WORK_ITEM_EVENT);
    }

    @Safe
    @Deprecated // We no longer talk about workflow processes. There are approval cases instead. Remove in 4.2.
    default boolean isWorkflowProcessRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_PROCESS_EVENT);
    }

    @Safe
    @Deprecated // We no longer talk about workflows. There are approvals instead. Remove in 4.2.
    default boolean isWorkflowRelated() {
        return isCategoryType(EventCategoryType.WORKFLOW_EVENT);
    }

    @Safe
    default boolean isPolicyRuleRelated() {
        return isCategoryType(EventCategoryType.POLICY_RULE_EVENT);
    }

    @Safe
    default boolean isCertCampaignStageRelated() {
        return isCategoryType(EventCategoryType.CERT_CAMPAIGN_STAGE_EVENT);
    }

    @Safe
    default boolean isAdd() {
        return isOperationType(EventOperationType.ADD);
    }

    @Safe
    default boolean isModify() {
        return isOperationType(EventOperationType.MODIFY);
    }

    @Safe
    default boolean isDelete() {
        return isOperationType(EventOperationType.DELETE);
    }

    @Safe
    default boolean isSuccess() {
        return isStatusType(EventStatusType.SUCCESS);
    }

    @Safe
    default boolean isAlsoSuccess() {
        return isStatusType(EventStatusType.ALSO_SUCCESS);
    }

    @Safe
    default boolean isFailure() {
        return isStatusType(EventStatusType.FAILURE);
    }

    @Safe
    default boolean isOnlyFailure() {
        return isStatusType(EventStatusType.ONLY_FAILURE);
    }

    @Safe
    default boolean isInProgress() {
        return isStatusType(EventStatusType.IN_PROGRESS);
    }

    /**
     * Checks if the event is related to an item with a given path.
     * The meaning of the result depends on a kind of event (focal, resource object, workflow)
     * and on operation (add, modify, delete).
     *
     * Namely, this method is currently defined for ADD and MODIFY (not for DELETE) operations,
     * for focal and resource objects events (not for workflow ones).
     *
     * For MODIFY it checks whether an item with a given path is touched.
     * For ADD it checks whether there is a value for an item with a given path in the object created.
     *
     * For unsupported events the method returns false.
     *
     * Paths are compared without taking ID segments into account.
     *
     * EXPERIMENTAL; does not always work (mainly for values being deleted)
     */
    @Safe
    default boolean isRelatedToItem(ItemPath itemPath) {
        return false;
    }

    /**
     * @return channel that was used to initiate the operation that caused this event
     */
    @Safe
    String getChannel();

    /**
     * @return textual representation of event status
     *
     * TODO consider what to do with this
     */
    @Safe
    String getStatusAsText();

    /**
     * Returns plaintext focus password value, if known.
     * Beware: might not always work correctly:
     * 1. If the change execution was only partially successful, the value returned might or might not be stored in the repo
     * 2. If the password was changed to null, the 'null' value is returned. So the caller cannot distinguish it from "no change"
     *    situation. A new method for this would be needed.
     */
    @Safe
    default String getFocusPassword() {
        return null;
    }
}
