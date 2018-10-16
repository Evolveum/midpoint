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
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.xml.security.encryption.XMLCipher;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;
import static org.testng.Assert.fail;

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
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
	    PrismObject<UserType> jack = prismContext.parserFor(FILE_USER_JACK).xml().parse();

	    // WHEN+THEN
	    checkEncryptedObject(jack);
    }

	@Test
	public void test110EncryptUser() throws Exception {
		final String TEST_NAME = "test110EncryptUser";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
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
		PrismContext prismContext = PrismTestUtil.getPrismContext();
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
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<SystemConfigurationType> config = prismContext.parserFor(FILE_SYSTEM_CONFIGURATION).xml().parse();

		// WHEN
		CryptoUtil.encryptValues(protector, config);

		// THEN
		System.out.println("After encryption:\n" + config.debugDump());
		CryptoUtil.checkEncrypted(config);
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
		ProtectorImpl protector = new ProtectorImpl();
		protector.setKeyStorePassword(KEYSTORE_PASSWORD);
		protector.setKeyStorePath(KEYSTORE_PATH);
		protector.setEncryptionAlgorithm(XMLCipher.AES_256);
		protector.init();
		return protector;
	}

}
