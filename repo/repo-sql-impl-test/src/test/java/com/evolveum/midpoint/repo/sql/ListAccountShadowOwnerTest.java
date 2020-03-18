/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class ListAccountShadowOwnerTest extends BaseSQLRepoTest {

    @Test
    public void listExistingOwner() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //insert sample data
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(OBJECTS_FILE).parseObjects();
        for (PrismObject object : elements) {
            repositoryService.addObject(object, null, result);
        }

        //look for account owner
        PrismObject<UserType> user = repositoryService.listAccountShadowOwner("1234", result);

        assertNotNull("No owner for account 1234", user);
        PrismProperty name = user.findProperty(ObjectType.F_NAME);
        AssertJUnit.assertNotNull(name);
        AssertJUnit.assertEquals("atestuserX00003", ((PolyString) name.getRealValue()).getOrig());
    }

    @Test
    public void listNonExistingOwner() throws Exception {
        OperationResult result = new OperationResult("LIST OWNER");

        PrismObject<UserType> user = repositoryService.listAccountShadowOwner("12345", result);
        AssertJUnit.assertNull(user);
    }

    @Test
    public void testLinkUnlink() throws Exception {
        // GIVEN
        OperationResult result = new OperationResult("testLinkUnlink");
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user.xml"));
        String userOid = repositoryService.addObject(user, null, result);
        assertNotNull("User oid is null", userOid);
        AssertJUnit.assertEquals("user oid is not equal to returned value", userOid, user.getOid());
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "account-shadow.xml"));
        String accountOid = repositoryService.addObject(account, null, result);
        assertNotNull("Account oid is null, couldn't add account or what?", account);
        AssertJUnit.assertEquals("account oid is not equal to returned value", accountOid, account.getOid());
        // precondition
        PrismObject<UserType> accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertNull("Account has owner and should not have (precondition)", accountOwnerOid);

        // WHEN (link account)
        Collection<? extends ItemDelta> modifications = prismContext.deltaFactory().reference().createModificationAddCollection(UserType.class,
                UserType.F_LINK_REF, account);
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        // THEN
        accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertEquals("listAccountShadowOwner returned wrong value", userOid, accountOwnerOid);

        // WHEN (unlink account)
        modifications = prismContext.deltaFactory().reference().createModificationDeleteCollection(UserType.class, UserType.F_LINK_REF,
                account);
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        // THEN
        accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertNull("listAccountShadowOwner returned non-null value after unlink", accountOwnerOid);
    }

    private void assertEquals(String string, String userOid, PrismObject<UserType> accountOwner) {
        AssertJUnit.assertEquals(string, userOid, accountOwner != null ? accountOwner.getOid() : null);
    }
}
