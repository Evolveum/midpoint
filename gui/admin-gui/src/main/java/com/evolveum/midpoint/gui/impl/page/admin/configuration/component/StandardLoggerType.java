/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

/**
 *  @author shood
 * */
public enum StandardLoggerType {

    MAPPING("com.evolveum.midpoint.model.common.mapping.Mapping"),
    EXPRESSION("com.evolveum.midpoint.model.common.expression.Expression"),
    SCRIPT_EXPRESSION("com.evolveum.midpoint.model.common.expression.script.ScriptExpression"),
    PROJECTOR("com.evolveum.midpoint.model.impl.lens.projector.Projector"),
    PROJECTOR_DETAIL("com.evolveum.midpoint.model.impl.lens.projector"),
    CLOCKWORK("com.evolveum.midpoint.model.impl.lens.Clockwork"),
    CHANGE_EXECUTOR("com.evolveum.midpoint.model.impl.lens.ChangeExecutor"),
    SYNCHRONIZATION("com.evolveum.midpoint.model.impl.sync"),
    AUTHORIZATION("com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl");

    private final String value;

    StandardLoggerType(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }

    public static StandardLoggerType fromValue(String value){
        for(StandardLoggerType l: StandardLoggerType.values()){
            if(l.value.equals(value)){
                return l;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
