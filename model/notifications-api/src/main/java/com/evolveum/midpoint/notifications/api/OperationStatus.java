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

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 14.1.2013
 * Time: 22:36
 * To change this template use File | Settings | File Templates.
 */
public enum OperationStatus {
    SUCCESS, IN_PROGRESS, FAILURE, OTHER;

    public boolean matchesEventStatusType(EventStatusType eventStatusType) {
        switch (eventStatusType) {
            case ALSO_SUCCESS:
            case SUCCESS: return this == OperationStatus.SUCCESS;
            case ONLY_FAILURE:
            case FAILURE: return this == OperationStatus.FAILURE;
            case IN_PROGRESS: return this == OperationStatus.IN_PROGRESS;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

}
