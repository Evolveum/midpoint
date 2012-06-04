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
package com.evolveum.midpoint.common.valueconstruction;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.common.valueconstruction.ValueConstructionTestEvaluator.OBJECTS_DIR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.jsr223.Jsr223ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueConstructionType;

/**
 * The class that takes care of all the ornaments of value construction execution. It is used to make the
 * tests easy to write.
 *  
 * @author Radovan Semancik
 *
 */
public class ValueConstructionTestEvaluator {
	
	public static File TEST_DIR = new File("src/test/resources/valueconstruction");
    public static File OBJECTS_DIR = new File("src/test/resources/objects");
    private static final String KEYSTORE_PATH = "src/test/resources/crypto/test-keystore.jceks";
    
    private PrismContext prismContext;
    private ValueConstructionFactory factory;
    AESProtector protector;
    
    public PrismContext getPrismContext() {
		return prismContext;
	}

	public void init() throws SAXException, IOException, SchemaException {
    	DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
    	prismContext = PrismTestUtil.createInitializedPrismContext();
    	
        ExpressionFactory expressionFactory = new ExpressionFactory(prismContext);
        XPathExpressionEvaluator xpathEvaluator = new XPathExpressionEvaluator(prismContext);
        expressionFactory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        Jsr223ExpressionEvaluator groovyEvaluator = new Jsr223ExpressionEvaluator("Groovy", prismContext);
        expressionFactory.registerEvaluator(groovyEvaluator.getLanguageUrl(), groovyEvaluator);
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        expressionFactory.setObjectResolver(resolver);

        factory = new ValueConstructionFactory();
        factory.setExpressionFactory(expressionFactory);
        factory.setObjectResolver(resolver);
        factory.setPrismContext(prismContext);
        
        protector = new AESProtector();
        protector.setKeyStorePath(KEYSTORE_PATH);
        protector.setKeyStorePassword("changeit");
        protector.setPrismContext(prismContext);
        protector.init();
        factory.setProtector(protector);
    }

	public PrismPropertyDefinition getPropertyDefinition(String propertyName) {
		return getPropertyDefinition(toPath(propertyName));
	}
	
	public PrismPropertyDefinition getPropertyDefinition(PropertyPath propertyPath) {
		PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        return userDef.findPropertyDefinition(propertyPath);
	}
	
    public <T> ValueConstruction<PrismPropertyValue<T>> createConstruction(Class<T> type, String filename, String propertyName, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	return createConstruction(type, filename, toPath(propertyName), inputPropertyValue, extraVariables, testName);
    }
    
