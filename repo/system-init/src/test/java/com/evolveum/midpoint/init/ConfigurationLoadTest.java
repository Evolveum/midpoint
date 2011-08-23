package com.evolveum.midpoint.init;


import static org.testng.Assert.*;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.testng.annotations.Test;


public class ConfigurationLoadTest {
  @Test
  public void sampleTest() {
	  StartupConfiguration sc = new StartupConfiguration();
	  assertNotNull(sc);
	  sc.init();
	  Configuration c = sc.getConfiguration("midpoint.repository");
	  assertEquals(c.getString("repositoryServiceFactoryClass") , "com.evolveum.midpoint.repo.xml.XmlRepositoryServiceFactory");
	  System.out.println(c.toString());
	  
	  c.getInt("port", 1234);
  }
}
