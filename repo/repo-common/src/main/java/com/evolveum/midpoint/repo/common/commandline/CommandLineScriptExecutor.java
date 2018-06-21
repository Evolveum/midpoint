/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.common.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.CommandLineScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptArgumentType;

/**
 * @author matus
 * @author semancik
 */
@Component
public class CommandLineScriptExecutor {
	
    private static final String QUOTATION_MARK = "\"";

    private static final Trace LOGGER = TraceManager.getTrace(CommandLineScriptExecutor.class);
    
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
        
    public void executeScript(CommandLineScriptType scriptType, ExpressionVariables variables, String shortDesc, Task task, OperationResult parentResult) throws IOException, InterruptedException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	OperationResult result = parentResult.createSubresult(CommandLineScriptExecutor.class.getSimpleName() + ".run");
    	
    	if (variables == null) {
    		variables = new ExpressionVariables();
    	}
    	
    	String expandedCode = expandMacros(scriptType, variables, shortDesc, task, result);
    	
    	// TODO: later: prepare agruments and environment
    	
    	String preparedCode = expandedCode.trim(); 
    	LOGGER.debug("Prepared shell code: {}", preparedCode);
    	
    	CommandLineRunner runner = new CommandLineRunner(preparedCode, result);
    	runner.setExectionMethod(scriptType.getExecutionMethod());
    	
    	runner.execute();
    	
        result.computeStatus();
    }

    
    private String expandMacros(CommandLineScriptType scriptType, ExpressionVariables variables, String shortDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	String code = scriptType.getCode();
    	for (ProvisioningScriptArgumentType macroDef: scriptType.getMacro()) {
    		
    		String macroName = macroDef.getName();
    		QName macroQName = new QName(SchemaConstants.NS_C, macroName);
    		
    		String expressionOutput = "";
    		
    		PrismPropertyDefinitionImpl<String> outputDefinition = new PrismPropertyDefinitionImpl(
    				ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING, prismContext);
    		outputDefinition.setMaxOccurs(1);
    		Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
    				.makeExpression(macroDef, outputDefinition, shortDesc, task, result);

    		Collection<Source<?, ?>> sources = new ArrayList<>(1);
    		ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables, shortDesc, task,
    				result);
    		
    		Object defaultObject = variables.get(macroQName);
    		if (defaultObject != null) {
	    		Item sourceItem;
	    		if (defaultObject instanceof Item) {
	    			sourceItem = (Item)defaultObject;
	    		} else if (defaultObject instanceof String) {
	    			PrismPropertyDefinitionImpl<String> sourceDefinition = new PrismPropertyDefinitionImpl(
	        				ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING, prismContext);
	    			sourceDefinition.setMaxOccurs(1);
	    			PrismProperty<String> sourceProperty = sourceDefinition.instantiate();
	    			sourceProperty.setRealValue(defaultObject==null?null:defaultObject.toString());
	    			sourceItem = sourceProperty;
	    		} else {
	    			sourceItem = null;
	    		}
				Source<?, ?> defaultSource = new Source<>(sourceItem, null, sourceItem, macroQName);
				context.setDefaultSource(defaultSource);
				sources.add(defaultSource);
    		}

    		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context);

    		LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

    		if (outputTriple != null) {
	    		Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
	    		if (nonNegativeValues != null && !nonNegativeValues.isEmpty()) {		
	    			Collection<String> expressionOutputs = PrismValue.getRealValuesOfCollection((Collection) nonNegativeValues);    		
		    		if (expressionOutputs != null && !expressionOutputs.isEmpty()) {
		    			expressionOutput = StringUtils.join(expressionOutputs, ",");
		    		}
	    		}
    		}
    		
    		code = replaceMacro(code, macroDef.getName(), expressionOutput);
    	}
    	return code;
	}

	private String replaceMacro(String code, String name, String value) {
		return code.replace("%"+name+"%", value);
	}

    public String getOsSpecificFilePath(String filepath) {
        StringBuilder pathEscapedSpaces = new StringBuilder();
        if (SystemUtils.IS_OS_UNIX) {
            pathEscapedSpaces.append("'").append(filepath).append("'");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            filepath = filepath.replace("/", "\\");
            pathEscapedSpaces.append(QUOTATION_MARK).append(filepath).append(QUOTATION_MARK);
        } else {
            return filepath;
        }
        return pathEscapedSpaces.toString();
    }
	
}
