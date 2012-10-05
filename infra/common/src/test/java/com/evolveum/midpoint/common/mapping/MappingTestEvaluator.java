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
package com.evolveum.midpoint.common.mapping;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.common.mapping.MappingTestEvaluator.OBJECTS_DIR;

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
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.evaluator.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * The class that takes care of all the ornaments of value construction execution. It is used to make the
 * tests easy to write.
 *  
 * @author Radovan Semancik
 *
 */
public class MappingTestEvaluator {
	
	public static File TEST_DIR = new File("src/test/resources/mapping");
    public static File OBJECTS_DIR = new File("src/test/resources/objects");
    public static final String KEYSTORE_PATH = "src/test/resources/crypto/test-keystore.jceks";
    public static final File USER_OLD_FILE = new File(TEST_DIR, "user-jack.xml");
	public static final String USER_OLD_OID = "2f9b9299-6f45-498f-bc8e-8d17c6b93b20";
	private static final File PASSWORD_POLICY_FILE = new File(TEST_DIR, "password-policy.xml");
    
    private PrismContext prismContext;
    private MappingFactory mappingFactory;
    AESProtector protector;
    
    public PrismContext getPrismContext() {
		return prismContext;
	}

	public void init() throws SAXException, IOException, SchemaException {
    	DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
    	prismContext = PrismTestUtil.createInitializedPrismContext();
    	ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
    	protector = new AESProtector();
        protector.setKeyStorePath(KEYSTORE_PATH);
        protector.setKeyStorePassword("changeit");
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
    	GenerateExpressionEvaluatorFactory generateFactory = new GenerateExpressionEvaluatorFactory(protector, prismContext);
    	expressionFactory.addEvaluatorFactory(generateFactory);

    	// script
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(resolver, prismContext);
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
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
		return createMapping(filename, testName, toPath(defaultTargetPropertyName), userDelta);
	}
	
	public <T> Mapping<PrismPropertyValue<T>> createMapping(String filename, String testName, PropertyPath defaultTargetPropertyPath, ObjectDelta<UserType> userDelta) throws SchemaException, FileNotFoundException, JAXBException  {
    
        JAXBElement<MappingType> mappingTypeElement = PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, filename), MappingType.class);
        MappingType valueConstructionType = mappingTypeElement.getValue();
        
        Mapping<PrismPropertyValue<T>> mapping = mappingFactory.createMapping(valueConstructionType, testName);
        
        // Source context: user
        PrismObject<UserType> userOld = getUserOld();
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<UserType>(userOld , userDelta, null);
        userOdo.recompute();
		mapping.setSourceContext(userOdo);
		// ... also as a variable $user
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
        
		// Target context: user
		PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
		mapping.setTargetContext(userDefinition);
		
		// Default target
		if (defaultTargetPropertyPath != null) {
			mapping.setDefaultTargetDefinition(userDefinition.findItemDefinition(defaultTargetPropertyPath));
		}
		
        return mapping;
    }
	
	private PrismObject<UserType> getUserOld() throws SchemaException {
		return PrismTestUtil.parseObject(USER_OLD_FILE);
	}
	
	private PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	public <T,I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(String filename, String testName, 
			PropertyPath defaultTargetPropertyPath) 
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
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_OLD_OID, toPath(changedPropertyName), 
				prismContext, valuesToAdd);
		Mapping<PrismPropertyValue<T>> mapping = createMapping(filename, testName, defaultTargetPropertyName, userDelta);
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
	
	public PropertyPath toPath(String propertyName) {
		return new PropertyPath(new QName(SchemaConstants.NS_C, propertyName));
	}
	
	public static <T> T getSingleValue(String setName, Collection<PrismPropertyValue<T>> set) {
		assertEquals("Expected single value in "+setName+" but found "+set.size()+" values: "+set, 1, set.size());
		PrismPropertyValue<T> propertyValue = set.iterator().next();
		return propertyValue.getValue();
	}

	public StringPolicyType getStringPolicy() throws SchemaException {
		PrismObject<PasswordPolicyType> passwordPolicy = PrismTestUtil.parseObject(PASSWORD_POLICY_FILE);
		return passwordPolicy.asObjectable().getStringPolicy();
	}

}
