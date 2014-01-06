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

package com.evolveum.midpoint.web.component.model.operationStatus;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;

import java.io.Serializable;

/**
 * @author mederly
 */
public class ModelOperationStatusDto implements Serializable {

    public static final String F_STATE = "state";
    public static final String F_FOCUS_TYPE = "focusType";
    public static final String F_FOCUS_NAME = "focusName";
    public static final String F_PRIMARY_DELTA = "primaryDelta";

    private ModelState state;
    private String focusType;
    private String focusName;
    private DeltaDto primaryDelta;

    public ModelOperationStatusDto(ModelContext modelContext) {

        state = modelContext.getState();

        if (modelContext.getFocusContext() != null) {

            // focusType & focusName
            PrismObject object = modelContext.getFocusContext().getObjectNew();
            if (object == null) {
                object = modelContext.getFocusContext().getObjectOld();
            }
            if (object != null) {
                focusType = object.getElementName() != null ? object.getElementName().toString() : null;
                focusName = object.asObjectable().getName() != null ? object.asObjectable().getName().getOrig() : null;
            }

            // primaryDelta
            if (modelContext.getFocusContext().getPrimaryDelta() != null) {
                primaryDelta = new DeltaDto(modelContext.getFocusContext().getPrimaryDelta());
            }
        }
    }


    public ModelState getState() {
        return state;
    }

    public String getFocusType() {
        return focusType;
    }

    public String getFocusName() {
        return focusName;
    }

    public DeltaDto getPrimaryDelta() {
        return primaryDelta;
    }
}
