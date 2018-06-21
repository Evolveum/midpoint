/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.init;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import static org.testng.Assert.assertTrue;
import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class TestConfigurationLoad {

	private static final Trace LOGGER = TraceManager.getTrace(TestConfigurationLoad.class);

	@Test
	public void test010SimpleConfigTest() {
		LOGGER.info("---------------- test010SimpleConfigTest -----------------");

		System.clearProperty("midpoint.home");
		LOGGER.info("midpoint.home => " + System.getProperty("midpoint.home"));

		assertNull(System.getProperty("midpoint.home"), "midpoint.home");

		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();
		Configuration c = sc.getConfiguration("midpoint.repository");
		assertEquals(c.getString("repositoryServiceFactoryClass"),
				"com.evolveum.midpoint.repo.sql.SqlRepositoryFactory");
		LOGGER.info(sc.toString());

		@SuppressWarnings("unchecked")
		Iterator<String> i = c.getKeys();

		while ( i.hasNext()) {
			String key = i.next();
			LOGGER.info("  " + key + " = " + c.getString(key));
		}

		assertEquals(c.getBoolean("asServer"), true);
		assertEquals(c.getString("baseDir"), System.getProperty("midpoint.home") );

	}

	/**
	 * MID-3349
	 */
	@Test
	public void test020DirectoryAndExtractionTest() throws Exception {
		LOGGER.info("---------------- test020DirectoryAndExtractionTest -----------------");

		File midpointHome = new File("target/midPointHome");
		System.setProperty("midpoint.home", "target/midPointHome/");
		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();

		assertNotNull(midpointHome);
		assertTrue(midpointHome.exists(),  "existence");
		assertTrue(midpointHome.isDirectory(),  "type directory");

		File configFile = new File(midpointHome, "config.xml");
		assertTrue(configFile.exists(),  "existence");
		assertTrue(configFile.isFile(),  "type file");
		TestUtil.assertPrivateFilePermissions(configFile);
		
		ConfigurableProtectorFactory keystoreFactory = new ConfigurableProtectorFactory();
		keystoreFactory.setConfiguration(sc);
		keystoreFactory.init();

		File keystoreFile = new File(midpointHome, "keystore.jceks");
		assertTrue(keystoreFile.exists(),  "existence");
		assertTrue(keystoreFile.isFile(),  "type file");
		TestUtil.assertPrivateFilePermissions(keystoreFile);
		
		//cleanup
		System.clearProperty("midpoint.home");

	}
	

}
