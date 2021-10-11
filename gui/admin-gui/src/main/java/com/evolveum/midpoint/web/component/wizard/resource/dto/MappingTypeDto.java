/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class MappingTypeDto implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(MappingTypeDto.class);

    public static final String F_MAPPING = "mappingObject";
    public static final String F_EXPRESSION = "expression";
    public static final String F_CONDITION = "condition";
    public static final String F_TARGET = "target";
    public static final String F_SOURCE = "source";
    public static final String F_EXPRESSION_TYPE = "expressionType";
    public static final String F_CONDITION_TYPE = "conditionType";
    public static final String F_EXPRESSION_LANG = "expressionLanguage";
    public static final String F_CONDITION_LANG = "conditionLanguage";
    public static final String F_EXPRESSION_POLICY_REF = "expressionPolicyRef";
    public static final String F_CONDITION_POLICY_REF = "conditionPolicyRef";

    private static final MappingStrengthType DEFAULT_MAPPING_STRENGTH = MappingStrengthType.NORMAL;

    private MappingType mappingObject;
    private MappingType oldMappingObject;
    private String expression;
    private String condition;
    private String target;
    private List<String> source = new ArrayList<>();
    private ExpressionUtil.ExpressionEvaluatorType expressionType = null;
    private ExpressionUtil.ExpressionEvaluatorType conditionType = null;
    private ExpressionUtil.Language expressionLanguage = ExpressionUtil.Language.GROOVY;
    private ExpressionUtil.Language conditionLanguage = ExpressionUtil.Language.GROOVY;
    private ObjectReferenceType expressionPolicyRef = null;
    private ObjectReferenceType conditionPolicyRef = null;

    public MappingTypeDto(MappingType mapping, PrismContext prismContext){

        if(mapping != null && mapping.equals(new MappingType())){
            mappingObject = mapping;
            expression = ExpressionUtil.EXPRESSION_AS_IS;
            expressionType = ExpressionUtil.ExpressionEvaluatorType.AS_IS;
        }

        if(mapping == null){
            MappingType newMapping = new MappingType();
            newMapping.setAuthoritative(true);
            mappingObject = newMapping;
        } else {
            mappingObject = mapping;
        }

        oldMappingObject = mappingObject.clone();

        for(VariableBindingDefinitionType mappingSource: mappingObject.getSource()){
            if(mappingSource.getPath() != null) {
                source.add(mappingSource.getPath().getItemPath().toString());
            }
        }

        if(mappingObject.getTarget() != null && mappingObject.getTarget().getPath() != null) {
            target = mappingObject.getTarget().getPath().getItemPath().toString();
        }

        if(mappingObject.getStrength() == null){
            mappingObject.setStrength(DEFAULT_MAPPING_STRENGTH);
        }

        loadExpressions(prismContext);
        loadConditions(prismContext);
    }

    private void loadExpressions(PrismContext context) {
        expression = ExpressionUtil.loadExpression(mappingObject.getExpression(), context, LOGGER);

        expressionType = ExpressionUtil.getExpressionType(expression);
        if (expressionType != null && expressionType.equals(ExpressionUtil.ExpressionEvaluatorType.SCRIPT)) {
            expressionLanguage = ExpressionUtil.getExpressionLanguage(expression);
        }
    }

    private void loadConditions(PrismContext context) {
        condition = ExpressionUtil.loadExpression(mappingObject.getCondition(), context, LOGGER);

        conditionType = ExpressionUtil.getExpressionType(condition);
        if (conditionType != null && conditionType.equals(ExpressionUtil.ExpressionEvaluatorType.SCRIPT)) {
            conditionLanguage = ExpressionUtil.getExpressionLanguage(condition);
        }
    }

    public void cancelChanges() {
        mappingObject.setName(oldMappingObject.getName());
        mappingObject.setDescription(oldMappingObject.getDescription());
        mappingObject.setAuthoritative(oldMappingObject.isAuthoritative());
        mappingObject.setExclusive(oldMappingObject.isExclusive());
        mappingObject.setStrength(oldMappingObject.getStrength());
        mappingObject.getChannel().clear();
        mappingObject.getChannel().addAll(oldMappingObject.getChannel());
        mappingObject.getExceptChannel().clear();
        mappingObject.getExceptChannel().addAll(oldMappingObject.getExceptChannel());
    }

    public MappingType prepareDtoToSave(PrismContext prismContext) throws SchemaException{

        if(mappingObject == null){
            mappingObject = new MappingType();
        }

        if(target != null){
            VariableBindingDefinitionType mappingTarget = new VariableBindingDefinitionType();
            mappingTarget.setPath(prismContext.itemPathParser().asItemPathType(target));
            mappingObject.setTarget(mappingTarget);
        } else {
            mappingObject.setTarget(null);
        }

        mappingObject.getSource().clear();
        List<VariableBindingDefinitionType> mappingSourceList = new ArrayList<>();
        for(String s: source){
            if(s == null){
                continue;
            }

            VariableBindingDefinitionType mappingSource = new VariableBindingDefinitionType();
            mappingSource.setPath(prismContext.itemPathParser().asItemPathType(s));
            mappingSourceList.add(mappingSource);
        }

        mappingObject.getSource().addAll(mappingSourceList);

        if (expression != null) {
            if(mappingObject.getExpression() == null){
                mappingObject.setExpression(new ExpressionType());
            }
            ExpressionUtil.parseExpressionEvaluators(expression, mappingObject.getExpression(), prismContext);
        }

        if (condition != null) {
            if (mappingObject.getCondition() == null) {
                mappingObject.setCondition(new ExpressionType());
            }
            ExpressionUtil.parseExpressionEvaluators(condition, mappingObject.getCondition(), prismContext);
        }

        return mappingObject;
    }

    public void updateExpressionGeneratePolicy(){
        expression = ExpressionUtil.getExpressionString(expressionType, expressionPolicyRef);
    }

    public void updateConditionGeneratePolicy(){
        condition = ExpressionUtil.getExpressionString(expressionType, conditionPolicyRef);
    }

    public void updateExpressionLanguage(){
        expression = ExpressionUtil.getExpressionString(expressionType, expressionLanguage);
    }

    public void updateConditionLanguage(){
        condition = ExpressionUtil.getExpressionString(conditionType, conditionLanguage);
    }

    public void updateExpression(){
        expression = ExpressionUtil.getExpressionString(expressionType);
    }

    public void updateCondition(){
        condition = ExpressionUtil.getExpressionString(conditionType);
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

    public ExpressionUtil.ExpressionEvaluatorType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionUtil.ExpressionEvaluatorType expressionType) {
        this.expressionType = expressionType;
    }

    public ExpressionUtil.ExpressionEvaluatorType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ExpressionUtil.ExpressionEvaluatorType conditionType) {
        this.conditionType = conditionType;
    }

    public ExpressionUtil.Language getExpressionLanguage() {
        return expressionLanguage;
    }

    public void setExpressionLanguage(ExpressionUtil.Language expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    public ExpressionUtil.Language getConditionLanguage() {
        return conditionLanguage;
    }

    public void setConditionLanguage(ExpressionUtil.Language conditionLanguage) {
        this.conditionLanguage = conditionLanguage;
    }

    public ObjectReferenceType getExpressionPolicyRef() {
        return expressionPolicyRef;
    }

    public void setExpressionPolicyRef(ObjectReferenceType expressionPolicyRef) {
        this.expressionPolicyRef = expressionPolicyRef;
    }

    public ObjectReferenceType getConditionPolicyRef() {
        return conditionPolicyRef;
    }

    public void setConditionPolicyRef(ObjectReferenceType conditionPolicyRef) {
        this.conditionPolicyRef = conditionPolicyRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MappingTypeDto)) return false;

        MappingTypeDto that = (MappingTypeDto) o;

        if (condition != null ? !condition.equals(that.condition) : that.condition != null) return false;
        if (conditionLanguage != that.conditionLanguage) return false;
        if (conditionPolicyRef != null ? !conditionPolicyRef.equals(that.conditionPolicyRef) : that.conditionPolicyRef != null)
            return false;
        if (conditionType != that.conditionType) return false;
        if (expression != null ? !expression.equals(that.expression) : that.expression != null) return false;
        if (expressionLanguage != that.expressionLanguage) return false;
        if (expressionPolicyRef != null ? !expressionPolicyRef.equals(that.expressionPolicyRef) : that.expressionPolicyRef != null)
            return false;
        if (expressionType != that.expressionType) return false;
        if (mappingObject != null ? !mappingObject.equals(that.mappingObject) : that.mappingObject != null)
            return false;
        if (oldMappingObject != null ? !oldMappingObject.equals(that.oldMappingObject) : that.oldMappingObject != null)
            return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mappingObject != null ? mappingObject.hashCode() : 0;
        result = 31 * result + (oldMappingObject != null ? oldMappingObject.hashCode() : 0);
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (expressionType != null ? expressionType.hashCode() : 0);
        result = 31 * result + (conditionType != null ? conditionType.hashCode() : 0);
        result = 31 * result + (expressionLanguage != null ? expressionLanguage.hashCode() : 0);
        result = 31 * result + (conditionLanguage != null ? conditionLanguage.hashCode() : 0);
        result = 31 * result + (expressionPolicyRef != null ? expressionPolicyRef.hashCode() : 0);
        result = 31 * result + (conditionPolicyRef != null ? conditionPolicyRef.hashCode() : 0);
        return result;
    }

    public static String createMappingLabel(MappingType mapping, Trace LOGGER, PrismContext context,
                                            String placeholder, String nameNotSpecified ){
        if(mapping == null){
            return placeholder;
        }

        StringBuilder sb = new StringBuilder();
        if(mapping.getName() != null && StringUtils.isNotEmpty(mapping.getName())){
            sb.append(mapping.getName());
            return sb.toString();
        }

        if (!mapping.getSource().isEmpty()) {
            for (VariableBindingDefinitionType source: mapping.getSource()) {
                if (source.getPath() != null && !ItemPath.isEmpty(source.getPath().getItemPath())) {
                    sb.append(source.getPath().getItemPath().last());
                    sb.append(",");
                }
            }
        }

        sb.append("-");
        sb.append(" (");
        if(mapping.getExpression() != null && mapping.getExpression().getExpressionEvaluator() != null){
            sb.append(ExpressionUtil.getExpressionType(ExpressionUtil.loadExpression(mapping.getExpression(), context, LOGGER)));
        }
        sb.append(")");
        sb.append("->");

        if (mapping.getTarget() != null) {
            VariableBindingDefinitionType target = mapping.getTarget();
            if (target.getPath() != null && !ItemPath.isEmpty(target.getPath().getItemPath())) {
                sb.append(target.getPath().getItemPath().last());
            }
        }

        return sb.toString();
    }
}
