/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ListAccountShadowOwnerTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ListAccountShadowOwnerTest.class);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void listExistingOwner() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //insert sample data
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(OBJECTS_FILE);
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            repositoryService.addObject(object, result);
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
        LOGGER.info("==[testLinkUnlink]==");
        // GIVEN
        OperationResult result = new OperationResult("testLinkUnlink");
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user.xml"));
        String userOid = repositoryService.addObject(user, result);
        assertNotNull("User oid is null", userOid);
        AssertJUnit.assertEquals("user oid is not equal to returned value", userOid, user.getOid());
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "account-shadow.xml"));
        String accountOid = repositoryService.addObject(account, result);
        assertNotNull("Account oid is null, couldn't add account or what?", account);
        AssertJUnit.assertEquals("account oid is not equal to returned value", accountOid, account.getOid());
        // precondition
        PrismObject<UserType> accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertNull("Account has owner and should not have (precondition)", accountOwnerOid);

        // WHEN (link account)
        Collection<? extends ItemDelta> modifications = ReferenceDelta.createModificationAddCollection(UserType.class,
                UserType.F_ACCOUNT_REF, prismContext, account);
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        // THEN
        accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertEquals("listAccountShadowOwner returned wrong value", userOid, accountOwnerOid);

        // WHEN (unlink account)
        modifications = ReferenceDelta.createModificationDeleteCollection(UserType.class, UserType.F_ACCOUNT_REF,
                prismContext, account);
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        // THEN
        accountOwnerOid = repositoryService.listAccountShadowOwner(accountOid, result);
        assertNull("listAccountShadowOwner returned non-null value after unlink", accountOwnerOid);
    }

    /**
     * @param string
     * @param userOid
     * @param accountOwnerOid
     */
    private void assertEquals(String string, String userOid, PrismObject<UserType> accountOwnerOid) {
        // TODO Auto-generated method stub

    }
}
