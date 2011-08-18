package com.evolveum.midpoint.init;


import static org.testng.Assert.*;

import org.apache.commons.configuration.Configuration;
import org.testng.annotations.Test;


public class ConfigurationLoadTest {
  @Test
  public void sampleTest() {
	  StartupConfiguration sc = new StartupConfiguration();
	  assertNotNull(sc);
	  Configuration c = sc.getConfiguration("midpoint.repository");
	  assertEquals(c.getString("class") , "com.evolveum.midpoint.repo.xml");
	  System.out.println(sc.dumpConfig(c));
  }
}
