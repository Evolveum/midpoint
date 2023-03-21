/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import static com.evolveum.midpoint.schema.util.task.work.SpecificWorkDefinitionUtil.*;

import static java.util.Collections.singleton;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.namespace.QName;

@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestCryptoUtil extends AbstractUnitTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "crypto");
    private static final File FILE_USER_JACK = new File(TEST_DIR, "user-jack.xml");
    private static final File FILE_TASK_MODIFY_JACK_PASSWORD = new File(TEST_DIR, "task-modify-jack-password.xml");
    private static final File FILE_TASK_ADD_JACK = new File(TEST_DIR, "task-add-jack.xml");
    private static final File FILE_TASK_ADD_ACCOUNT = new File(TEST_DIR, "task-add-account.xml");
    private static final File FILE_SYSTEM_CONFIGURATION = new File(TEST_DIR, "system-configuration.xml");

    private static final String KEYSTORE_PATH = TEST_RESOURCES_PATH + "/keystore.jceks";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private static final String PASSWORD_PLAINTEXT = "pass1234word";
    private static final String PASSWORD_PLAINTEXT2 = "pass1234w0rd";

    private Protector protector;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        protector = createProtector();
    }

    @Test
    public void test100CheckEncryptedUser() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();

        // WHEN+THEN
        checkEncryptedObject(jack);
    }

    @Test
    public void test110EncryptUser() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();

        // WHEN
        CryptoUtil.encryptValues(protector, jack);

        // THEN
        System.out.println("After encryption:\n" + jack.debugDump());
        CryptoUtil.checkEncrypted(jack);
    }

    @Test
    public void test120EncryptBulkActionTask() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> task = prismContext.parserFor(FILE_TASK_MODIFY_JACK_PASSWORD).xml().parse();

        // WHEN
        CryptoUtil.encryptValues(protector, task);

        // THEN
        String serialized = prismContext.xmlSerializer().serialize(task);
        System.out.println("After encryption:\n" + serialized);
        assertFalse("Serialized object contains the password!", serialized.contains(PASSWORD_PLAINTEXT));

        CryptoUtil.checkEncrypted(task);
    }

    /**
     * MID-6086
     */
    @Test
    public void test125EncryptAddAccountTask() throws Exception {
        given();
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> task = prismContext.parserFor(FILE_TASK_ADD_ACCOUNT).xml().parse();

        when();
        CryptoUtil.encryptValues(protector, task);

        then();
        String serialized = prismContext.xmlSerializer().serialize(task);
        System.out.println("After encryption:\n" + serialized);
        assertFalse("Serialized object contains the password!", serialized.contains(PASSWORD_PLAINTEXT));

        CryptoUtil.checkEncrypted(task);
    }

    /**
     * MID-6086
     */
    @Test
    public void test127EncryptAddAccountTaskManuallyConstructedLegacy() throws Exception {
        given();
        PrismContext prismContext = getPrismContext();
        PrismObject<TaskType> task = new TaskType(prismContext)
                .name("test127")
                .asPrismObject();

        // This legacy use is OK. We want to check exactly this way of the task construction.
        PrismPropertyDefinition<ObjectDeltaType> deltasDefinition = task.getDefinition()
                .findPropertyDefinition(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS));
        PrismProperty<ObjectDeltaType> deltas = deltasDefinition.instantiate();

        ShadowType shadow = new ShadowType(prismContext)
                .name("some-shadow");
        PrismContainerDefinition<Containerable> attributesDef = shadow.asPrismObject().getDefinition()
                .findContainerDefinition(ShadowType.F_ATTRIBUTES);
        PrismContainer<?> attributes = attributesDef.instantiate();
        shadow.asPrismObject().add(attributes);

        MutablePrismPropertyDefinition<ProtectedStringType> passwordDef = prismContext.definitionFactory()
                .createPropertyDefinition(
                        new QName(SchemaConstants.NS_ICF_SCHEMA, "password"), ProtectedStringType.COMPLEX_TYPE);
        PrismProperty<ProtectedStringType> password = passwordDef.instantiate();
        ProtectedStringType passwordRealValue = new ProtectedStringType();
        passwordRealValue.setClearValue(PASSWORD_PLAINTEXT);
        password.setRealValue(passwordRealValue);
        attributes.add(password);

        PrismReferenceValue linkToAdd = ObjectTypeUtil.createObjectRefWithFullObject(shadow).asReferenceValue();
        ObjectDelta<UserType> userDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(linkToAdd)
                .asObjectDelta("some-oid");

        deltas.addRealValue(DeltaConvertor.toObjectDeltaType(userDelta, null));
        task.addExtensionItem(deltas);

        when();
        CryptoUtil.encryptValues(protector, task);

        then();
        String serialized = prismContext.xmlSerializer().serialize(task);
        System.out.println("After encryption:\n" + serialized);
        assertFalse("Serialized object contains the password!", serialized.contains(PASSWORD_PLAINTEXT));

        CryptoUtil.checkEncrypted(task);
    }

    /**
     * MID-6086
     */
    @Test
    public void test128EncryptAddAccountTaskManuallyConstructedNew() throws Exception {
        given();
        PrismContext prismContext = getPrismContext();
        PrismObject<TaskType> task = new TaskType(prismContext)
                .name("test128")
                .asPrismObject();

        ShadowType shadow = new ShadowType(prismContext)
                .name("some-shadow");
        PrismContainerDefinition<Containerable> attributesDef = shadow.asPrismObject().getDefinition()
                .findContainerDefinition(ShadowType.F_ATTRIBUTES);
        PrismContainer<?> attributes = attributesDef.instantiate();
        shadow.asPrismObject().add(attributes);

        MutablePrismPropertyDefinition<ProtectedStringType> passwordDef = prismContext.definitionFactory()
                .createPropertyDefinition(
                        new QName(SchemaConstants.NS_ICF_SCHEMA, "password"), ProtectedStringType.COMPLEX_TYPE);
        PrismProperty<ProtectedStringType> password = passwordDef.instantiate();
        ProtectedStringType passwordRealValue = new ProtectedStringType();
        passwordRealValue.setClearValue(PASSWORD_PLAINTEXT);
        password.setRealValue(passwordRealValue);
        attributes.add(password);

        PrismReferenceValue linkToAdd = ObjectTypeUtil.createObjectRefWithFullObject(shadow).asReferenceValue();
        ObjectDelta<UserType> userDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(linkToAdd)
                .asObjectDelta("some-oid");

        task.asObjectable().setActivity(
                createExplicitChangeExecutionDef(List.of(userDelta), null));

        when();
        CryptoUtil.encryptValues(protector, task);

        then();
        String serialized = prismContext.xmlSerializer().serialize(task);
        System.out.println("After encryption:\n" + serialized);
        assertFalse("Serialized object contains the password!", serialized.contains(PASSWORD_PLAINTEXT));

        CryptoUtil.checkEncrypted(task);
    }

    @Test
    public void test130EncryptUserInDelta() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> task = prismContext.parserFor(FILE_TASK_ADD_JACK).xml().parse();

        // WHEN
        CryptoUtil.encryptValues(protector, task);

        // THEN
        String serialized = prismContext.xmlSerializer().serialize(task);
        System.out.println("After encryption:\n" + serialized);
        assertFalse("Serialized object contains the password!", serialized.contains(PASSWORD_PLAINTEXT));

        CryptoUtil.checkEncrypted(task);
    }

    // MID-4941
    @Test
    public void test200CheckEncryptedSystemConfiguration() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<SystemConfigurationType> config = prismContext.parserFor(FILE_SYSTEM_CONFIGURATION).xml().parse();

        // WHEN+THEN
        checkEncryptedObject(config);
    }

    // MID-4941
    @Test
    public void test210EncryptSystemConfiguration() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<SystemConfigurationType> config = prismContext.parserFor(FILE_SYSTEM_CONFIGURATION).xml().parse();

        // WHEN
        CryptoUtil.encryptValues(protector, config);

        // THEN
        System.out.println("After encryption:\n" + config.debugDump());
        CryptoUtil.checkEncrypted(config);
    }

    // MID-4942
    @SuppressWarnings("SimplifiedTestNGAssertion")
    @Test
    public void test300Reencryption() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();
        PrismObject<SystemConfigurationType> config = prismContext.parserFor(FILE_SYSTEM_CONFIGURATION).xml().parse();
        Protector compromisedProtector = createCompromisedProtector();

        // WHEN
        CryptoUtil.encryptValues(compromisedProtector, jack);
        CryptoUtil.encryptValues(compromisedProtector, config);
        System.out.println("jack compromised:\n" + prismContext.xmlSerializer().serialize(jack));
        System.out.println("sysconfig compromised:\n" + prismContext.xmlSerializer().serialize(config));
        CryptoUtil.checkEncrypted(jack);
        CryptoUtil.checkEncrypted(config);
        MailConfigurationType mail = config.asObjectable().getNotificationConfiguration().getMail();
        SmsConfigurationType sms1 = config.asObjectable().getNotificationConfiguration().getSms().get(0);
        SmsConfigurationType sms2 = config.asObjectable().getNotificationConfiguration().getSms().get(1);
        String compromisedKeyName = getKeyName(
                jack.asObjectable().getCredentials().getPassword().getValue(),
                mail.getServer().get(0).getPassword(),
                sms1.getGateway().get(0).getPassword(),
                sms2.getGateway().get(0).getPassword());
        System.out.println("Compromised key name: " + compromisedKeyName);
        PrismAsserts.assertSets("Wrong key names in jack", singleton(compromisedKeyName), CryptoUtil.getEncryptionKeyNames(jack));
        PrismAsserts.assertSets("Wrong key names in sysconfig", singleton(compromisedKeyName), CryptoUtil.getEncryptionKeyNames(config));

        // THEN
        PrismObject<UserType> jackOld = jack.clone();
        PrismObject<SystemConfigurationType> configOld = config.clone();
        Collection<? extends ItemDelta<?, ?>> reencryptJackOldMods = CryptoUtil.computeReencryptModifications(compromisedProtector, jack);
        Collection<? extends ItemDelta<?, ?>> reencryptJackNewMods = CryptoUtil.computeReencryptModifications(protector, jack);
        Collection<? extends ItemDelta<?, ?>> reencryptConfigNewMods = CryptoUtil.computeReencryptModifications(protector, config);
        int reencryptJackOldCount = CryptoUtil.reencryptValues(compromisedProtector, jack);
        int reencryptJackNewCount = CryptoUtil.reencryptValues(protector, jack);
        int reencryptConfigNewCount = CryptoUtil.reencryptValues(protector, config);

        assertTrue("Unexpected reencrypt delta (jack old): " + reencryptJackNewMods, reencryptJackOldMods.isEmpty());
        assertReencryptDelta("jack new", reencryptJackNewMods, 1, jackOld, protector);
        assertReencryptDelta("config new", reencryptConfigNewMods, 2, configOld, protector);   // mail + sms

        assertEquals("Wrong # of reencrypted passwords (jack old)", 0, reencryptJackOldCount);
        assertEquals("Wrong # of reencrypted passwords (jack new)", 1, reencryptJackNewCount);
        assertEquals("Wrong # of reencrypted passwords (sysconfig new)", 3, reencryptConfigNewCount);
        System.out.println("jack reencrypted:\n" + prismContext.xmlSerializer().serialize(jack));
        System.out.println("sysconfig reencrypted:\n" + prismContext.xmlSerializer().serialize(config));
        String newKeyName = getKeyName(
                jack.asObjectable().getCredentials().getPassword().getValue(),
                mail.getServer().get(0).getPassword(),
                sms1.getGateway().get(0).getPassword(),
                sms2.getGateway().get(0).getPassword());
        System.out.println("New key name: " + newKeyName);
        PrismAsserts.assertSets("Wrong key names in jack (new)", singleton(newKeyName), CryptoUtil.getEncryptionKeyNames(jack));
        PrismAsserts.assertSets("Wrong key names in sysconfig (new)", singleton(newKeyName), CryptoUtil.getEncryptionKeyNames(config));
        assertFalse("New and compromised key names are NOT different", compromisedKeyName.equals(newKeyName));
    }

    @Test
    public void test310HashWithFixedSalt() throws Exception {
        // GIVEN
        Protector fixedSaltProtector = createFixedSaltProtector();
        ProtectedStringType passwordRealValue = new ProtectedStringType();
        ProtectedStringType passwordRealValue2 = new ProtectedStringType();
        ProtectedStringType passwordRealValue3 = new ProtectedStringType();
        ProtectedStringType passwordRealValueOrd = new ProtectedStringType();
        ProtectedStringType passwordRealValueOrd2 = new ProtectedStringType();
        passwordRealValue.setClearValue(PASSWORD_PLAINTEXT);
        passwordRealValue2.setClearValue(PASSWORD_PLAINTEXT);
        passwordRealValue3.setClearValue(PASSWORD_PLAINTEXT2);
        passwordRealValueOrd.setClearValue(PASSWORD_PLAINTEXT);
        passwordRealValueOrd2.setClearValue(PASSWORD_PLAINTEXT);

        // WHEN
        // ordinary protector:
        protector.hash(passwordRealValueOrd);
        protector.hash(passwordRealValueOrd2);

        // fixed-salt protector:
        fixedSaltProtector.hash(passwordRealValue);
        fixedSaltProtector.hash(passwordRealValue2);
        fixedSaltProtector.hash(passwordRealValue3); // different password

        // THEN
        // ordinary hashing produces different hash for same input as salt changes:
        Assert.assertNotEquals(passwordRealValueOrd.getHashedDataType().getDigestMethod().getSalt(),
                passwordRealValueOrd2.getHashedDataType().getDigestMethod().getSalt());

        Assert.assertNotEquals(passwordRealValueOrd.getHashedDataType().getDigestValue(),
                passwordRealValueOrd2.getHashedDataType().getDigestValue());

        // fixed-salt hashing produces same salt for different values and same hash for same values:
        Assert.assertEquals(passwordRealValue.getHashedDataType().getDigestMethod().getSalt(),
                passwordRealValue3.getHashedDataType().getDigestMethod().getSalt());

        Assert.assertEquals(passwordRealValue.getHashedDataType().getDigestValue(),
                passwordRealValue2.getHashedDataType().getDigestValue());
    }

    private <T extends ObjectType> void assertReencryptDelta(String label, Collection<? extends ItemDelta<?, ?>> modifications,
            int expectedModificationsCount, PrismObject<T> oldObject, Protector protector) throws SchemaException, EncryptionException {
        System.out.println("Modifications for " + label + ":\n" + modifications);
        assertEquals("Delta has wrong # of modifications: " + label, expectedModificationsCount, modifications.size());
        PrismObject<T> patchedObject = oldObject.clone();
        ItemDeltaCollectionsUtil.applyTo(modifications, patchedObject);
        int fixes = CryptoUtil.reencryptValues(protector, patchedObject);
        assertEquals("Wrong # of re-encryption fixes on reencrypted object: " + label, 0, fixes);
    }

    private String getKeyName(ProtectedStringType... values) {
        Set<String> names = new HashSet<>();
        for (ProtectedStringType value : values) {
            names.add(value.getEncryptedDataType().getKeyInfo().getKeyName());
        }
        assertEquals("Wrong # of different key names: " + names, 1, names.size());
        return names.iterator().next();
    }

    private void checkEncryptedObject(PrismObject<? extends ObjectType> object) {
        try {
            CryptoUtil.checkEncrypted(object);
            fail("Unexpected success");
        } catch (IllegalStateException e) {
            System.out.println("Got expected exception: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    private Protector createProtector() {
        return KeyStoreBasedProtectorBuilder.create(getPrismContext())
                .keyStorePassword(KEYSTORE_PASSWORD)
                .keyStorePath(KEYSTORE_PATH)
                .encryptionAlgorithm(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC)
                .initialize();
    }

    private Protector createCompromisedProtector() {
        return KeyStoreBasedProtectorBuilder.create(getPrismContext())
                .keyStorePassword(KEYSTORE_PASSWORD)
                .keyStorePath(KEYSTORE_PATH)
                .encryptionKeyAlias("compromised")
                .encryptionAlgorithm(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC)
                .initialize();
    }

    private Protector createFixedSaltProtector() {
        return KeyStoreBasedProtectorBuilder.create(getPrismContext())
                .keyStorePassword(KEYSTORE_PASSWORD)
                .keyStorePath(KEYSTORE_PATH)
                .encryptionAlgorithm(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC)
                .fixedSalt("mp1-salt")
                .initialize();
    }
}
