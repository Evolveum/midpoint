/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.prism.PrismObject;
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

}
