/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.xpath;

import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.xpath.MidPointXPathFunctionResolver;
import com.evolveum.midpoint.util.xpath.functions.CapitalizeFunction;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Radovan Semancik
 */
public class XPathExpressionEvaluator implements ExpressionEvaluator {

    public static String XPATH_LANGUAGE_URL = "http://www.w3.org/TR/xpath/";

    private XPathFactory factory = XPathFactory.newInstance();

    @Override
    public <T> PrismPropertyValue<T> evaluateScalar(Class<T> type, Element code, Map<QName, Object> variables,
            ObjectResolver objectResolver, String contextDescription, OperationResult result) throws
            ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException {

        QName returnType = determineRerturnType(type);

        Object evaluatedExpression = evaluate(returnType, code, variables, objectResolver, contextDescription, result);

        PrismPropertyValue<T> propertyValue = convertScalar(type, returnType, evaluatedExpression, contextDescription);
        T value = propertyValue.getValue();
        if (value == null || ((value instanceof String) && ((String) value).isEmpty())) {
            return null;
        }

        return propertyValue;
    }

    /* (non-Javadoc)
      * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluateList(java.lang.Class, org.w3c.dom.Element, java.util.Map, com.evolveum.midpoint.schema.util.ObjectResolver, java.lang.String)
      */
    @Override
    public <T> List<PrismPropertyValue<T>> evaluateList(Class<T> type, Element code, Map<QName, Object> variables,
            ObjectResolver objectResolver, String contextDescription, OperationResult result) throws
            ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException {

        Object evaluatedExpression = evaluate(XPathConstants.NODESET, code, variables, objectResolver, contextDescription, result);

        if (!(evaluatedExpression instanceof NodeList)) {
            throw new IllegalStateException("The expression " + contextDescription + " resulted in " + evaluatedExpression.getClass().getName() + " while exprecting NodeList");
        }

        return convertList(type, (NodeList) evaluatedExpression, contextDescription);
    }


    private Object evaluate(QName returnType, Element code, Map<QName, Object> variables, ObjectResolver objectResolver,
            String contextDescription, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        XPathExpressionCodeHolder codeHolder = new XPathExpressionCodeHolder(code);

        XPath xpath = factory.newXPath();
        XPathVariableResolver variableResolver = new LazyXPathVariableResolver(variables, objectResolver, contextDescription, result);
        xpath.setXPathVariableResolver(variableResolver);
        xpath.setNamespaceContext(new MidPointNamespaceContext(codeHolder.getNamespaceMap()));
        xpath.setXPathFunctionResolver(getFunctionResolver());

        XPathExpression expr;
        try {

            expr = xpath.compile(codeHolder.getExpressionAsString());

        } catch (Exception e) {
            Throwable originalException = ExceptionUtil.lookForTunneledException(e);
            if (originalException != null && originalException instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) originalException;
            }
            if (originalException != null && originalException instanceof SchemaException) {
                throw (SchemaException) originalException;
            }
            if (e instanceof XPathExpressionException) {
                throw createExpressionEvaluationException(e, contextDescription);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new SystemException(e.getMessage(), e);
        }

        Object rootNode = determineRootNode(variableResolver);
        Object evaluatedExpression;

        try {

            evaluatedExpression = expr.evaluate(rootNode, returnType);

        } catch (Exception e) {
            Throwable originalException = ExceptionUtil.lookForTunneledException(e);
            if (originalException != null && originalException instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) originalException;
            }
            if (originalException != null && originalException instanceof SchemaException) {
                throw (SchemaException) originalException;
            }
            if (e instanceof XPathExpressionException) {
                throw createExpressionEvaluationException(e, contextDescription);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new SystemException(e.getMessage(), e);
        }

        if (evaluatedExpression == null) {
            return null;
        }

        return evaluatedExpression;
    }


    private ExpressionEvaluationException createExpressionEvaluationException(Exception e, String contextDescription) {
        return new ExpressionEvaluationException(ExceptionUtil.lookForMessage(e) + " in " + contextDescription, e);
    }

    /**
     * Kind of convenience magic. Try few obvious variables and set them as the root node
     * for evaluation. This allow to use "fullName" instead of "$user/fullName".
     */
    private Object determineRootNode(XPathVariableResolver variableResolver) {
        Object rootNode = variableResolver.resolveVariable(null);
        if (rootNode == null) {
        	// Add empty document instead of null so the expressions don't die with exception.
        	// This is necessary e.g. on deletes in sync when there may be nothing to evaluate.
        	rootNode = DOMUtil.getDocument();
        }
        return rootNode;
    }

    private QName determineRerturnType(Class<?> type) throws ExpressionEvaluationException {
        if (type.equals(String.class)) {
            return XPathConstants.STRING;
        }
        if (type.equals(Double.class)) {
            return XPathConstants.NUMBER;
        }
        if (type.equals(Integer.class)) {
            return XPathConstants.NUMBER;
        }
        if (type.equals(Boolean.class)) {
            return XPathConstants.BOOLEAN;
        }
        if (type.equals(NodeList.class)) {
            return XPathConstants.NODESET;
        }
        if (type.equals(Node.class)) {
            return XPathConstants.NODE;
        }
        throw new ExpressionEvaluationException("Unsupported return type " + type);
    }

    private <T> PrismPropertyValue<T> convertScalar(Class<T> type, QName returnType, Object value,
            String contextDescription) throws ExpressionEvaluationException {
        if (type.isAssignableFrom(value.getClass())) {
            return new PrismPropertyValue<T>((T) value);
        }
        try {
            if (value instanceof String) {
                return new PrismPropertyValue<T>(XmlTypeConverter.toJavaValue((String) value, type));
            }
            if (value instanceof Element) {
                return new PrismPropertyValue<T>(XmlTypeConverter.convertValueElementAsScalar((Element) value, type));
            }
            throw new ExpressionEvaluationException("Unexpected scalar return type " + value.getClass().getName());
        } catch (SchemaException e) {
            throw new ExpressionEvaluationException("Error converting result of "
                    + contextDescription + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Error converting result of "
                    + contextDescription + ": " + e.getMessage(), e);
        }
    }

    private <T> List<PrismPropertyValue<T>> convertList(Class<T> type, NodeList valueNodes, String contextDescription) throws
            ExpressionEvaluationException {
        List<PrismPropertyValue<T>> values = new ArrayList<PrismPropertyValue<T>>();
        if (valueNodes == null) {
            return values;
        }

        try {
            List<T> list = XmlTypeConverter.convertValueElementAsList(valueNodes, type);
            for (T item : list) {
                if (item == null || ((item instanceof String) && ((String) item).isEmpty())) {
                    continue;
                }
                values.add(new PrismPropertyValue<T>(item));
            }
            return values;
        } catch (SchemaException e) {
            throw new ExpressionEvaluationException("Error converting return value of " + contextDescription + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Error converting return value of " + contextDescription + ": " + e.getMessage(), e);
        }
    }

    private MidPointXPathFunctionResolver getFunctionResolver() {
        MidPointXPathFunctionResolver resolver = new MidPointXPathFunctionResolver();
        resolver.registerFunction(new QName("http://midpoint.evolveum.com/custom", "capitalize"), new CapitalizeFunction());
        return resolver;
    }

    @Override
    public String getLanguageName() {
        return "XPath 2.0";
    }

}
