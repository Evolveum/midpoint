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

package com.evolveum.midpoint.notifications.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ModelEvent extends Event {

    private ModelContext<UserType, ? extends ShadowType> modelContext;

    public ModelContext<UserType, ? extends ShadowType> getModelContext() {
        return modelContext;
    }

    public void setModelContext(ModelContext<UserType, ? extends ShadowType> modelContext) {
        this.modelContext = modelContext;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        if (modelContext.getFocusContext() != null && modelContext.getFocusContext().getPrimaryDelta() != null) {
            ObjectDelta primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
            switch (eventOperationType) {
                case ADD: return primaryDelta.isAdd();
                case MODIFY: return primaryDelta.isModify();
                case DELETE: return primaryDelta.isDelete();
                default: throw new IllegalStateException("Unknown EventOperationType: " + eventOperationType);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.USER_OPERATION;
    }

    public List<ObjectDelta<UserType>> getUserDeltas() {
        List<ObjectDelta<UserType>> retval = new ArrayList<ObjectDelta<UserType>>();
        if (modelContext.getFocusContext() != null) {
            ModelElementContext<UserType> fc = modelContext.getFocusContext();
            Class c = modelContext.getFocusClass();
            if (c != null && UserType.class.isAssignableFrom(c)) {
                if (fc.getPrimaryDelta() != null) {
                    retval.add(fc.getPrimaryDelta());
                }
                if (fc.getSecondaryDelta() != null) {
                    retval.add(fc.getSecondaryDelta());
                }
            }
        }
        return retval;
    }
}
