/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

/**
 *
 * @author lazyman
 *
 * TODO is this class ever used?
 */
@Component
public class ExpressionHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionHandler.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private PrismContext prismContext;

    public String evaluateExpression(ShadowType shadow, ExpressionType expressionType,
            String shortDesc, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        Validate.notNull(shadow, "Resource object shadow must not be null.");
        Validate.notNull(expressionType, "Expression must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        ResourceType resource = resolveResource(shadow, result);

        VariablesMap variables = getDefaultXPathVariables(null, shadow, resource);

        PrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
                DOMUtil.XSD_STRING);
        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType,
                outputDefinition, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);

        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
        if (outputTriple == null) {
            return null;
        }
        Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues.isEmpty()) {
            return null;
        }
        if (nonNegativeValues.size() > 1) {
            throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        return nonNegativeValues.iterator().next().getValue();
    }

    public boolean evaluateConfirmationExpression(UserType user, ShadowType shadow,
            ExpressionType expressionType, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        Validate.notNull(user, "User must not be null.");
        Validate.notNull(shadow, "Resource object shadow must not be null.");
        Validate.notNull(expressionType, "Expression must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        ResourceType resource = resolveResource(shadow, result);
        VariablesMap variables = getDefaultXPathVariables(user, shadow, resource);
        String shortDesc = "confirmation expression for "+resource.asPrismObject();

        PrismPropertyDefinition<Boolean> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
                DOMUtil.XSD_BOOLEAN);
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType,
                outputDefinition, expressionProfile, shortDesc, task, result);

        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
        Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
            throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        if (nonNegativeValues.size() > 1) {
            throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        PrismPropertyValue<Boolean> resultpval = nonNegativeValues.iterator().next();
        if (resultpval == null) {
            throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        Boolean resultVal = resultpval.getValue();
        if (resultVal == null) {
            throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        return resultVal;
    }

    // TODO: refactor - this method is also in SchemaHandlerImpl
    private ResourceType resolveResource(ShadowType shadow, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        ObjectReferenceType ref = shadow.getResourceRef();
        if (ref == null) {
            throw new ExpressionEvaluationException("Resource shadow object " + shadow + " doesn't have defined resource.");
        }
        PrismObject<ResourceType> resource = ref.getObject();
        if (resource != null) {
            return resource.asObjectable();
        }
        if (ref.getOid() == null) {
            throw new ExpressionEvaluationException("Resource shadow object " + shadow + " defines null resource OID.");
        }

        return modelObjectResolver.getObjectSimple(ResourceType.class, ref.getOid(), null, null, result);
    }

    public static VariablesMap getDefaultXPathVariables(UserType user,
            ShadowType shadow, ResourceType resource) {

        VariablesMap variables = new VariablesMap();
        if (user != null) {
            variables.put(ExpressionConstants.VAR_FOCUS, user.asPrismObject(), user.asPrismObject().getDefinition());
            variables.put(ExpressionConstants.VAR_USER, user.asPrismObject(), user.asPrismObject().getDefinition());
        }

        if (shadow != null) {
            variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadow.asPrismObject(), shadow.asPrismObject().getDefinition());
            variables.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadow.asPrismObject(), shadow.asPrismObject().getDefinition());
        }

        if (resource != null) {
            variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource.asPrismObject(), resource.asPrismObject().getDefinition());
        }

        return variables;
    }

    // Called from the ObjectResolver.resolve
    public ObjectType resolveRef(ObjectReferenceType ref, String contextDescription, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        Class<? extends ObjectType> type;
        if (ref.getType() != null) {
            ObjectTypes objectTypeType = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
            type = objectTypeType.getClassDefinition();
        } else {
            type = ObjectType.class;
        }

        return repositoryService.getObject(type, ref.getOid(), createReadOnlyCollection(), result).asObjectable();

    }

}
