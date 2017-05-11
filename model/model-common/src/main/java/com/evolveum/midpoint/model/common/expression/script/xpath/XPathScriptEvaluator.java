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
package com.evolveum.midpoint.model.common.expression.script.xpath;

import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Radovan Semancik
 */
public class XPathScriptEvaluator implements ScriptEvaluator {

    public static String XPATH_LANGUAGE_URL = "http://www.w3.org/TR/xpath/";

    private XPathFactory factory = XPathFactory.newInstance();
    
    private PrismContext prismContext;

    public XPathScriptEvaluator(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

    @Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType,
			ExpressionVariables variables, ItemDefinition outputDefinition,
			Function<Object, Object> additionalConvertor,
			ScriptExpressionReturnTypeType suggestedReturnType,
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {

    	String codeString = expressionType.getCode();
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}

        Class<T> type = null;

        if (outputDefinition != null) {
		    QName xsdReturnType = outputDefinition.getTypeName();
            type = XsdTypeMapper.toJavaType(xsdReturnType);         // may return null if unknown
        }
        if (type == null) {
        	type = (Class<T>) Element.class;                        // actually, if outputDefinition is null, the return value is of no interest for us
        }
		
        QName returnType = determineRerturnType(type, expressionType, outputDefinition, suggestedReturnType);

        Object evaluatedExpression = evaluate(returnType, codeString, variables, objectResolver, functions,
        		contextDescription, result);

        List<V> propertyValues;
        
        boolean scalar = !outputDefinition.isMultiValue();
        if (expressionType.getReturnType() != null) {
        	scalar = isScalar(expressionType.getReturnType());
        } else if (suggestedReturnType != null) {
        	scalar = isScalar(suggestedReturnType);
        }
        
        if (scalar) {
        	if (evaluatedExpression instanceof NodeList) {
        		NodeList evaluatedExpressionNodeList = (NodeList)evaluatedExpression;
        		if (evaluatedExpressionNodeList.getLength() > 1) {
        			throw new ExpressionEvaluationException("Expected scalar expression result but got a list result with "+evaluatedExpressionNodeList.getLength()+" elements in "+contextDescription);
        		}
        		if (evaluatedExpressionNodeList.getLength() == 0) {
        			evaluatedExpression = null;
        		} else {
        			evaluatedExpression = evaluatedExpressionNodeList.item(0);
        		}
        	}
        	propertyValues = new ArrayList<V>(1);
        	V pval = convertScalar(type, returnType, evaluatedExpression, contextDescription);
        	if (pval instanceof PrismPropertyValue && !isNothing(((PrismPropertyValue<T>)pval).getValue())) {
        		propertyValues.add(pval);
        	}
        } else {
        	if (!(evaluatedExpression instanceof NodeList)) {
                throw new IllegalStateException("The expression " + contextDescription + " resulted in " + evaluatedExpression.getClass().getName() + " while exprecting NodeList in "+contextDescription);
            }
        	propertyValues = convertList(type, (NodeList) evaluatedExpression, contextDescription);
        }
        
        return (List<V>) PrismValue.cloneCollection(propertyValues);
    }

	private boolean isScalar(ScriptExpressionReturnTypeType returnType) {
		if (returnType == ScriptExpressionReturnTypeType.SCALAR) {
    		return true;
    	} else {
    		return false;
    	}
	}

