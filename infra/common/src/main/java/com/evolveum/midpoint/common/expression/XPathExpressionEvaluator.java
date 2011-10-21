/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.xpath.MidPointNamespaceContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MapXPathVariableResolver;
import com.evolveum.midpoint.util.xpath.MidPointXPathFunctionResolver;
import com.evolveum.midpoint.util.xpath.functions.CapitalizeFunction;

/**
 * @author Radovan Semancik
 *
 */
public class XPathExpressionEvaluator implements ExpressionEvaluator {
	
	public static String XPATH_LANGUAGE_URL = "http://www.w3.org/TR/xpath/";

	private XPathFactory factory = XPathFactory.newInstance();
	
	@Override
	public <T> T evaluate(Class<T> type, Element code, Map<QName, Object> variables) throws ExpressionEvaluationException {
		
		XPathExpressionCodeHolder codeHolder = new XPathExpressionCodeHolder(code);
		
		XPath xpath = factory.newXPath();
        xpath.setXPathVariableResolver(new MapXPathVariableResolver(variables));
        xpath.setNamespaceContext(new MidPointNamespaceContext(codeHolder.getNamespaceMap()));
        xpath.setXPathFunctionResolver(getFunctionResolver());
        
        XPathExpression expr;
		try {
			expr = xpath.compile(codeHolder.getExpressionAsString());
		} catch (XPathExpressionException e) {
			throw new ExpressionEvaluationException(e.getMessage(),e);
		}
        Node rootNode = determineRootNode(variables);
        QName returnType = determineRerturnType(type);
        Object evaluatedExpression;
		try {
			evaluatedExpression = expr.evaluate(rootNode, returnType);
		} catch (XPathExpressionException e) {
			throw new ExpressionEvaluationException(e.getMessage(),e);
		}
        
        if (evaluatedExpression == null) {
        	return null;
        }
        
        if (!type.isAssignableFrom(evaluatedExpression.getClass())) {
        	throw new ExpressionEvaluationException("Expression returned incompatible type "+evaluatedExpression.getClass());
        }
        
        return (T)evaluatedExpression;
	}

	/**
	 * Kind of convenience magic. Try few obvious variables and set them as the root node
	 * for evaluation. This allow to use "fullName" instead of "$user/fullName".
	 */
	private Node determineRootNode(Map<QName, Object> variables) {
		if (variables.containsKey(SchemaConstants.I_USER)) {
			return (Node)variables.get(SchemaConstants.I_USER);
		}
		if (variables.containsKey(SchemaConstants.I_ACCOUNT)) {
			return (Node)variables.get(SchemaConstants.I_ACCOUNT);
		}
		// Otherwise an empty document is just fine
		return DOMUtil.getDocument();
	}
	
	private QName determineRerturnType(Class<?> type) throws ExpressionEvaluationException {
		if (type.equals(String.class)) {
			return XPathConstants.STRING;
		}
		if (type.equals(Double.class)) {
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
		throw new ExpressionEvaluationException("Unsupported return type "+type);
	}

	private MidPointXPathFunctionResolver getFunctionResolver() {
		MidPointXPathFunctionResolver resolver = new MidPointXPathFunctionResolver();
		resolver.registerFunction(new QName("http://midpoint.evolveum.com/custom", "capitalize"), new CapitalizeFunction());
		return resolver;
	}

}
