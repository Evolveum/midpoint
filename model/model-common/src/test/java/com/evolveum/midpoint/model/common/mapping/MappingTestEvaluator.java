/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.DirectoryFileObjectResolver;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * The class that takes care of all the ornaments of value construction execution. It is used to make the
 * tests easy to write.
 *
 * @author Radovan Semancik
 */
public class MappingTestEvaluator {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "mapping");
    public static final File USER_OLD_FILE = new File(TEST_DIR, "user-jack.xml");
    public static final File ACCOUNT_FILE = new File(TEST_DIR, "account-jack.xml");
    public static final String USER_OLD_OID = "2f9b9299-6f45-498f-bc8e-8d17c6b93b20";
    private static final File PASSWORD_POLICY_FILE = new File(TEST_DIR, "password-policy.xml");

    private PrismContext prismContext;
    private MappingFactory mappingFactory;
    private Protector protector;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void init() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        prismContext = PrismTestUtil.createInitializedPrismContext();
        ObjectResolver resolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
        protector = ExpressionTestUtil.createInitializedProtector(prismContext);
        Clock clock = new Clock();
        ExpressionFactory expressionFactory = ExpressionTestUtil.createInitializedExpressionFactory(resolver, protector, prismContext, clock, null, null);

        mappingFactory = new MappingFactory();
        mappingFactory.setExpressionFactory(expressionFactory);
        mappingFactory.setObjectResolver(resolver);
        mappingFactory.setPrismContext(prismContext);
        mappingFactory.setProfiling(true);

        mappingFactory.setProtector(protector);
    }

    public Protector getProtector() {
        return protector;
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(
            String filename, String testName, final ValuePolicyType policy,
            String defaultTargetPropertyName, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        return this.<T>createMappingBuilder(filename, testName, policy, toPath(defaultTargetPropertyName), userDelta).build();
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(
            String filename, String testName, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        return this.<T>createMappingBuilder(filename, testName, null, toPath(defaultTargetPropertyName), userDelta).build();
    }

    public <T> MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMappingBuilder(
            String filename, String testName, String defaultTargetPropertyName, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        return createMappingBuilder(filename, testName, null, toPath(defaultTargetPropertyName), userDelta);
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(
            String filename, String testName, ItemName defaultTargetPropertyName, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        return this.<T>createMappingBuilder(filename, testName, null, defaultTargetPropertyName, userDelta).build();
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(
            String filename, String testName, String defaultTargetPropertyName,
            ObjectDelta<UserType> userDelta, PrismObject<UserType> userOld)
            throws SchemaException, IOException {
        return this.<T>createMappingBuilder(filename, testName, null, toPath(defaultTargetPropertyName), userDelta, userOld).build();
    }

    public <T> MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMappingBuilder(String filename, String testName, String defaultTargetPropertyName,
            ObjectDelta<UserType> userDelta, PrismObject<UserType> userOld) throws SchemaException, IOException {
        return this.createMappingBuilder(filename, testName, null, toPath(defaultTargetPropertyName), userDelta, userOld);
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMapping(
            String filename, String testName, ItemPath defaultTargetPropertyName, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        return this.<T>createMappingBuilder(filename, testName, null, defaultTargetPropertyName, userDelta).build();
    }

    public <T> MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMappingBuilder(
            String filename, String testName, final ValuePolicyType policy,
            ItemPath defaultTargetPropertyPath, ObjectDelta<UserType> userDelta)
            throws SchemaException, IOException, EncryptionException {
        PrismObject<UserType> userOld = null;
        if (userDelta == null || !userDelta.isAdd()) {
            userOld = getUserOld();
        }
        return createMappingBuilder(filename, testName, policy, defaultTargetPropertyPath, userDelta, userOld);
    }

    public <T> MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createMappingBuilder(
            String filename, String testName, final ValuePolicyType policy, ItemPath defaultTargetPropertyPath,
            ObjectDelta<UserType> userDelta, PrismObject<UserType> userOld)
            throws SchemaException, IOException {
        MappingType mappingType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, filename), MappingType.COMPLEX_TYPE);

        MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mappingBuilder =
                mappingFactory.createMappingBuilder(mappingType, testName);
        mappingBuilder.prismContext(prismContext);

        // Source context: user
        PrismObjectDefinition<UserType> objectDefinition =
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        ObjectDeltaObject<UserType> userOdo =
                new ObjectDeltaObject<>(userOld, userDelta, null, objectDefinition);
        userOdo.recompute();
        mappingBuilder.sourceContext(userOdo);

        // Variable $focus
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, userOdo);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);

        // Variable $projection
        PrismObject<ShadowType> account = getAccount();
        ObjectDeltaObject<ShadowType> accountOdo =
                new ObjectDeltaObject<>(account, null, null, account.getDefinition());
        accountOdo.recompute();
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, accountOdo);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountOdo);

        // Target context: user
        PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
        mappingBuilder.targetContext(userDefinition);

        ValuePolicyResolver stringPolicyResolver = new ValuePolicyResolver() {
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
            public ValuePolicyType resolve() {
                return policy;
            }
        };

        mappingBuilder.stringPolicyResolver(stringPolicyResolver);
        // Default target
        if (defaultTargetPropertyPath != null) {
            PrismPropertyDefinition<T> targetDefDefinition = userDefinition.findItemDefinition(defaultTargetPropertyPath);
            if (targetDefDefinition == null) {
                throw new IllegalArgumentException("The item path '" + defaultTargetPropertyPath + "' does not have a definition in " + userDefinition);
            }
            mappingBuilder.defaultTargetDefinition(targetDefDefinition);
        }

        return mappingBuilder;
    }

    public <T> MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createInboudMapping(
            String filename, String testName, ItemDelta delta, UserType user, ShadowType account,
            ResourceType resource, final ValuePolicyType policy)
            throws SchemaException, IOException {

        MappingType mappingType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, filename), MappingType.COMPLEX_TYPE);

        MappingImpl.Builder<PrismPropertyValue<T>, PrismPropertyDefinition<T>> builder = mappingFactory.createMappingBuilder(mappingType, testName);

        Source<PrismPropertyValue<T>, PrismPropertyDefinition<T>> defaultSource = new Source<>(null, delta, null, ExpressionConstants.VAR_INPUT_QNAME, delta.getDefinition());
        defaultSource.recompute();
        builder.defaultSource(defaultSource);
        builder.targetContext(getUserDefinition());
        builder.addVariableDefinition(ExpressionConstants.VAR_USER, user, UserType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, user, UserType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account.asPrismObject(), ShadowType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account.asPrismObject(), ShadowType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, account.asPrismObject(), ShadowType.class);

        ValuePolicyResolver stringPolicyResolver = new ValuePolicyResolver() {
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
            public ValuePolicyType resolve() {
                return policy;
            }
        };

        builder.stringPolicyResolver(stringPolicyResolver);

        builder.originType(OriginType.INBOUND);
        builder.originObject(resource);

        return builder.build();
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

    public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(
            String filename, String testName, ItemPath defaultTargetPropertyPath)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyPath, null);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(
            String filename, String testName, ItemName defaultTargetPropertyName)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, null);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMapping(
            String filename, String testName, String defaultTargetPropertyName)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, null);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public void assertResult(OperationResult opResult) {
        if (opResult.isEmpty()) {
            // this is OK. Nothing added to result.
            return;
        }
        opResult.computeStatus();
        TestUtil.assertSuccess(opResult);
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(
            String filename, String testName, String defaultTargetPropertyName,
            String changedPropertyName, I... valuesToAdd)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        return evaluateMappingDynamicAdd(filename, testName,
                toPath(defaultTargetPropertyName), changedPropertyName, valuesToAdd);
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicAdd(
            String filename, String testName, ItemPath defaultTargetPropertyPath,
            String changedPropertyName, I... valuesToAdd)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationAddProperty(
                        UserType.class, USER_OLD_OID, toPath(changedPropertyName), valuesToAdd);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyPath, userDelta);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicDelete(
            String filename, String testName, String defaultTargetPropertyName,
            String changedPropertyName, I... valuesToAdd)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationDeleteProperty(
                        UserType.class, USER_OLD_OID, toPath(changedPropertyName), valuesToAdd);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, userDelta);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(
            String filename, String testName, String defaultTargetPropertyName,
            String changedPropertyName, I... valuesToReplace)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, USER_OLD_OID, toPath(changedPropertyName), valuesToReplace);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, userDelta);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(
            String filename, String testName, String defaultTargetPropertyName,
            ItemPath changedPropertyName, I... valuesToReplace)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, USER_OLD_OID, changedPropertyName, valuesToReplace);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, userDelta);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(
            String filename, String testName, ItemPath defaultTargetPropertyName,
            String changedPropertyName, I... valuesToReplace)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, USER_OLD_OID, toPath(changedPropertyName), valuesToReplace);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, userDelta);
        OperationResult opResult = new OperationResult(testName);

        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public <T, I> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateMappingDynamicReplace(
            String filename, String testName, ItemPath defaultTargetPropertyName,
            ItemPath changedPropertyName, I... valuesToReplace)
            throws SchemaException, IOException, ExpressionEvaluationException,
            ObjectNotFoundException, EncryptionException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_OLD_OID, changedPropertyName,
                        valuesToReplace);
        MappingImpl<PrismPropertyValue<T>, PrismPropertyDefinition<T>> mapping =
                createMapping(filename, testName, defaultTargetPropertyName, userDelta);
        OperationResult opResult = new OperationResult(testName);
        mapping.evaluate(new NullTaskImpl(), opResult);
        assertResult(opResult);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        if (outputTriple != null) {
            outputTriple.checkConsistence();
        }
        return outputTriple;
    }

    public ItemPath toPath(String propertyName) {
        return ItemPath.create(new QName(SchemaConstants.NS_C, propertyName));
    }

    public static <T> T getSingleValue(String setName, Collection<PrismPropertyValue<T>> set) {
        assertEquals("Expected single value in " + setName + " but found " + set.size() + " values: " + set, 1, set.size());
        PrismPropertyValue<T> propertyValue = set.iterator().next();
        return propertyValue.getValue();
    }

    public ValuePolicyType getValuePolicy() throws SchemaException, IOException {
        PrismObject<ValuePolicyType> passwordPolicy = PrismTestUtil.parseObject(PASSWORD_POLICY_FILE);
        return passwordPolicy.asObjectable();
    }

    public Object createProtectedString(String string) throws EncryptionException {
        return protector.encryptString(string);
    }

    public void assertProtectedString(
            String desc, Collection<PrismPropertyValue<ProtectedStringType>> set, String expected)
            throws EncryptionException {
        assertEquals("Unexpected size of " + desc + ": " + set, 1, set.size());
        PrismPropertyValue<ProtectedStringType> pval = set.iterator().next();
        ProtectedStringType ps = pval.getValue();
        String zeroString = protector.decryptString(ps);
        assertEquals("Unexpected value in " + desc + ": " + set, expected, zeroString);
    }
}
