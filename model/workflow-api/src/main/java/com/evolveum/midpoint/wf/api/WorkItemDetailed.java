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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.io.Serializable;
import java.util.Date;

/**
 * Structure carrying data about a particular work item, i.e. a task that has to be carried out by a user
 * (or an information that has to be displayed to him).
 *
 * In addition to basic data, this class (WorkItemDetailed) contains detailed information that has to be
 * shown to the user.
 *
 * @author mederly
 */
public class WorkItemDetailed extends WorkItem {

    private PrismObject<UserType> requester;
    private PrismObject<? extends ObjectType> objectOld;              // object before requested modification (typically, a user)
    private PrismObject<? extends ObjectType> objectNew;              // object after requested modification (typically, a user)
    private PrismObject<? extends ObjectType> requestSpecificData;    // data whose format is specific to the request (e.g. reason, ...)
    private PrismObject<? extends ObjectType> trackingData;           // general tracking data, e.g. IDs of related objects in activiti
    private PrismObject<? extends ObjectType> additionalData;         // additional data, e.g. the description of the role to be added
    private ObjectDelta objectDelta;                                  // delta to be approved; generally, objectNew = objectOld + objectDelta

    public PrismObject<? extends ObjectType> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(PrismObject<? extends ObjectType> additionalData) {
        this.additionalData = additionalData;
    }

    public PrismObject<? extends ObjectType> getObjectNew() {
        return objectNew;
    }

    public void setObjectNew(PrismObject<? extends ObjectType> objectNew) {
        this.objectNew = objectNew;
    }

    public PrismObject<? extends ObjectType> getObjectOld() {
        return objectOld;
    }

    public void setObjectOld(PrismObject<? extends ObjectType> objectOld) {
        this.objectOld = objectOld;
    }

    public PrismObject<UserType> getRequester() {
        return requester;
    }

    public void setRequester(PrismObject<UserType> requester) {
        this.requester = requester;
    }

    public PrismObject<? extends ObjectType> getRequestSpecificData() {
        return requestSpecificData;
    }

    public void setRequestSpecificData(PrismObject<? extends ObjectType> requestSpecificData) {
        this.requestSpecificData = requestSpecificData;
    }

    public PrismObject<? extends ObjectType> getTrackingData() {
        return trackingData;
    }

    public void setTrackingData(PrismObject<? extends ObjectType> trackingData) {
        this.trackingData = trackingData;
    }

    public ObjectDelta getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta objectDelta) {
        this.objectDelta = objectDelta;
    }
}