    public <T> ValueConstruction<PrismPropertyValue<T>> createConstruction(Class<T> type, String filename, PropertyPath propertyPath, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    
        JAXBElement<ValueConstructionType> valueConstructionTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, filename), ValueConstructionType.class);
        ValueConstructionType valueConstructionType = valueConstructionTypeElement.getValue();

        PrismPropertyDefinition propDef = getPropertyDefinition(propertyPath);
        
        ValueConstruction<PrismPropertyValue<T>> construction = factory.createValueConstruction(valueConstructionType, propDef, testName);
        if (inputPropertyValue != null) {
        	PrismProperty inputProperty = propDef.instantiate();
        	PrismPropertyValue<T> pval = new PrismPropertyValue<T>(inputPropertyValue);
        	inputProperty.add(pval);
        	inputProperty.setValue(pval);
        	construction.setInput(inputProperty);
        }
        if (extraVariables != null) {
        	construction.addVariableDefinitions(extraVariables);
        }
        return construction;
    }
    
    public <T> PrismProperty<T> evaluateConstructionStatic(Class<T> type, String filename, String propertyName, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {

    	return evaluateConstructionStatic(type, filename, toPath(propertyName), inputPropertyValue, extraVariables, testName);
    }
    
	public <T> PrismProperty<T> evaluateConstructionStatic(Class<T> type, String filename, PropertyPath propertyPath, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	
    	ValueConstruction<PrismPropertyValue<T>> construction = createConstruction(type, filename, propertyPath, inputPropertyValue, extraVariables, testName);    	
        OperationResult opResult = new OperationResult(testName);

        construction.evaluate(opResult);
        PrismProperty<T> result = (PrismProperty<T>) construction.getOutput();        

        return result;
    }
	
	public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateConstructionDynamicAdd(Class<T> type, String filename, String propertyName, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName, T... valuesToAdd) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		return evaluateConstructionDynamicAdd(type, filename, toPath(propertyName), inputPropertyValue, extraVariables, testName, valuesToAdd);
	}
	
	public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateConstructionDynamicAdd(Class<T> type, String filename, PropertyPath propertyPath, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName, T... valuesToAdd) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
        PropertyDelta<T> delta = createAddDelta(type, propertyPath, valuesToAdd);
        return evaluateConstructionDynamic(type, filename, propertyPath, inputPropertyValue, delta, extraVariables, testName);
	}
	
	public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateConstructionDynamicDelete(Class<T> type, String filename, String propertyName, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName, T... valuesToDelete) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		return evaluateConstructionDynamicDelete(type, filename, toPath(propertyName), inputPropertyValue, extraVariables, testName, valuesToDelete);
	}

	public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateConstructionDynamicDelete(Class<T> type, String filename, PropertyPath propertyPath, 
    		T inputPropertyValue, Map<QName, Object> extraVariables, String testName, T... valuesToDelete) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		PropertyDelta<T> delta = createDeleteDelta(type, propertyPath, valuesToDelete);
        return evaluateConstructionDynamic(type, filename, propertyPath, inputPropertyValue, delta, extraVariables, testName);
	}
	
	public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateConstructionDynamic(Class<T> type, String filename, PropertyPath propertyPath, 
    		T inputPropertyValue, PropertyDelta<T> delta, Map<QName, Object> extraVariables, String testName) 
    		throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
    	
    	ValueConstruction<PrismPropertyValue<T>> construction = createConstruction(type, filename, propertyPath, inputPropertyValue, extraVariables, testName);
    	construction.setInputDelta(delta);
        OperationResult opResult = new OperationResult(testName);

        construction.evaluate(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = construction.getOutputTriple();        

        return outputTriple;
    }
	
	public <T> PropertyDelta<T> createAddDelta(Class<T> type, String propertyName, T... valuesToAdd) {
		return createAddDelta(type, toPath(propertyName), valuesToAdd);
	}

	public <T> PropertyDelta<T> createAddDelta(Class<T> type, PropertyPath propertyPath, T... valuesToAdd) {
		PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismPropertyDefinition propDef = userDef.findPropertyDefinition(propertyPath);
        PropertyDelta<T> delta = new PropertyDelta<T>(propertyPath, propDef);
        delta.addValuesToAdd(PrismPropertyValue.createCollection(valuesToAdd));
        return delta;
	}
	
	public <T> PropertyDelta<T> createDeleteDelta(Class<T> type, String propertyName, T... valuesToAdd) {
		return createDeleteDelta(type, toPath(propertyName), valuesToAdd);
	}
	
	public <T> PropertyDelta<T> createDeleteDelta(Class<T> type, PropertyPath propertyPath, T... valuesToDelete) {
		PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismPropertyDefinition propDef = userDef.findPropertyDefinition(propertyPath);
		PropertyDelta<T> delta = new PropertyDelta<T>(propertyPath, propDef);
		delta.addValuesToDelete(PrismPropertyValue.createCollection(valuesToDelete));
		return delta;
	}
	
	public <T> PropertyDelta<T> createReplaceDelta(Class<T> type, String propertyName, T... valuesToAdd) {
		return createReplaceDelta(type, toPath(propertyName), valuesToAdd);
	}
	
	public <T> PropertyDelta<T> createReplaceDelta(Class<T> type, PropertyPath propertyPath, T... valuesToReplace) {
		PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismPropertyDefinition propDef = userDef.findPropertyDefinition(propertyPath);
		PropertyDelta<T> delta = new PropertyDelta<T>(propertyPath, propDef);
		delta.setValuesToReplace(PrismPropertyValue.createCollection(valuesToReplace));
		return delta;
	}

	public ProtectedStringType createProtectedString(String clearString) throws EncryptionException {
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue(clearString);
		protector.encrypt(ps);
		return ps;
	}

	
	private PropertyPath toPath(String propertyName) {
		return new PropertyPath(new QName(SchemaConstants.NS_C, propertyName));
	}

	public static <T> T getSingleValue(String setName, Collection<PrismPropertyValue<T>> set) {
		assertEquals("Expected single value in "+setName+" but found "+set.size()+" values: "+set, 1, set.size());
		PrismPropertyValue<T> propertyValue = set.iterator().next();
		return propertyValue.getValue();
	}


}
