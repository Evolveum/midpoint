/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;


/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ActionManagerImplTest extends AbstractTestNGSpringContextTests  {

    @Autowired(required = true)
    private ActionManager<? extends Action> manager;

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void setActionMappingNull() {
        manager.setActionMapping(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getActionInstanceNullUri() {
        manager.getActionInstance(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void getctionInstanceEmptyUri() {
        manager.getActionInstance("");
    }

    @Test
    public void getActionInstanceNullParameters() {
        Action filter = manager
                .getActionInstance("http://midpoint.evolveum.com/xml/ns/public/model/action-3#addUser");
        assertNotNull(filter);
    }

    @Test
    public void getActionInstanceNotExistingFilter() {
        Action filter = manager.getActionInstance("notExistingUri");
        assertNull(filter);
    }
}
