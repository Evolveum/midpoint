/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

import java.util.Map;

/**
 * @author mederly
 */
public interface Event extends DebugDumpable, ShortDumpable {

    LightweightIdentifier getId();

    boolean isStatusType(EventStatusType eventStatusType);
    boolean isOperationType(EventOperationType eventOperationType);
    boolean isCategoryType(EventCategoryType eventCategoryType);

    boolean isAccountRelated();

    boolean isUserRelated();

    boolean isWorkItemRelated();

    boolean isWorkflowProcessRelated();

    boolean isWorkflowRelated();

    boolean isPolicyRuleRelated();

    boolean isAdd();

    boolean isModify();

    boolean isDelete();

    boolean isSuccess();

    boolean isAlsoSuccess();

    boolean isFailure();

    boolean isOnlyFailure();

    boolean isInProgress();

    // requester

    SimpleObjectRef getRequester();

    String getRequesterOid();

    void setRequester(SimpleObjectRef requester);

    // requestee

    SimpleObjectRef getRequestee();

    String getRequesteeOid();

    void setRequestee(SimpleObjectRef requestee);

    void createExpressionVariables(Map<QName, Object> variables, OperationResult result);

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
     *
     * @param itemPath
     * @return
     */
    boolean isRelatedToItem(UniformItemPath itemPath);

    String getChannel();

    /**
     * If needed, we can prescribe the handler that should process this event. It is recommended only for ad-hoc situations.
     * A better is to define handlers in system configuration.
     */
    EventHandlerType getAdHocHandler();

    /**
     * Returns plaintext focus password value, if known.
     * Beware: might not always work correctly:
     * 1. If the change execution was only partially successful, the value returned might or might not be stored in the repo
     * 2. If the password was changed to null, the 'null' value is returned. So the caller cannot distinguish it from "no change"
     *    situation. A new method for this would be needed.
     */
    default String getFocusPassword() {
        return null;
    }
}
