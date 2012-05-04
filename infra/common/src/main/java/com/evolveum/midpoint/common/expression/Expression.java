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
package com.evolveum.midpoint.common.expression;

import com.evolveum.midpoint.common.valueconstruction.ObjectDeltaObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionReturnTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

/**
 * The expressions should be created by ExpressionFactory. They expect correct setting of
 * expression evaluator and proper conversion form the XML ExpressionType. Factory does this.
 *
 * @author Radovan Semancik
 */
public class Expression {

    private Element code;
    private Map<QName, Object> variables;
    private String shortDesc;
    private ExpressionEvaluator evaluator;
    private ObjectResolver objectResolver;
    private ExpressionReturnTypeType returnType;

    private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

    Expression(ExpressionEvaluator evaluator, ExpressionType expressionType, String shortDesc) {
        this.code = expressionType.getCode();
        this.shortDesc = shortDesc;
        this.evaluator = evaluator;
        this.variables = new HashMap<QName, Object>();
        if (expressionType.getVariable() != null) {
            for (VariableDefinitionType varDef : expressionType.getVariable()) {
                addVariableDefinition(varDef);
            }
        }
        this.returnType = null;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public ExpressionReturnTypeType getReturnType() {
        return returnType;
    }

    public void setReturnType(ExpressionReturnTypeType returnType) {
        this.returnType = returnType;
    }

    public void addVariableDefinition(VariableDefinitionType varDef) {
        if (varDef.getName() == null) {
            throw new IllegalArgumentException("Null variable name in " + shortDesc);
        }
        if (varDef.getObjectRef() != null) {
            addVariableDefinition(varDef.getName(), varDef.getObjectRef());
        } else if (varDef.getValue() != null) {
            addVariableDefinition(varDef.getName(), varDef.getValue());
        } else {
            LOGGER.warn("Empty definition of variable {} in expression {}, ignoring it", varDef.getName(), shortDesc);
        }
    }

    public void addVariableDefinition(QName name, ObjectReferenceType objectRef) {
        addVariableDefinition(name, (Object) objectRef);
    }

    public void addVariableDefinition(QName name, String value) {
        addVariableDefinition(name, (Object) value);
    }

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) the operation fail because
     * it cannot decide which version to use.
     */
    public void addVariableDefinitions(Map<QName, Object> extraVariables) {
        for (Entry<QName, Object> entry : extraVariables.entrySet()) {
        	Object value = entry.getValue();
        	if (value instanceof ObjectDeltaObject<?>) {
        		ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
        		if (odo.getDelta() != null) {
        			throw new IllegalArgumentException("Cannot use variables with deltas in addVariableDefinitions, use addVariableDefinitionsOld or addVariableDefinitionsNew");
        		}
        		value = odo.getOldObject();
        	}
            variables.put(entry.getKey(), value);
        }
    }

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) it takes the "old" version
     * of the object.
     */
    public void addVariableDefinitionsOld(Map<QName, Object> extraVariables) {
        for (Entry<QName, Object> entry : extraVariables.entrySet()) {
        	Object value = entry.getValue();
        	if (value instanceof ObjectDeltaObject<?>) {
        		ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
        		value = odo.getOldObject();
        	}
            variables.put(entry.getKey(), value);
        }
    }

    /**
     * Adds map of extra variables to the expression.
     * If there are variables with deltas (ObjectDeltaObject) it takes the "new" version
     * of the object.
     */
    public void addVariableDefinitionsNew(Map<QName, Object> extraVariables) {
        for (Entry<QName, Object> entry : extraVariables.entrySet()) {
        	Object value = entry.getValue();
        	if (value instanceof ObjectDeltaObject<?>) {
        		ObjectDeltaObject<?> odo = (ObjectDeltaObject<?>)value;
        		value = odo.getNewObject();
        	}
            variables.put(entry.getKey(), value);
        }
    }
    
    public void setRootNode(ObjectReferenceType objectRef) {
        addVariableDefinition(null, (Object) objectRef);
    }

    private void addVariableDefinition(QName name, Object value) {
        if (variables.containsKey(name)) {
            LOGGER.warn("Duplicate definition of variable {} in expression {}, ignoring it", name, shortDesc);
            return;
        }
        variables.put(name, value);
    }

    public <T> PrismPropertyValue<T> evaluateScalar(Class<T> type, OperationResult result) throws
            ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	
    	if (code == null) {
    		throw new ExpressionEvaluationException("No expression code in "+shortDesc);
    	}

        // Return type override
        if (returnType == ExpressionReturnTypeType.LIST) {
            List<PrismPropertyValue<T>> retList = evaluateList(type, result);
            if (retList.isEmpty()) {
                return null;
            }
            if (retList.size() > 1) {
                String msg = "Expression produced list of " + retList.size() + " entries, but it was requested to be evaluated in scalar context. Cannot reduce list to scalar.";
                ExpressionEvaluationException ex = new ExpressionEvaluationException(msg);
                LOGGER.error(msg, ex);
                throw ex;
            }
            return retList.get(0);
        }

        // Normal evaluation
        try {

            PrismPropertyValue<T> ret = evaluator.evaluateScalar(type, code, variables, objectResolver, shortDesc, result);

            traceExpressionSuccess("scalar", type, ret);
            return ret;
        } catch (ExpressionEvaluationException ex) {
            traceExpressionFailure("scalar", type, ex);
            throw ex;
        } catch (ObjectNotFoundException ex) {
            traceExpressionFailure("scalar", type, ex);
            throw ex;
        } catch (SchemaException ex) {
            traceExpressionFailure("scalar", type, ex);
            throw ex;
        } catch (RuntimeException ex) {
            traceExpressionFailure("scalar", type, ex);
            throw ex;
        }
    }

    public <T> List<PrismPropertyValue<T>> evaluateList(Class<T> type, OperationResult result) throws
            ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

    	if (code == null) {
    		throw new ExpressionEvaluationException("No expression code in "+shortDesc);
    	}

        // Return type override
        if (returnType == ExpressionReturnTypeType.SCALAR) {
            PrismPropertyValue<T> ret = evaluateScalar(type, result);
            List<PrismPropertyValue<T>> retList = new ArrayList<PrismPropertyValue<T>>(1);
            if (ret != null) {
                retList.add(ret);
            }
            return retList;
        }

        // Normal evaluation
        try {

            List<PrismPropertyValue<T>> ret = evaluator.evaluateList(type, code, variables, objectResolver, shortDesc, result);

            traceExpressionSuccess("list", type, ret);
            return ret;
        } catch (ExpressionEvaluationException ex) {
            traceExpressionFailure("list", type, ex);
            throw ex;
        } catch (ObjectNotFoundException ex) {
            traceExpressionFailure("list", type, ex);
            throw ex;
        } catch (SchemaException ex) {
            traceExpressionFailure("list", type, ex);
            throw ex;
        } catch (RuntimeException ex) {
            traceExpressionFailure("list", type, ex);
            throw ex;
        }
    }

    private void traceExpressionSuccess(String returnContext, Class<?> returnType, Object returnValue) {
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Expression trace:\n"+
            		"---[ EXPRESSION {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Return type: {} ({})\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Result: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), returnType, returnContext, formatVariables(), formatCode(),
                    SchemaDebugUtil.prettyPrint(returnValue)
            });
        }
    }

    private void traceExpressionFailure(String returnContext, Class<?> returnType, Exception exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Expression trace:\n"+
            		"---[ EXPRESSION {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Return type: {} ({})\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Error: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), returnType, returnContext, formatVariables(), formatCode(),
                    SchemaDebugUtil.prettyPrint(exception)
            });
        }
    }

    private String formatVariables() {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<QName, Object>> i = variables.entrySet().iterator();
        while (i.hasNext()) {
            Entry<QName, Object> entry = i.next();
            SchemaDebugUtil.indentDebugDump(sb, 1);
            sb.append(SchemaDebugUtil.prettyPrint(entry.getKey())).append(": ");
            Object value = entry.getValue();
            if (value instanceof DebugDumpable) {
            	sb.append("\n");
            	sb.append(((DebugDumpable)value).debugDump(2));
            } else if (value instanceof Element) {
            	sb.append("\n");
            	sb.append(DOMUtil.serializeDOMToString(((Element)value)));
            } else {
            	sb.append(SchemaDebugUtil.prettyPrint(value));
            }
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String formatCode() {
        return DOMUtil.serializeDOMToString(code);
    }

}
