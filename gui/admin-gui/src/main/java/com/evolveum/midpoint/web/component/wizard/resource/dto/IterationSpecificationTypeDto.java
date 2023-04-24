/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import jakarta.xml.bind.JAXBElement;
import java.io.Serializable;

/**
 *  @author shood
 * */
public class IterationSpecificationTypeDto implements Serializable{

    private static final Trace LOGGER = TraceManager.getTrace(IterationSpecificationTypeDto.class);

    public static final String TOKEN_EXPRESSION_PREFIX = "token";
    public static final String PRE_EXPRESSION_PREFIX = "pre";
    public static final String POST_EXPRESSION_PREFIX = "post";

    public static final String F_EXPRESSION_TYPE = "ExpressionType";
    public static final String F_LANGUAGE = "Language";
    public static final String F_POLICY_REF = "PolicyRef";
    public static final String F_EXPRESSION = "Expression";
    public static final String F_ITERATION = "iterationObject";

    private ExpressionUtil.ExpressionEvaluatorType tokenExpressionType;
    private ExpressionUtil.ExpressionEvaluatorType preExpressionType;
    private ExpressionUtil.ExpressionEvaluatorType postExpressionType;
    private ExpressionUtil.Language tokenLanguage;
    private ExpressionUtil.Language preLanguage;
    private ExpressionUtil.Language postLanguage;
    private ObjectReferenceType tokenPolicyRef;
    private ObjectReferenceType prePolicyRef;
    private ObjectReferenceType postPolicyRef;
    private String tokenExpression;
    private String preExpression;
    private String postExpression;
    private IterationSpecificationType iterationObject;

    public IterationSpecificationTypeDto(IterationSpecificationType iteration){
        iterationObject = iteration;
    }

    public IterationSpecificationTypeDto(IterationSpecificationType iteration, PrismContext prismContext){
        iterationObject = iteration;

        if(!iterationObject.getTokenExpression().getExpressionEvaluator().isEmpty()){
            loadExpression(tokenExpression, tokenExpressionType, tokenLanguage, tokenPolicyRef,
                    iterationObject.getTokenExpression(), prismContext);
        }

        if(!iterationObject.getPreIterationCondition().getExpressionEvaluator().isEmpty()){
            loadExpression(preExpression, preExpressionType, preLanguage, prePolicyRef,
                    iterationObject.getPreIterationCondition(), prismContext);
        }

        if(!iterationObject.getPostIterationCondition().getExpressionEvaluator().isEmpty()){
            loadExpression(postExpression, postExpressionType, postLanguage, postPolicyRef,
                    iterationObject.getPostIterationCondition(), prismContext);
        }
    }

    private void loadExpression(String expression, ExpressionUtil.ExpressionEvaluatorType type, ExpressionUtil.Language language,
                                ObjectReferenceType ref, ExpressionType expressionType, PrismContext prismContext){

        try{
            if(expressionType.getExpressionEvaluator().size() == 1){
                expression = prismContext.xmlSerializer().serialize(expressionType.getExpressionEvaluator().get(0));
            } else {
                StringBuilder sb = new StringBuilder();
                for(JAXBElement<?> element: expressionType.getExpressionEvaluator()){
                    String subElement = prismContext.xmlSerializer().serialize(element);
                    sb.append(subElement).append("\n");
                }

                expression = sb.toString();
            }

            type = ExpressionUtil.getExpressionType(expression);
            if(type != null && type.equals(ExpressionUtil.ExpressionEvaluatorType.SCRIPT)){
                language = ExpressionUtil.getExpressionLanguage(expression);
            }

        } catch (SchemaException e){
            //TODO - how can we show this error to user?
            LoggingUtils.logUnexpectedException(LOGGER, "Could not load expressions from mapping.", e, e.getStackTrace());
            expression = e.getMessage();
        }
    }

