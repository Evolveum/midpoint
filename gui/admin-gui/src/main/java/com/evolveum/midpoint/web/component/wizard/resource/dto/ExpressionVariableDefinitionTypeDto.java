/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;

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

    public void prepareDtoToSave(PrismContext prismContext){
        if(variableObject == null){
            variableObject = new ExpressionVariableDefinitionType();
        }

        if(value != null){
            variableObject.setValue(value);
        }

        if(path != null){
            variableObject.setPath(prismContext.itemPathParser().asItemPathType(path));
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
