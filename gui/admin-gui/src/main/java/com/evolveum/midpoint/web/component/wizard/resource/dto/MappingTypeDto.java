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

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSourceDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class MappingTypeDto implements Serializable {

    public static enum ExpressionEvaluatorType{
        LITERAL,
        AS_IS,
        PATH,
        SCRIPT,
        GENERATE
    }

    public static final String F_MAPPING = "mappingObject";
    public static final String F_EXPRESSION = "expression";
    public static final String F_CONDITION = "condition";
    public static final String F_TARGET = "target";
    public static final String F_SOURCE = "source";
    public static final String F_EXPRESSION_TYPE = "expressionType";
    public static final String F_CONDITION_TYPE = "conditionType";

    private MappingType mappingObject;
    private String expression;
    private String condition;
    private String target;
    private List<String> source = new ArrayList<>();
    private ExpressionEvaluatorType expressionType = null;
    private ExpressionEvaluatorType conditionType = null;

    public MappingTypeDto(MappingType mapping){

        if(mapping == null){
            mappingObject = new MappingType();
        } else {
            mappingObject = mapping;
        }

        if(mappingObject.getChannel().isEmpty()){
            mappingObject.getChannel().add(new String());
        }

        if(mappingObject.getExceptChannel().isEmpty()){
        mappingObject.getExceptChannel().add(new String());
        }

        for(MappingSourceDeclarationType mappingSource: mappingObject.getSource()){
            if(mappingSource.getPath() != null && mappingSource.getPath().getItemPath() != null){
                source.add(mappingSource.getPath().getItemPath().toString());
            }
        }

        if(source.isEmpty()){
            source.add(new String());
        }

        if(mappingObject.getTarget() != null && mappingObject.getTarget().getPath() != null
                && mappingObject.getTarget().getPath().getItemPath() != null){
            target = mappingObject.getTarget().getPath().getItemPath().toString();
        }

        //TODO - get expressions and conditions from mapping
    }

    protected MappingType prepareDtoToSave(){
        //TODO - implement
        return new MappingType();
    }

    public MappingType getMappingObject() {
        return mappingObject;
    }

    public void setMappingObject(MappingType mappingObject) {
        this.mappingObject = mappingObject;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public ExpressionEvaluatorType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionEvaluatorType expressionType) {
        this.expressionType = expressionType;
    }

    public ExpressionEvaluatorType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ExpressionEvaluatorType conditionType) {
        this.conditionType = conditionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MappingTypeDto)) return false;

        MappingTypeDto that = (MappingTypeDto) o;

        if (condition != null ? !condition.equals(that.condition) : that.condition != null) return false;
        if (conditionType != that.conditionType) return false;
        if (expression != null ? !expression.equals(that.expression) : that.expression != null) return false;
        if (expressionType != that.expressionType) return false;
        if (mappingObject != null ? !mappingObject.equals(that.mappingObject) : that.mappingObject != null)
            return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mappingObject != null ? mappingObject.hashCode() : 0;
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (expressionType != null ? expressionType.hashCode() : 0);
        result = 31 * result + (conditionType != null ? conditionType.hashCode() : 0);
        return result;
    }
}
