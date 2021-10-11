/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
 package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchShadowOwnerTest extends BaseSQLRepoTest {

    @Test
    public void searchNonExistingShadowOwner() {
        //searching owner for non existing shadow
        OperationResult result = new OperationResult("List owner");
        PrismObject<FocusType> shadow = repositoryService.searchShadowOwner("12345", null, result);
        AssertJUnit.assertNull(shadow);
        result.computeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Override
    public void initSystem() throws Exception {
        super.initSystem();

        OperationResult result = new OperationResult("Add sample data");

        //insert sample data
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(OBJECTS_FILE).parseObjects();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            repositoryService.addObject(object, null, result);
        }
        result.computeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void searchShadowOwner() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //look for account owner
        PrismObject<UserType> user = repositoryService.searchShadowOwner("11223344", null,  result);

        assertNotNull("No owner for account", user);
        PrismProperty name = user.findProperty(ObjectType.F_NAME);
        AssertJUnit.assertNotNull(name);
        AssertJUnit.assertEquals("atestuserX00003", ((PolyString) name.getRealValue()).getOrig());
    }

    @Test
    public void searchShadowOwnerIsRole() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //look for account owner
        PrismObject<RoleType> role = repositoryService.searchShadowOwner("11223355", null, result);

        assertNotNull("No owner for account", role);
        PrismProperty name = role.findProperty(ObjectType.F_NAME);
        AssertJUnit.assertNotNull(name);
        String orig = ((PolyString) name.getRealValue()).getOrig();
        // there are two object which own tested shadow
        if ("Judge".equals(orig)
                || "lazyman vm".equals(orig)) {
            return;
        }
        AssertJUnit.fail("Unexpected object name for shadow owner '" + orig + "'");
    }
}
