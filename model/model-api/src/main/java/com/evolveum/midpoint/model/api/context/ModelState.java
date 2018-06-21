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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelStateType;

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
