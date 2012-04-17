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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../application-context-sql-no-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class ModifyTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyTest.class);
    private static final File TEST_DIR = new File("./src/test/resources/modify");

    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;

    @Test(expectedExceptions = ObjectNotFoundException.class)
    public void modifyNotExistingUser() throws Exception {
        System.out.println("=== [ modifyNotExistingUser ] ===");

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        OperationResult result = new OperationResult("MODIFY");
        repositoryService.modifyObject(UserType.class, "1234", deltas, result);
    }

    @Test
    public void modifyUserOnNonExistingAccountTest() throws Exception {
        System.out.println("=== [ modifyUserOnNonExistingAccountTest ] ===");

        OperationResult result = new OperationResult("MODIFY");

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName("non-existing-account-user");

        String oid = repositoryService.addObject(user, result);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add-non-existing.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test
    public void modifyUserOnExistingAccountTest() throws Exception {
        System.out.println("=== [ modifyUserOnExistingAccountTest ] ===");

        OperationResult result = new OperationResult("MODIFY");

        //add account
        File accountFile = new File(TEST_DIR, "account.xml");
        PrismObject<AccountShadowType> account = prismContext.getPrismDomProcessor().parseObject(accountFile);
        repositoryService.addObject(account, result);

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);

        String userOid = user.getOid();
        String oid = repositoryService.addObject(user, result);
        AssertJUnit.assertEquals(userOid, oid);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }
}