	private Object evaluate(QName returnType, String code, ExpressionVariables variables, ObjectResolver objectResolver,
			Collection<FunctionLibrary> functions, 
    		String contextDescription, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException {

        XPathExpressionCodeHolder codeHolder = new XPathExpressionCodeHolder(code);
        //System.out.println("code " + code);
        XPath xpath = factory.newXPath();
        XPathVariableResolver variableResolver = new LazyXPathVariableResolver(variables, objectResolver, 
        		contextDescription, prismContext, result);
        xpath.setXPathVariableResolver(variableResolver);
        xpath.setNamespaceContext(new MidPointNamespaceContext(codeHolder.getNamespaceMap()));
        xpath.setXPathFunctionResolver(getFunctionResolver(functions));

        XPathExpression expr;
        try {

            expr = xpath.compile(codeHolder.getExpressionAsString());

        } catch (Exception e) {
            Throwable originalException = ExceptionUtil.lookForTunneledException(e);
            if (originalException != null && originalException instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) originalException;
            }
            if (originalException != null && originalException instanceof ExpressionSyntaxException) {
                throw (ExpressionSyntaxException) originalException;
            }
            if (e instanceof XPathExpressionException) {
                throw createExpressionEvaluationException(e, contextDescription);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new SystemException(e.getMessage(), e);
        }

        Object rootNode;
		try {
			rootNode = determineRootNode(variableResolver, contextDescription);
		} catch (SchemaException e) {
			throw new ExpressionSyntaxException(e.getMessage(), e);
		}
        Object evaluatedExpression;

        try {

            evaluatedExpression = expr.evaluate(rootNode, returnType);

        } catch (Exception e) {
            Throwable originalException = ExceptionUtil.lookForTunneledException(e);
            if (originalException != null && originalException instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) originalException;
            }
            if (originalException != null && originalException instanceof ExpressionSyntaxException) {
                throw (ExpressionSyntaxException) originalException;
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
        return new ExpressionEvaluationException(lookForMessage(e) + " in " + contextDescription, e);
    }
    
    public static String lookForMessage(Throwable e) {
    	// the net.sf.saxon.trans.XPathException lies. It has meaningless message. skip it.
    	if (e instanceof net.sf.saxon.trans.XPathException && e.getCause() != null) {
    		return lookForMessage(e.getCause());
    	}
		if (e.getMessage() != null) {
			return e.getMessage();
		}
		if (e.getCause() != null) {
			return lookForMessage(e.getCause());
		}
		return null;
	}

    /**
     * Kind of convenience magic. Try few obvious variables and set them as the root node
     * for evaluation. This allow to use "fullName" instead of "$user/fullName".
     */
    private Object determineRootNode(XPathVariableResolver variableResolver, String contextDescription) throws SchemaException {
        Object rootNode = variableResolver.resolveVariable(null);
        if (rootNode == null) {
        	// Add empty document instead of null so the expressions don't die with exception.
        	// This is necessary e.g. on deletes in sync when there may be nothing to evaluate.
        	return DOMUtil.getDocument();
        } else {
        	return LazyXPathVariableResolver.convertToXml(rootNode, null, prismContext, contextDescription);
        }
    }

	private <T> QName determineRerturnType(Class<T> type, ScriptExpressionEvaluatorType expressionType,
			ItemDefinition outputDefinition, ScriptExpressionReturnTypeType suggestedReturnType) throws ExpressionEvaluationException {

		if (expressionType.getReturnType() == ScriptExpressionReturnTypeType.LIST || suggestedReturnType == ScriptExpressionReturnTypeType.LIST) {
			return XPathConstants.NODESET;
		}
		
		if (expressionType.getReturnType() == ScriptExpressionReturnTypeType.SCALAR) {
			return toXPathReturnType(outputDefinition.getTypeName());
		}
		
		if (suggestedReturnType == ScriptExpressionReturnTypeType.LIST) {
			return XPathConstants.NODESET;
		}
		
		if (suggestedReturnType == ScriptExpressionReturnTypeType.SCALAR) {
			return toXPathReturnType(outputDefinition.getTypeName());
		}
		
		if (outputDefinition.isMultiValue()) {
			return XPathConstants.NODESET;
		} else {
			return toXPathReturnType(outputDefinition.getTypeName());
		}
	}

	private QName toXPathReturnType(QName xsdTypeName) throws ExpressionEvaluationException {
		if (xsdTypeName.equals(DOMUtil.XSD_STRING)) {
			return XPathConstants.STRING;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_FLOAT)) {
			return XPathConstants.NUMBER;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_DOUBLE)) {
			return XPathConstants.NUMBER;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_INT)) {
			return XPathConstants.NUMBER;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_INTEGER)) {
			return XPathConstants.NUMBER;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_LONG)) {
			return XPathConstants.NUMBER;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_BOOLEAN)) {
			return XPathConstants.BOOLEAN;
		}
		if (xsdTypeName.equals(DOMUtil.XSD_DATETIME)) {
			return XPathConstants.STRING;
		}
		if (xsdTypeName.equals(PolyStringType.COMPLEX_TYPE)) {
			return XPathConstants.STRING;
		}
		throw new ExpressionEvaluationException("Unsupported return type " + xsdTypeName);
	}


	/*
	 if (type.equals(String.class))
		{
            return XPathConstants.STRING;
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return XPathConstants.NUMBER;
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return XPathConstants.NUMBER;
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return XPathConstants.NUMBER;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return XPathConstants.BOOLEAN;
        }
        if (type.equals(NodeList.class)) {
        	if (expressionType.getReturnType() == ScriptExpressionReturnTypeType.SCALAR) {
        		// FIXME: is this OK?
        		return XPathConstants.STRING;
        	} else {
        		return XPathConstants.NODESET;
        	}
        }
        if (type.equals(Node.class)) {
            return XPathConstants.NODE;
        }
        if (type.equals(PolyString.class) || type.equals(PolyStringType.class)) {
        	return XPathConstants.STRING;
        }
        throw new ExpressionEvaluationException("Unsupported return type " + type);
    }
*/
    private <T, V extends PrismValue> V convertScalar(Class<T> type, QName returnType, Object value,
            String contextDescription) throws ExpressionEvaluationException {
        if (value instanceof ObjectReferenceType){
        	return (V) ((ObjectReferenceType) value).asReferenceValue();
        }
    	
    	if (type.isAssignableFrom(value.getClass())) {
            return (V) new PrismPropertyValue<T>((T) value);
        }
        try {
        	T resultValue = null;
            if (value instanceof String) {
            	resultValue = XmlTypeConverter.toJavaValue((String) value, type);
            } else if (value instanceof Boolean) {
            	resultValue = (T)value;
            } else if (value instanceof Element) {
            	resultValue = XmlTypeConverter.convertValueElementAsScalar((Element) value, type);
            } else {
            	throw new ExpressionEvaluationException("Unexpected scalar return type " + value.getClass().getName());
            }
            if (returnType.equals(PrismConstants.POLYSTRING_TYPE_QNAME) && resultValue instanceof String) {
            	resultValue = (T) new PolyString((String)resultValue);
            }
            PrismUtil.recomputeRealValue(resultValue, prismContext);
            
            return (V) new PrismPropertyValue<T>(resultValue);
        } catch (SchemaException e) {
            throw new ExpressionEvaluationException("Error converting result of "
                    + contextDescription + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Error converting result of "
                    + contextDescription + ": " + e.getMessage(), e);
        }
    }

    private <T, V extends PrismValue> List<V> convertList(Class<T> type, NodeList valueNodes, String contextDescription) throws
            ExpressionEvaluationException {
        List<V> values = new ArrayList<V>();
        if (valueNodes == null) {
            return values;
        }

        try {
            List<T> list = XmlTypeConverter.convertValueElementAsList(valueNodes, type);
            for (T item : list) {
            	if (item instanceof ObjectReferenceType){
            		values.add((V)((ObjectReferenceType) item).asReferenceValue());
            	}
                if (isNothing(item)) {
                    continue;
                }
                values.add((V) new PrismPropertyValue<T>(item));
            }
            return values;
        } catch (SchemaException e) {
            throw new ExpressionEvaluationException("Error converting return value of " + contextDescription + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Error converting return value of " + contextDescription + ": " + e.getMessage(), e);
        }
    }
    
    private <T> boolean isNothing(T value) {
    	return value == null || ((value instanceof String) && ((String) value).isEmpty());
    }

    private XPathFunctionResolver getFunctionResolver(Collection<FunctionLibrary> functions) {
    	return new ReflectionXPathFunctionResolver(functions);
    }

    @Override
    public String getLanguageName() {
        return "XPath 2.0";
    }
    
	@Override
	public String getLanguageUrl() {
		return XPATH_LANGUAGE_URL;
	}

}
