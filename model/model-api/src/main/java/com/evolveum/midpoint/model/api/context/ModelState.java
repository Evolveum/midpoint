/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ModelStateType;

/**
 * @author semancik
 *
 */
public enum ModelState {
	
	INITIAL,
	
	PRIMARY,
	
	SECONDARY,
	
	EXECUTION,
	
	POSTEXECUTION,
	
	FINAL;

    public ModelStateType toModelStateType() {
        switch (this) {
            case INITIAL: return ModelStateType.INITIAL;
            case PRIMARY: return ModelStateType.PRIMARY;
            case SECONDARY: return ModelStateType.SECONDARY;
            case EXECUTION: return ModelStateType.EXECUTION;
            case POSTEXECUTION: return ModelStateType.POSTEXECUTION;
            case FINAL: return ModelStateType.FINAL;
            default: throw new AssertionError("Unknown value of ModelState: " + this);
        }
    }

    public static ModelState fromModelStateType(ModelStateType modelStateType) {
        if (modelStateType == null) {
            return null;
        }
        switch (modelStateType) {
            case INITIAL: return INITIAL;
            case PRIMARY: return PRIMARY;
            case SECONDARY: return SECONDARY;
            case EXECUTION: return EXECUTION;
            case POSTEXECUTION: return POSTEXECUTION;
            case FINAL: return FINAL;
            default: throw new AssertionError("Unknown value of ModelStateType: " + modelStateType);
        }
    }

}
