/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    public static ModelStateType toModelStateType(ModelState value) {
        return value != null ? value.toModelStateType() : null;
    }

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
