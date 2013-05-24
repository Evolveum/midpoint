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
package com.evolveum.midpoint.common.mapping;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.CommonTestConstants;
import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * The class that takes care of all the ornaments of value construction execution. It is used to make the
 * tests easy to write.
 *  
 * @author Radovan Semancik
 *
 */
public class MappingTestEvaluator {
	
	public static File TEST_DIR = new File("src/test/resources/mapping");
    public static final File USER_OLD_FILE = new File(TEST_DIR, "user-jack.xml");
    public static final File ACCOUNT_FILE = new File(TEST_DIR, "account-jack.xml");
	public static final String USER_OLD_OID = "2f9b9299-6f45-498f-bc8e-8d17c6b93b20";
	private static final File PASSWORD_POLICY_FILE = new File(TEST_DIR, "password-policy.xml");
    
    private PrismContext prismContext;
    private MappingFactory mappingFactory;
    AESProtector protector;
    
    public PrismContext getPrismContext() {
		return prismContext;
	}

	public void init() throws SAXException, IOException, SchemaException {
    	PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
    	prismContext = PrismTestUtil.createInitializedPrismContext();
    	ObjectResolver resolver = new DirectoryFileObjectResolver(CommonTestConstants.OBJECTS_DIR);
    	protector = new AESProtector();
        protector.setKeyStorePath(CommonTestConstants.KEYSTORE_PATH);
        protector.setKeyStorePassword(CommonTestConstants.KEYSTORE_PASSWORD);
        protector.setPrismContext(prismContext);
        protector.init();
    	
    	ExpressionFactory expressionFactory = new ExpressionFactory(resolver, prismContext);
    	
    	// asIs
    	AsIsExpressionEvaluatorFactory asIsFactory = new AsIsExpressionEvaluatorFactory(prismContext);
    	expressionFactory.addEvaluatorFactory(asIsFactory);
    	expressionFactory.setDefaultEvaluatorFactory(asIsFactory);

    	// value
    	LiteralExpressionEvaluatorFactory valueFactory = new LiteralExpressionEvaluatorFactory(prismContext);
    	expressionFactory.addEvaluatorFactory(valueFactory);
    	
    	// path
    	PathExpressionEvaluatorFactory pathFactory = new PathExpressionEvaluatorFactory(prismContext, resolver);
    	expressionFactory.addEvaluatorFactory(pathFactory);
    	
    	// generate
    	GenerateExpressionEvaluatorFactory generateFactory = new GenerateExpressionEvaluatorFactory(protector, resolver, prismContext);
    	expressionFactory.addEvaluatorFactory(generateFactory);

    	// script
    	Collection<FunctionLibrary> functions = new ArrayList<FunctionLibrary>();
        functions.add(ExpressionUtil.createBasicFunctionLibrary(prismContext));
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(resolver, prismContext, functions);
        XPathScriptEvaluator xpathEvaluator = new XPathScriptEvaluator(prismContext);
        scriptExpressionFactory.registerEvaluator(XPathScriptEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        Jsr223ScriptEvaluator groovyEvaluator = new Jsr223ScriptEvaluator("Groovy", prismContext);
        scriptExpressionFactory.registerEvaluator(groovyEvaluator.getLanguageUrl(), groovyEvaluator);
        ScriptExpressionEvaluatorFactory scriptExpressionEvaluatorFactory = new ScriptExpressionEvaluatorFactory(scriptExpressionFactory);
        expressionFactory.addEvaluatorFactory(scriptExpressionEvaluatorFactory);

        mappingFactory = new MappingFactory();
        mappingFactory.setExpressionFactory(expressionFactory);
        mappingFactory.setObjectResolver(resolver);
        mappingFactory.setPrismContext(prismContext);
        
        mappingFactory.setProtector(protector);
    }
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, final StringPolicyType policy, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
		return createMapping(filename, testName, policy, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
		return createMapping(filename, testName, null, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, ItemPath defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
		return createMapping(filename, testName, null, defaultTargetPropertyName, userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, final StringPolicyType policy, ItemPath defaultTargetPropertyPath, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
    
        JAXBElement<MappingType> mappingTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, filename), MappingType.class);
        MappingType valueConstructionType = mappingTypeElement.getValue();
        
        Mapping<PrismPropertyValue<T>> mapping = mappingFactory.createMapping(valueConstructionType, testName);
        
        // Source context: user
        PrismObject<UserType> userOld = null;
        if (userDelta == null || !userDelta.isAdd()) {
        	userOld = getUserOld();
        }
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<UserType>(userOld , userDelta, null);
        userOdo.recompute();
		mapping.setSourceContext(userOdo);
		
		// Variable $user
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		
		// Variable $account
		PrismObject<ShadowType> account = getAccount();
		ObjectDeltaObject<ShadowType> accountOdo = new ObjectDeltaObject<ShadowType>(account , null, null);
		accountOdo.recompute();
		mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountOdo);
        
		// Target context: user
		PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
		mapping.setTargetContext(userDefinition);
		
		
		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			ItemPath outputPath;
			ItemDefinition outputDefinition;
			
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				return policy;
			}
		};
		
		mapping.setStringPolicyResolver(stringPolicyResolver);
		// Default target
		if (defaultTargetPropertyPath != null) {
			ItemDefinition targetDefDefinition = userDefinition.findItemDefinition(defaultTargetPropertyPath);
			if (targetDefDefinition == null) {
				throw new IllegalArgumentException("The item path '"+defaultTargetPropertyPath+"' does not have a definition in "+userDefinition);
			}
			mapping.setDefaultTargetDefinition(targetDefDefinition);
		}
		
        return mapping;
    }
	
	protected PrismObject<UserType> getUserOld() throws SchemaException {
		return PrismTestUtil.parseObject(USER_OLD_FILE);
	}
	
	protected PrismObject<ShadowType> getAccount() throws SchemaException {
		return PrismTestUtil.parseObject(ACCOUNT_FILE);
	}
	
	private PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			ItemPath defaultTargetPropertyPath) 
			throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyPath, null);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(opResult);
		// TODO: Assert result success
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			String defaultTargetPropertyName) 
			throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, null);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(opResult);
		// TODO: Assert result success
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		return evaluateMappingDynamicAdd(filename, testName, toPath(defaultTargetPropertyName), changedPropertyName, valuesToAdd);
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(String filename, String testName, 
			ItemPath defaultTargetPropertyPath,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToAdd);
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyPath, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(opResult);
		// TODO: Assert result success
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicDelete(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToAdd);
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(opResult);
		// TODO: Assert result success
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToReplace) throws SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToReplace);
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(opResult);
		// TODO: Assert result success
		return mapping.getOutputTriple();
	}
	
	public ItemPath toPath(String propertyName) {
		return new ItemPath(new QName(SchemaConstants.NS_C, propertyName));
	}
	
	public static <T> T getSingleValue(String setName, Collection<PrismPropertyValue<T>> set) {
		assertEquals("Expected single value in "+setName+" but found "+set.size()+" values: "+set, 1, set.size());
		PrismPropertyValue<T> propertyValue = set.iterator().next();
		return propertyValue.getValue();
	}

	public StringPolicyType getStringPolicy() throws SchemaException {
		PrismObject<ValuePolicyType> passwordPolicy = PrismTestUtil.parseObject(PASSWORD_POLICY_FILE);
		return passwordPolicy.asObjectable().getStringPolicy();
	}

}
