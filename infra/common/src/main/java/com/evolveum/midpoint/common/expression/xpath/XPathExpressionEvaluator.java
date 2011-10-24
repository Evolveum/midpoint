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
package com.evolveum.midpoint.common.expression.xpath;

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

import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
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
	public <T> T evaluate(Class<T> type, Element code, Map<QName, Object> variables, ObjectResolver objectResolver, String contextDescription) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		XPathExpressionCodeHolder codeHolder = new XPathExpressionCodeHolder(code);
		
		XPath xpath = factory.newXPath();
		XPathVariableResolver variableResolver = new LazyXPathVariableResolver(variables, objectResolver, contextDescription);
        xpath.setXPathVariableResolver(variableResolver);
        xpath.setNamespaceContext(new MidPointNamespaceContext(codeHolder.getNamespaceMap()));
        xpath.setXPathFunctionResolver(getFunctionResolver());
        
        XPathExpression expr;
		try {
			
			expr = xpath.compile(codeHolder.getExpressionAsString());
			
		} catch (Exception e) {
			Throwable originalException = ExceptionUtil.lookForTunneledException(e);
			if (originalException != null && originalException instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)originalException;
			}
			if (originalException != null && originalException instanceof SchemaException) {
				throw (SchemaException)originalException;
			}
			if (e instanceof XPathExpressionException) {
				throw createExpressionEvaluationException(e);
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new SystemException(e.getMessage(),e);
		}
		
        Object rootNode = determineRootNode(variableResolver);
        QName returnType = determineRerturnType(type);
        Object evaluatedExpression;
        
		try {
			
			evaluatedExpression = expr.evaluate(rootNode, returnType);
			
		} catch (Exception e) {
			Throwable originalException = ExceptionUtil.lookForTunneledException(e);
			if (originalException != null && originalException instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)originalException;
			}
			if (originalException != null && originalException instanceof SchemaException) {
				throw (SchemaException)originalException;
			}
			if (e instanceof XPathExpressionException) {
				throw createExpressionEvaluationException(e);
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new SystemException(e.getMessage(),e);
		}
        
        if (evaluatedExpression == null) {
        	return null;
        }
        
        if (!type.isAssignableFrom(evaluatedExpression.getClass())) {
        	throw new ExpressionEvaluationException("Expression returned incompatible type "+evaluatedExpression.getClass());
        }
        
        return (T)evaluatedExpression;
	}


	private ExpressionEvaluationException createExpressionEvaluationException(Exception e) {
		return new ExpressionEvaluationException(ExceptionUtil.lookForMessage(e),e);
	}

	/**
	 * Kind of convenience magic. Try few obvious variables and set them as the root node
	 * for evaluation. This allow to use "fullName" instead of "$user/fullName".
	 */
	private Object determineRootNode(XPathVariableResolver variableResolver) {
		return variableResolver.resolveVariable(null);
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
