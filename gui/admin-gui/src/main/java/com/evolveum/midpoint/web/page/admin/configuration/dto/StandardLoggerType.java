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
package com.evolveum.midpoint.web.page.admin.configuration.dto;

/**
 *  @author shood
 * */
public enum StandardLoggerType {

    MAPPING("com.evolveum.midpoint.common.mapping.Mapping"),
    EXPRESSION("com.evolveum.midpoint.common.expression.Expression"),
    SCRIPT_EXPRESSION("com.evolveum.midpoint.common.expression.script.ScriptExpression"),
    PROJECTOR("com.evolveum.midpoint.model.lens.projector.Projector"),
    PROJECTOR_DETAIL("com.evolveum.midpoint.model.lens.projector"),
    CLOCKWORK("com.evolveum.midpoint.model.lens.Clockwork"),
    CHANGE_EXECUTOR("com.evolveum.midpoint.model.lens.ChangeExecutor");

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
