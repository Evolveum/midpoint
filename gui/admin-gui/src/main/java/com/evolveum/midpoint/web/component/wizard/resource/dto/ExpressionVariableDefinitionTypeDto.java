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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ExpressionVariableDefinitionTypeDto implements Serializable{

    public static final String F_VARIABLE = "variableObject";
    public static final String F_PATH = "path";
    public static final String F_VALUE = "value";

    private ExpressionVariableDefinitionType variableObject;
    private String path;
    private String value;

    public ExpressionVariableDefinitionTypeDto(ExpressionVariableDefinitionType variable){
        if(variable == null){
            variableObject = new ExpressionVariableDefinitionType();
        } else {
            variableObject = variable;
        }

        if (variableObject.getPath() != null) {
            path = variableObject.getPath().getItemPath().toString();
        }

        if (variableObject.getValue() != null) {
            value = variableObject.getValue().toString();
        }
    }

    public void prepareDtoToSave(){
        if(variableObject == null){
            variableObject = new ExpressionVariableDefinitionType();
        }

        if(value != null){
            variableObject.setValue(value);
        }

        if(path != null){
            variableObject.setPath(new ItemPathType(path));
        }
    }

    public ExpressionVariableDefinitionType getVariableObject() {
        return variableObject;
    }

    public void setVariableObject(ExpressionVariableDefinitionType variableObject) {
        this.variableObject = variableObject;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