    public void updateExpression(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            tokenExpression = ExpressionUtil.getExpressionString(tokenExpressionType);
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            preExpression = ExpressionUtil.getExpressionString(preExpressionType);
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            postExpression = ExpressionUtil.getExpressionString(postExpressionType);
        }
    }

    public void updateExpressionLanguage(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            tokenExpression = ExpressionUtil.getExpressionString(tokenExpressionType, tokenLanguage);
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            preExpression = ExpressionUtil.getExpressionString(preExpressionType, preLanguage);
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            postExpression = ExpressionUtil.getExpressionString(postExpressionType, postLanguage);
        }
    }

    public void updateExpressionPolicy(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            tokenExpression = ExpressionUtil.getExpressionString(tokenExpressionType, tokenPolicyRef);
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            preExpression = ExpressionUtil.getExpressionString(preExpressionType, prePolicyRef);
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            postExpression = ExpressionUtil.getExpressionString(postExpressionType, postPolicyRef);
        }
    }

    public ExpressionUtil.Language getLanguage(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            return tokenLanguage;
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            return preLanguage;
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            return postLanguage;
        }

        return null;
    }

    public ExpressionUtil.ExpressionEvaluatorType getExpressionType(String prefix){
        if(prefix.equals(TOKEN_EXPRESSION_PREFIX)){
            return tokenExpressionType;
        } else if(prefix.equals(PRE_EXPRESSION_PREFIX)){
            return preExpressionType;
        } else if(prefix.equals(POST_EXPRESSION_PREFIX)){
            return postExpressionType;
        }

        return null;
    }

    public ExpressionUtil.ExpressionEvaluatorType getTokenExpressionType() {
        return tokenExpressionType;
    }

    public void setTokenExpressionType(ExpressionUtil.ExpressionEvaluatorType tokenExpressionType) {
        this.tokenExpressionType = tokenExpressionType;
    }

    public ExpressionUtil.ExpressionEvaluatorType getPreExpressionType() {
        return preExpressionType;
    }

    public void setPreExpressionType(ExpressionUtil.ExpressionEvaluatorType preExpressionType) {
        this.preExpressionType = preExpressionType;
    }

    public ExpressionUtil.ExpressionEvaluatorType getPostExpressionType() {
        return postExpressionType;
    }

    public void setPostExpressionType(ExpressionUtil.ExpressionEvaluatorType postExpressionType) {
        this.postExpressionType = postExpressionType;
    }

    public ExpressionUtil.Language getTokenLanguage() {
        return tokenLanguage;
    }

    public void setTokenLanguage(ExpressionUtil.Language tokenLanguage) {
        this.tokenLanguage = tokenLanguage;
    }

    public ExpressionUtil.Language getPreLanguage() {
        return preLanguage;
    }

    public void setPreLanguage(ExpressionUtil.Language preLanguage) {
        this.preLanguage = preLanguage;
    }

    public ExpressionUtil.Language getPostLanguage() {
        return postLanguage;
    }

    public void setPostLanguage(ExpressionUtil.Language postLanguage) {
        this.postLanguage = postLanguage;
    }

    public ObjectReferenceType getTokenPolicyRef() {
        return tokenPolicyRef;
    }

    public void setTokenPolicyRef(ObjectReferenceType tokenPolicyRef) {
        this.tokenPolicyRef = tokenPolicyRef;
    }

    public ObjectReferenceType getPrePolicyRef() {
        return prePolicyRef;
    }

    public void setPrePolicyRef(ObjectReferenceType prePolicyRef) {
        this.prePolicyRef = prePolicyRef;
    }

    public ObjectReferenceType getPostPolicyRef() {
        return postPolicyRef;
    }

    public void setPostPolicyRef(ObjectReferenceType postPolicyRef) {
        this.postPolicyRef = postPolicyRef;
    }

    public String getTokenExpression() {
        return tokenExpression;
    }

    public void setTokenExpression(String tokenExpression) {
        this.tokenExpression = tokenExpression;
    }

    public String getPreExpression() {
        return preExpression;
    }

    public void setPreExpression(String preExpression) {
        this.preExpression = preExpression;
    }

    public String getPostExpression() {
        return postExpression;
    }

    public void setPostExpression(String postExpression) {
        this.postExpression = postExpression;
    }

    public IterationSpecificationType getIterationObject() {
        return iterationObject;
    }

    public void setIterationObject(IterationSpecificationType iterationObject) {
        this.iterationObject = iterationObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IterationSpecificationTypeDto that = (IterationSpecificationTypeDto) o;

        if (iterationObject != null ? !iterationObject.equals(that.iterationObject) : that.iterationObject != null)
            return false;
        if (postExpression != null ? !postExpression.equals(that.postExpression) : that.postExpression != null)
            return false;
        if (postExpressionType != that.postExpressionType) return false;
        if (postLanguage != that.postLanguage) return false;
        if (postPolicyRef != null ? !postPolicyRef.equals(that.postPolicyRef) : that.postPolicyRef != null)
            return false;
        if (preExpression != null ? !preExpression.equals(that.preExpression) : that.preExpression != null)
            return false;
        if (preExpressionType != that.preExpressionType) return false;
        if (preLanguage != that.preLanguage) return false;
        if (prePolicyRef != null ? !prePolicyRef.equals(that.prePolicyRef) : that.prePolicyRef != null) return false;
        if (tokenExpression != null ? !tokenExpression.equals(that.tokenExpression) : that.tokenExpression != null)
            return false;
        if (tokenExpressionType != that.tokenExpressionType) return false;
        if (tokenLanguage != that.tokenLanguage) return false;
        if (tokenPolicyRef != null ? !tokenPolicyRef.equals(that.tokenPolicyRef) : that.tokenPolicyRef != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tokenExpressionType != null ? tokenExpressionType.hashCode() : 0;
        result = 31 * result + (preExpressionType != null ? preExpressionType.hashCode() : 0);
        result = 31 * result + (postExpressionType != null ? postExpressionType.hashCode() : 0);
        result = 31 * result + (tokenLanguage != null ? tokenLanguage.hashCode() : 0);
        result = 31 * result + (preLanguage != null ? preLanguage.hashCode() : 0);
        result = 31 * result + (postLanguage != null ? postLanguage.hashCode() : 0);
        result = 31 * result + (tokenPolicyRef != null ? tokenPolicyRef.hashCode() : 0);
        result = 31 * result + (prePolicyRef != null ? prePolicyRef.hashCode() : 0);
        result = 31 * result + (postPolicyRef != null ? postPolicyRef.hashCode() : 0);
        result = 31 * result + (tokenExpression != null ? tokenExpression.hashCode() : 0);
        result = 31 * result + (preExpression != null ? preExpression.hashCode() : 0);
        result = 31 * result + (postExpression != null ? postExpression.hashCode() : 0);
        result = 31 * result + (iterationObject != null ? iterationObject.hashCode() : 0);
        return result;
    }
}
