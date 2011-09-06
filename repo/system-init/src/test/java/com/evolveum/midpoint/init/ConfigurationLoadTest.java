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
	
	private static final Trace logger = TraceManager.getTrace(ConfigurationLoadTest.class);

	@Test
	public void t01simpleConfigTest() {
		logger.info("---------------- simpleConfigTest -----------------");
		
		System.clearProperty("midpoint.home");
		logger.info("midpoint.home => " + System.getProperty("midpoint.home"));
		
		assertNull(System.getProperty("midpoint.home"), "midpoint.home");
		
		StartupConfiguration sc = new StartupConfiguration();
		assertNotNull(sc);
		sc.init();
		Configuration c = sc.getConfiguration("midpoint.repository");
		assertEquals(c.getString("repositoryServiceFactoryClass"),
				"com.evolveum.midpoint.repo.xml.XmlRepositoryServiceFactory");
		logger.info(sc.toString());
		
		@SuppressWarnings("unchecked")
		Iterator<String> i = c.getKeys();
		
		while ( i.hasNext()) {
			String key = i.next();
			logger.info("  " + key + " = " + c.getString(key));
		}
		
		assertEquals(c.getInt("port"),1984);
		assertEquals(c.getString("initialDataPath"), "" );
		
	}

	
	@Test
	public void t02directoryAndExtractionTest() {
		logger.info("---------------- directoryAndExtractionTest -----------------");
		
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

	@Test
	public void t03complexConfigTest() {
		logger.info("---------------- complexConfigTest -----------------");
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
			logger.info("  " + key + " = " + c.getString(key));
		}
		
		assertEquals(c.getString("repository.initialDataPath"), "target/midPointHome/" );
		
		//cleanup
		System.clearProperty("midpoint.home");		
	}
}
