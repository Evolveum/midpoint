/*
 * Copyright (c) 2012 Evolveum
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

package com.evolveum.midpoint.web.component.model.operationStatus;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
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
                focusType = object.getName() != null ? object.getName().toString() : null;
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
