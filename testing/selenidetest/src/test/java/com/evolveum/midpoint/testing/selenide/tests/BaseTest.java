package com.evolveum.midpoint.testing.selenide.tests;

import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * Created by Kate on 13.08.2015.
 */
@ContextConfiguration(locations = {"classpath:spring-module.xml"})
public class BaseTest extends AbstractTestNGSpringContextTests {

    public BaseTest() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }



}
