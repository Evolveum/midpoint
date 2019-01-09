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
package com.evolveum.midpoint.common;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_4.*;
import com.evolveum.prism.xml.ns._public.types_4.ProtectedStringType;
import org.apache.xml.security.encryption.XMLCipher;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;
import static java.util.Collections.singleton;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public class TestCryptoUtil {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "crypto");
    private static final File FILE_USER_JACK = new File(TEST_DIR, "user-jack.xml");
    private static final File FILE_SYSTEM_CONFIGURATION = new File(TEST_DIR, "system-configuration.xml");

	private static final String KEYSTORE_PATH = TEST_RESOURCES_PATH + "/keystore.jceks";
	private static final String KEYSTORE_PASSWORD = "changeit";

	private Protector protector;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		protector = createProtector();
	}

    @Test
    public void test100CheckEncryptedUser() throws Exception {
    	final String TEST_NAME = "test100CheckEncryptedUser";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
    	PrismContext prismContext = getPrismContext();
	    PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();

	    // WHEN+THEN
	    checkEncryptedObject(jack);
    }

	@Test
	public void test110EncryptUser() throws Exception {
		final String TEST_NAME = "test110EncryptUser";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = getPrismContext();
		PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();

		// WHEN
		CryptoUtil.encryptValues(protector, jack);

		// THEN
		System.out.println("After encryption:\n" + jack.debugDump());
		CryptoUtil.checkEncrypted(jack);
	}

	// MID-4941
	@Test
	public void test200CheckEncryptedSystemConfiguration() throws Exception {
		final String TEST_NAME = "test200CheckEncryptedSystemConfiguration";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = getPrismContext();
		PrismObject<SystemConfigurationType> config = prismContext.parserFor(FILE_SYSTEM_CONFIGURATION).xml().parse();

		// WHEN+THEN
		checkEncryptedObject(config);
	}

	// MID-4941
	@Test
	public void test210EncryptSystemConfiguration() throws Exception {
		final String TEST_NAME = "test210EncryptSystemConfiguration";
		TestUtil.displayTestTitle(TEST_NAME);

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
		final String TEST_NAME = "test300Reencryption";
		TestUtil.displayTestTitle(TEST_NAME);

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
				.encryptionAlgorithm(XMLCipher.AES_256)
				.initialize();
	}

	private Protector createCompromisedProtector() {
		return KeyStoreBasedProtectorBuilder.create(getPrismContext())
				.keyStorePassword(KEYSTORE_PASSWORD)
				.keyStorePath(KEYSTORE_PATH)
				.encryptionKeyAlias("compromised")
				.encryptionAlgorithm(XMLCipher.AES_256)
				.initialize();
	}

}
