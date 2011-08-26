package com.evolveum.midpoint.util;

import java.util.Set;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import com.evolveum.midpoint.util.ClassPathUtil;

public class ClassPathTest {
  @Test
  public void listClassesLocalTest() {
	  
	  Set<Class> cs = ClassPathUtil.listClasses("com.evolveum.midpoint.util");
	  assertNotNull(cs);
	  assertTrue(cs.contains(ClassPathUtil.class));
  }
  
  @Test
  public void listClassesJarTest() {
	  
	  Set<Class> cs = ClassPathUtil.listClasses("org.testng.annotations");
//	  Set<Class> cs = ClassPathUtil.listClasses("org.testng");
	  assertNotNull(cs);
	  assertTrue(cs.contains(Test.class));
  }
  
}
