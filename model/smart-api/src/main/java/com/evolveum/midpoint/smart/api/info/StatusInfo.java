/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api.info;

import java.io.Serializable;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Information about the status of a smart integration operation executing in the background.
 */
public interface StatusInfo<T> extends Serializable, DebugDumpable {

    /** Identification token of the operation. */
    String getToken();

    /** Was the operation already started? */
    boolean wasStarted();

    /**
     * Returns true if the operation is currently executing. This means the activity is not completed yet and there is at least
     * one task executing the activity.
     */
    boolean isExecuting();

    /**
     * Is the operation already complete? This generally means that it produced at least partially usable result.
     * If the operation was suspended or failed, {@link #isHalted()} is true instead.
     */
    boolean isComplete();

    /** Was the operation started but failed without completing? (E.g. suspended by admin or failed.) */
    default boolean isHalted() {
        return wasStarted() && !isExecuting() && !isComplete();
    }

    /**
     * Status of the operation, typically one of the following:
     *
     * - {@link OperationResultStatus#UNKNOWN} if the operation was not started yet or if the status is unknown
     * - {@link OperationResultStatus#IN_PROGRESS} if the operation is still running and there is a chance that it will complete
     * - {@link OperationResultStatus#SUCCESS} (or warning or handled error) if the operation completed more-or-less successfully
     * - {@link OperationResultStatus#FATAL_ERROR} if the operation failed and it is not expected to complete successfully
     * - {@link OperationResultStatus#PARTIAL_ERROR} if the operation partially failed
     *
     * Note that, in theory, completed operations may still have a kind of error status. Also, suspended or failed operations
     * still can have a kind of success status. Hence, to learn about the execution status of the operation, it is recommended
     * to use {@link #wasStarted()}, {@link #isExecuting()}, {@link #isComplete()} methods instead of relying on the status.
     */
    OperationResultStatusType getStatus();

    /** Returns an explanatory message about the operation status, if available. Should be present for error statuses. */
    @Nullable LocalizableMessage getMessage();

    /** Returns the message (if available) in the system locale. */
    @Nullable String getLocalizedMessage();

    /**
     * Returns the details about the progress of the activity and potential sub-activities.
     *
     * Most relevant information are:
     *
     * * {@link ActivityProgressInformation#getActivityIdentifier()} used to derive the activity display name using properties,
     * like this:
     * ** `Activity.name.shadowsCollection="Shadows collection"`
     * ** `Activity.explanation.shadowsCollection="Collecting correlated shadows"`
     * (Note that activity identifier is `null` for the root activity, i.e. the operation itself.)
     *
     * * {@link ActivityProgressInformation#getDisplayOrder()} used to order the activities in the UI.
     *
     * * {@link ActivityProgressInformation#getRealizationState()} (or helper methods like
     * {@link ActivityProgressInformation#isInProgress()}) to see the state of the activity.
     *
     * * {@link ActivityProgressInformation#getItemsProgress()} to see the (numerical) progress of the activity.
     * Alternatively, you can use {@link ActivityProgressInformation#toHumanReadableString(boolean)} to get a human-readable
     * progress right away. Note that that method deserves internationalization.
     *
     * * {@link ActivityProgressInformation#getChildren()} to get the sub-activities of the activity.
     */
    @Nullable ActivityProgressInformation getProgressInformation();

    /**
     * Returns the information about what objects (resource, object class, kind, intent) is this request about.
     * Note that "suggest object types" request has obviously no kind and intent; and other (type related) requests
     * might miss the object class name. This may change in the future.
     */
    @Nullable BasicResourceObjectSetType getRequest();

    /**
     * When the operation was started. Normally not null, but may be null if there are some issues with
     * scheduling and the task was not started yet.
     *
     * @see #wasStarted()
     */
    @Nullable XMLGregorianCalendar getRealizationStartTimestamp();

    /**
     * When the operation was finished. Null if the operation is still unfinished.
     *
     * By "finished" we mean "finished with at least some result". So, e.g., if the operation was suspended by admin,
     * or totally failed because of the remote service unavailability, this is still null.
     *
     * @see #getRunEndTimestamp()
     */
    @Nullable XMLGregorianCalendar getRealizationEndTimestamp();

    /**
     * When the last operation run finished. Null if the operation is still running.
     *
     * There will be a value here even if the operation was suspended by admin or failed.
     *
     * NOTE: If some of these activities would be delegated (to a separate task) or distributed (to worker tasks), the timestamp
     * here would be the moment when the activity yielded the control to delegate or to worker tasks. This is currently not the
     * case, as we do not use delegation or distribution in smart integration operations. But it may change in the future.
     * After that, we'd need to introduce something like "when the task stopped executing the activity" timestamp. We don't have
     * it now.
     */
    @Nullable XMLGregorianCalendar getRunEndTimestamp();

    /** Result of the operation (e.g., suggested object types) - if applicable. */
    T getResult();

    /**
     * Returns the name of the object class that is being processed by this operation, if applicable.
     * Currently present on "suggest object types" operations only.
     */
    default @Nullable QName getObjectClassName() {
        var request = getRequest();
        return request != null ? request.getObjectclass() : null;
    }

    default @Nullable String getObjectClassNameLocalPart() {
        var objectClassName = getObjectClassName();
        return objectClassName != null ? objectClassName.getLocalPart() : null;
    }
}
