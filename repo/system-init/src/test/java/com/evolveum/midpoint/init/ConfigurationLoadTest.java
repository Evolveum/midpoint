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

package com.evolveum.midpoint.init;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import static org.testng.Assert.assertTrue;
import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ConfigurationLoadTest {

	private static final Trace LOGGER = TraceManager.getTrace(ConfigurationLoadTest.class);

    @Test
    public void dummyTest() {}

	@Test(enabled = false)
	public void t01simpleConfigTest() {
		LOGGER.info("---------------- simpleConfigTest -----------------");

		System.clearProperty("midpoint.home");
		LOGGER.info("midpoint.home => " + System.getProperty("midpoint.home"));

		assertNull(System.getProperty("midpoint.home"), "midpoint.home");

		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();
		Configuration c = sc.getConfiguration("midpoint.repository");
		assertEquals(c.getString("repositoryServiceFactoryClass"),
				"com.evolveum.midpoint.repo.xml.XmlRepositoryServiceFactory");
		LOGGER.info(sc.toString());

		@SuppressWarnings("unchecked")
		Iterator<String> i = c.getKeys();

		while ( i.hasNext()) {
			String key = i.next();
			LOGGER.info("  " + key + " = " + c.getString(key));
		}

		assertEquals(c.getInt("port"),1984);
		assertEquals(c.getString("serverPath"), "" );

	}


	@Test
	public void t02directoryAndExtractionTest() {
		LOGGER.info("---------------- directoryAndExtractionTest -----------------");

		File f = new File("target/midPointHome");
		System.setProperty("midpoint.home", "target/midPointHome/");
		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();

		assertNotNull(f);
		assertTrue(f.exists(),  "existence");
		assertTrue(f.isDirectory(),  "type directory");

		f = new File("target/midPointHome/config.xml");
		assertTrue(f.exists(),  "existence");
		assertTrue(f.isFile(),  "type file");

		//cleanup
		System.clearProperty("midpoint.home");

	}

    @Test(enabled = false)
	public void t03complexConfigTest() {
		LOGGER.info("---------------- complexConfigTest -----------------");
		System.setProperty("midpoint.home", "target/midPointHome/");
		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();
		Configuration c = sc.getConfiguration("midpoint");
		assertEquals(c.getString("repository.repositoryServiceFactoryClass"),
				"com.evolveum.midpoint.repo.xml.XmlRepositoryServiceFactory");

		@SuppressWarnings("unchecked")
		Iterator<String> i = c.getKeys();

		while ( i.hasNext()) {
			String key = i.next();
			LOGGER.info("  " + key + " = " + c.getString(key));
		}

		assertEquals(c.getString("repository.serverPath"), "target/midPointHome/" );

		//cleanup
		System.clearProperty("midpoint.home");
	}
}
