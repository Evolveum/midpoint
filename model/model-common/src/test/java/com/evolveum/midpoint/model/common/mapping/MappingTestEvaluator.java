/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.common.mapping;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.AESProtector;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
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
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * The class that takes care of all the ornaments of value construction execution. It is used to make the
 * tests easy to write.
 *  
 * @author Radovan Semancik
 *
 */
public class MappingTestEvaluator {
	
	public static File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "mapping");
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
    	ObjectResolver resolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
    	protector = ExpressionTestUtil.createInitializedProtector(prismContext);
    	ExpressionFactory expressionFactory = ExpressionTestUtil.createInitializedExpressionFactory(resolver, protector, prismContext, null);
    	
        mappingFactory = new MappingFactory();
        mappingFactory.setExpressionFactory(expressionFactory);
        mappingFactory.setObjectResolver(resolver);
        mappingFactory.setPrismContext(prismContext);
        mappingFactory.setProfiling(true);
        
        mappingFactory.setProtector(protector);
    }
	
	public AESProtector getProtector() {
		return protector;
	}

	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, final StringPolicyType policy, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, IOException, JAXBException, EncryptionException  {
		return createMapping(filename, testName, policy, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, String defaultTargetPropertyName, 
			ObjectDelta<UserType> userDelta) throws SchemaException, IOException, JAXBException, EncryptionException  {
		return createMapping(filename, testName, null, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, QName defaultTargetPropertyName, 
			ObjectDelta<UserType> userDelta) throws SchemaException, IOException, JAXBException, EncryptionException  {
		return createMapping(filename, testName, null, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, String defaultTargetPropertyName, 
			ObjectDelta<UserType> userDelta, PrismObject<UserType> userOld) throws SchemaException, IOException, JAXBException  {
		return createMapping(filename, testName, null, toPath(defaultTargetPropertyName), userDelta, userOld);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, ItemPath defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, IOException, JAXBException, EncryptionException  {
		return createMapping(filename, testName, null, defaultTargetPropertyName, userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, final StringPolicyType policy, 
			ItemPath defaultTargetPropertyPath, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, JAXBException, EncryptionException  {
		PrismObject<UserType> userOld = null;
        if (userDelta == null || !userDelta.isAdd()) {
        	userOld = getUserOld();
        }
		return createMapping(filename, testName, policy, defaultTargetPropertyPath, userDelta, userOld);
	}
	
	public <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(String filename, String testName, final StringPolicyType policy, 
			ItemPath defaultTargetPropertyPath, ObjectDelta<UserType> userDelta,  PrismObject<UserType> userOld)
            throws SchemaException, IOException, JAXBException  {
    
        MappingType mappingType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, filename), MappingType.COMPLEX_TYPE);

        Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = mappingFactory.createMapping(mappingType, testName);
        
        // Source context: user
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
			PrismPropertyDefinition<T> targetDefDefinition = userDefinition.findItemDefinition(defaultTargetPropertyPath);
			if (targetDefDefinition == null) {
				throw new IllegalArgumentException("The item path '"+defaultTargetPropertyPath+"' does not have a definition in "+userDefinition);
			}
			mapping.setDefaultTargetDefinition(targetDefDefinition);
		}
		
        return mapping;
    }
	
	public  <T> Mapping<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createInboudMapping(String filename, String testName, ItemDelta delta, UserType user, ShadowType account, ResourceType resource, final StringPolicyType policy) throws SchemaException, IOException, JAXBException{
		
		MappingType mappingType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, filename), MappingType.COMPLEX_TYPE);

		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = mappingFactory.createMapping(mappingType,testName);
    	
    	
    	Source<PrismPropertyValue<T>,PrismPropertyDefinition<T>> defaultSource = new Source<>(null, delta, null, ExpressionConstants.VAR_INPUT);
    	defaultSource.recompute();
		mapping.setDefaultSource(defaultSource);
		mapping.setTargetContext(getUserDefinition());
    	mapping.addVariableDefinition(ExpressionConstants.VAR_USER, user);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, user);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account.asPrismObject());
    	mapping.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account.asPrismObject());
		
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

		mapping.setOriginType(OriginType.INBOUND);
		mapping.setOriginObject(resource);
		
		return mapping;
	}
	
	protected PrismObject<UserType> getUserOld() throws SchemaException, EncryptionException, IOException {
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_OLD_FILE);
		ProtectedStringType passwordPs = user.asObjectable().getCredentials().getPassword().getValue();
		protector.encrypt(passwordPs);
		return user;
	}
	
	protected PrismObject<ShadowType> getAccount() throws SchemaException, IOException {
		return PrismTestUtil.parseObject(ACCOUNT_FILE);
	}
	
	public PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			ItemPath defaultTargetPropertyPath)
            throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyPath, null);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			QName defaultTargetPropertyName)
            throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, null);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			String defaultTargetPropertyName)
            throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, null);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public void assertResult(OperationResult opResult) {
		if (opResult.isEmpty()) {
			// this is OK. Nothing added to result.
			return;
		}
		opResult.computeStatus();
		TestUtil.assertSuccess(opResult);		
	}

	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		return evaluateMappingDynamicAdd(filename, testName, toPath(defaultTargetPropertyName), changedPropertyName, valuesToAdd);
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(String filename, String testName, 
			ItemPath defaultTargetPropertyPath,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToAdd);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyPath, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicDelete(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToAdd) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToAdd);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(String filename, String testName, 
			String defaultTargetPropertyName,
			String changedPropertyName, I... valuesToReplace) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToReplace);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(String filename, String testName, 
			String defaultTargetPropertyName,
			ItemPath changedPropertyName, I... valuesToReplace) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_OLD_OID, changedPropertyName, 
				prismContext, valuesToReplace);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(String filename, String testName, 
			ItemPath defaultTargetPropertyName,
			String changedPropertyName, I... valuesToReplace) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToReplace);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(String filename, String testName, 
			ItemPath defaultTargetPropertyName,
			ItemPath changedPropertyName, I... valuesToReplace) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException, EncryptionException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_OLD_OID, changedPropertyName, 
				prismContext, valuesToReplace);
		Mapping<PrismPropertyValue<T>,PrismPropertyDefinition<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
		OperationResult opResult = new OperationResult(testName);
		mapping.evaluate(null, opResult);
		assertResult(opResult);
		return mapping.getOutputTriple();
	}
	
	public ItemPath toPath(String propertyName) {
		return new ItemPath(new QName(SchemaConstants.NS_C, propertyName));
	}
	
	public ItemPath toPath(QName propertyName) {
		return new ItemPath(propertyName);
	}
	
	public static <T> T getSingleValue(String setName, Collection<PrismPropertyValue<T>> set) {
		assertEquals("Expected single value in "+setName+" but found "+set.size()+" values: "+set, 1, set.size());
		PrismPropertyValue<T> propertyValue = set.iterator().next();
		return propertyValue.getValue();
	}

	public StringPolicyType getStringPolicy() throws SchemaException, IOException {
		PrismObject<ValuePolicyType> passwordPolicy = PrismTestUtil.parseObject(PASSWORD_POLICY_FILE);
		return passwordPolicy.asObjectable().getStringPolicy();
	}

	public Object createProtectedString(String string) throws EncryptionException {
		return protector.encryptString(string);
	}

	public void assertProtectedString(String desc,
			Collection<PrismPropertyValue<ProtectedStringType>> set,
			String expected) throws EncryptionException {
    	assertEquals("Unexpected size of "+desc+": "+set, 1, set.size());
    	PrismPropertyValue<ProtectedStringType> pval = set.iterator().next();
    	ProtectedStringType ps = pval.getValue();
    	String zeroString = protector.decryptString(ps);
    	assertEquals("Unexpected value in "+desc+": "+set, expected, zeroString);
		
	}

}
