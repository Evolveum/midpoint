/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.*;

import org.opends.server.types.DirectoryException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Recon should delete resourceAttributes
 * <p>
 * resourceAttributes title and givenName have strong mappings
 * title has source honorificPrefix
 * givenName has source givenName
 * <p>
 * focus attributes honorificPrefix and/or givenName do not exist and resourceAttributes  title and givenName are added manually
 * -> reconcile should remove resourceAtributes
 * as of git-v3.7.1-57-gc5757c3b0d this seems to work for honorificPrefix/title but not for givenName/givenName
 *
 * @author michael gruber
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconNullValue extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "recon-null-value");

    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
    public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";

    private static final String USER_0_NAME = "User0";

    private static final String LDAP_INTENT_DEFAULT = "default";

    private static final String ACCOUNT_ATTRIBUTE_TITLE = "title";
    private static final String ACCOUNT_ATTRIBUTE_GIVENNAME = "givenName";

    @Override
    protected String getTopOrgOid() {
        return ORG_TOP_OID;
    }

    private File getTestDir() {
        return TEST_DIR;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        //Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, new File(getTestDir(), "resource-opendj.xml"), RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        //org
        importObjectFromFile(new File(getTestDir(), "org-top.xml"), initResult);

        //role
        importObjectFromFile(new File(getTestDir(), "role-ldap.xml"), initResult);

        //template
        importObjectFromFile(new File(getTestDir(), "object-template-user.xml"), initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
        dumpLdap();
        display("FINISHED: test000Sanity");
    }

    @Test
    public void test100CreateUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user0Before = createUser(USER_0_NAME, "givenName0", "familyName0", true);

        // WHEN
        when();
        display("Adding user0", user0Before);
        addObject(user0Before, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user0After = getObjectByName(UserType.class, USER_0_NAME);
        display("user0 after", user0After);

        dumpOrgTree();

        assertShadowAttribute(user0After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, ACCOUNT_ATTRIBUTE_GIVENNAME, "givenName0");

    }

    /**
     * add honorificPrefix
     * <p>
     * in resource account value for title should have been added
     */
    @Test
    public void test130AddHonorificPrefix() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);
        PrismObject<UserType> userNewPrism = userBefore.clone();
        prismContext.adopt(userNewPrism);
        UserType userNew = userNewPrism.asObjectable();
        userNew.setHonorificPrefix(new PolyStringType("Princess"));

        ObjectDelta<UserType> delta = userBefore.diff(userNewPrism);
        displayDumpable("Modifying user with delta", delta);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User after adding attribute honorificPrefix", userAfter);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute deletion", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute deletion", accountModel);

        assertShadowAttribute(userAfter, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, ACCOUNT_ATTRIBUTE_GIVENNAME, "givenName0");
        assertShadowAttribute(userAfter, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, ACCOUNT_ATTRIBUTE_TITLE, "Princess");

    }

    /**
     * delete honorificPrefix and givenName
     * <p>
     * in resource account value for title and givenName should have been deleted
     */
    @Test
    public void test140dDeleteHonorificPrefixGivenName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);
        PrismObject<UserType> userNewPrism = userBefore.clone();
        prismContext.adopt(userNewPrism);
        UserType userNew = userNewPrism.asObjectable();
        userNew.setHonorificPrefix(null);
        userNew.setGivenName(null);

        ObjectDelta<UserType> delta = userBefore.diff(userNewPrism);
        displayDumpable("Modifying user with delta", delta);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User after deleting attribute honorificPrefix", userAfter);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute deletion", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute deletion", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_TITLE));
        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_GIVENNAME));

    }

    /**
     * add title in resource account (not using midpoint)
     * do recompute
     * in resource account value for title should have been removed again
     */
    @Test
    public void test150RemoveTitleRA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);

        openDJController.executeLdifChange("dn: uid=" + USER_0_NAME + ",ou=people,dc=example,dc=com\n" +
                "changetype: modify\n" +
                "add: title\n" +
                "title: Earl");

        display("LDAP after addition");
        dumpLdap();

        // WHEN
        when();
        modelService.recompute(UserType.class, userBefore.getOid(), null, task, result);

        display("LDAP after reconcile");
        dumpLdap();

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User smack after adding attribute title", userAfter);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute addition", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute addition", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_TITLE));

    }

    /**
     * add givenName in resource account (not using midpoint)
     * do recompute
     * in resource account value for givenName should have been removed again
     * See also https://wiki.evolveum.com/display/midPoint/Resource+Schema+Handling#ResourceSchemaHandling-AttributeTolerance
     */
    @Test //MID-4567
    public void test160SetGivenNameAttributeAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);

        openDJController.executeLdifChange("dn: uid=" + USER_0_NAME + ",ou=people,dc=example,dc=com\n" +
                "changetype: modify\n" +
                "replace: givenName\n" +
                "givenName: given0again");

        display("LDAP after addition");
        dumpLdap();

        // WHEN
        when();
        modelService.recompute(UserType.class, userBefore.getOid(), null, task, result);

        display("LDAP after reconcile");
        dumpLdap();

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User smack after adding attribute title", userAfter);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute addition", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute addition", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_GIVENNAME));

    }

    /**
     * See also https://wiki.evolveum.com/display/midPoint/Resource+Schema+Handling#ResourceSchemaHandling-AttributeTolerance
     */
    @Test //MID-4567
    public void test170ReplaceGivenNameEmpty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);

        openDJController.executeLdifChange("dn: uid=" + USER_0_NAME + ",ou=people,dc=example,dc=com\n" +
                "changetype: modify\n" +
                "replace: givenName\n" +
                "givenName: given1again");

        display("LDAP after addition");
        dumpLdap();

        // WHEN
        when();
        modifyUserReplace(userBefore.getOid(), UserType.F_GIVEN_NAME, task, result /* no value */);

        display("LDAP after reconcile");
        dumpLdap();

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User smack after adding attribute title", userAfter);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute addition", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute addition", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_GIVENNAME));

    }

    private void dumpLdap() throws DirectoryException {
        displayValue("LDAP server tree", openDJController.dumpTree());
        displayValue("LDAP server content", openDJController.dumpEntries());
    }

    protected <F extends FocusType> PrismObject<F> getObjectByName(Class clazz, String name)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<F> object = (PrismObject<F>) findObjectByName(clazz, name);
        assertNotNull("The object " + name + " of type " + clazz + " is missing!", object);
        display(clazz + " " + name, object);
        PrismAsserts.assertPropertyValue(object, F.F_NAME, PrismTestUtil.createPolyString(name));
        return object;
    }

    private void assertShadowAttribute(PrismObject focus, ShadowKindType kind,
            String intent, String attribute, String... values)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String focusName = focus.getName().toString();
        displayValue("assert focus " + focus.getCompileTimeClass(), focusName);

        String objOid = getLinkRefOid(focus, RESOURCE_OPENDJ_OID, kind, intent);
        PrismObject<ShadowType> objShadow = getShadowModel(objOid);
        display("Focus " + focusName + " kind " + kind + " intent " + intent + " shadow", objShadow);

        List<String> valuesList = new ArrayList<>(Arrays.asList(values));

        for (Object att : objShadow.asObjectable().getAttributes().asPrismContainerValue().getItems()) {
            if (att instanceof ResourceAttribute) {
                Collection propVals = ((ResourceAttribute) att).getRealValues();

                if (attribute.equals(((ResourceAttribute) att).getNativeAttributeName())) {

                    List<String> propValsString = new ArrayList<>(propVals.size());
                    for (Object pval : propVals) {
                        propValsString.add(pval.toString());
                    }

                    Collections.sort(propValsString);
                    Collections.sort(valuesList);

                    assertEquals(propValsString, valuesList);

                }
            }
        }
    }
}
