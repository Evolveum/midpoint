/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.RepoShadowAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

public abstract class AbstractOpenDjTest extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/opendj");

    public static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    static final File RESOURCE_OPENDJ_INITIALIZED_FILE = new File(TEST_DIR, "resource-opendj-initialized.xml");
    public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

    static final File RESOURCE_OPENDJ_BAD_CREDENTIALS_FILE = new File(TEST_DIR, "resource-opendj-bad-credentials.xml");
    static final String RESOURCE_OPENDJ_BAD_CREDENTIALS_OID = "8bc3ff5a-ef5d-11e4-8bba-001e8c717e5b";

    static final File RESOURCE_OPENDJ_BAD_BIND_DN_FILE = new File(TEST_DIR, "resource-opendj-bad-bind-dn.xml");
    static final String RESOURCE_OPENDJ_BAD_BIND_DN_OID = "d180258a-ef5f-11e4-8737-001e8c717e5b";

    static final File ACCOUNT_JBOND_FILE = new File (TEST_DIR, "account-jbond.xml");
    static final File ACCOUNT_JBOND_REPO_FILE = new File(TEST_DIR, "account-jbond-repo.xml");
    static final String ACCOUNT_JBOND_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1cccc";

    protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
    protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
    static final String ACCOUNT_WILL_DN = "uid=will,ou=People,dc=example,dc=com";

    static final File ACCOUNT_POLY_FILE = new File(TEST_DIR, "account-poly.xml");
    static final String ACCOUNT_POLY_OID = "cef31578-5493-11e9-bbed-17f032005a6b";
    static final String ACCOUNT_POLY_DN = "uid=poly,ou=People,dc=example,dc=com";
    static final String ACCOUNT_POLY_DESCRIPTION_ORIG = "Poly the Parrot";
    // TODO why are these unused?
    protected static final String ACCOUNT_POLY_DESCRIPTION_EN = "Polly the Parrot";
    protected static final String ACCOUNT_POLY_DESCRIPTION_SK = "Papagáj Poly";
    protected static final String ACCOUNT_POLY_DESCRIPTION_CZ = "Papoušek Poly";
    protected static final String ACCOUNT_POLY_DESCRIPTION_HR = "Papiga Poly";
    protected static final String ACCOUNT_POLY_DESCRIPTION_RU = "Попугай Поли";

    private static final File ACCOUNT_BAD_REPO_FILE = new File(TEST_DIR, "account-bad-repo.xml");
    static final String ACCOUNT_BAD_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1ffff";

    static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack.xml");
    static final File ACCOUNT_JACK_REPO_FILE = new File(TEST_DIR, "account-jack-repo.xml");
    static final String ACCOUNT_JACK_OID = "c0c010c0-d34d-b44f-f11d-333222444555";
    static final File ACCOUNT_JACK_CHANGE_FILE = new File(TEST_DIR, "account-jack-change.xml");

    static final String ACCOUNT_BARBOSSA_DN = "uid=hbarbossa,ou=People,dc=example,dc=com";

    static final File ACCOUNT_MODIFY_PASSWORD_FILE = new File(TEST_DIR, "account-modify-password.xml");
    static final String ACCOUNT_MODIFY_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222444566";

    static final File ACCOUNT_SPARROW_FILE = new File(TEST_DIR, "account-sparrow.xml");
    static final File ACCOUNT_SPARROW_REPO_FILE = new File(TEST_DIR, "account-sparrow-repo.xml");
    static final String ACCOUNT_SPARROW_OID = "c0c010c0-d34d-b44f-f11d-333222654321";

    static final File ACCOUNT_SEARCH_ITERATIVE_FILE = new File(TEST_DIR, "account-search-iterative.xml");
    static final String ACCOUNT_SEARCH_ITERATIVE_OID = "c0c010c0-d34d-b44f-f11d-333222666666";

    static final File ACCOUNT_SEARCH_FILE = new File(TEST_DIR, "account-search.xml");
    static final String ACCOUNT_SEARCH_OID = "c0c010c0-d34d-b44f-f11d-333222777777";

    static final File ACCOUNT_NEW_WITH_PASSWORD_FILE = new File(TEST_DIR, "account-new-with-password.xml");
    static final String ACCOUNT_NEW_WITH_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222124422";

    static final File ACCOUNT_NEW_DISABLED_FILE = new File (TEST_DIR, "account-new-disabled.xml");
    static final String ACCOUNT_NEW_DISABLED_OID = "c0c010c0-d34d-b44f-f11d-d3d2d2d2d4d2";

    static final File ACCOUNT_NEW_ENABLED_FILE = new File (TEST_DIR, "account-new-enabled.xml");
    static final String ACCOUNT_NEW_ENABLED_OID = "c0c010c0-d34d-b44f-f11d-d3d2d2d2d4d3";

    static final File ACCOUNT_DISABLE_SIMULATED_FILE = new File(TEST_DIR, "account-disable-simulated-opendj.xml");
    static final String ACCOUNT_DISABLE_SIMULATED_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1aaaa";

    static final File ACCOUNT_POSIX_MCMUTTON_FILE = new File (TEST_DIR, "account-posix-mcmutton.xml");
    static final String ACCOUNT_POSIX_MCMUTTON_OID = "3a1902a4-14d8-11e5-b0b5-001e8c717e5b";
    static final String ACCOUNT_POSIX_MCMUTTON_DN = "uid=mcmutton,ou=People,dc=example,dc=com";
    static final File ACCOUNT_POSIX_MCMUTTON_CHANGE_FILE = new File (TEST_DIR, "account-posix-mcmutton-change.xml");

    static final File ACCOUNT_POSIX_VANHELGEN_LDIF_FILE = new File(TEST_DIR, "vanhelgen.ldif");

    static final File REQUEST_DISABLE_ACCOUNT_SIMULATED_FILE = new File(TEST_DIR, "disable-account-simulated.xml");

    static final File ACCOUNT_NO_SN_FILE = new File(TEST_DIR, "account-opendj-no-sn.xml");

    protected static final File ACCOUNT_MORGAN_FILE = new File(TEST_DIR, "account-morgan.xml");
    static final String ACCOUNT_MORGAN_OID = "8dfcf05e-c571-11e3-abbd-001e8c717e5b";
    static final String ACCOUNT_MORGAN_DN = "uid=morgan,ou=People,dc=example,dc=com";

    static final File GROUP_SWASHBUCKLERS_FILE = new File(TEST_DIR, "group-swashbucklers.xml");
    static final String GROUP_SWASHBUCKLERS_OID = "3d96846e-c570-11e3-a80f-001e8c717e5b";
    static final String GROUP_SWASHBUCKLERS_DN_ORIG = "cn=SwashBucklers,ou=GrOuPs,dc=example,dc=COM";
    static final String GROUP_SWASHBUCKLERS_DN_NORM = "cn=swashbucklers,ou=groups,dc=example,dc=com";

    static final File GROUP_SPECIALISTS_FILE = new File(TEST_DIR, "group-specialists.xml");
    static final String GROUP_SPECIALISTS_OID = "3da6ddca-cc0b-11e5-9b3f-2b7f453dbfb3";
    static final String GROUP_SPECIALISTS_DN_ORIG = "cn=specialists,ou=specialgroups,dc=example,dc=COM";
    static final String GROUP_SPECIALISTS_DN_NORM = "cn=specialists,ou=specialgroups,dc=example,dc=com";

    static final File GROUP_CORSAIRS_FILE = new File(TEST_DIR, "group-corsairs.xml");
    static final String GROUP_CORSAIRS_OID = "70a1f3ee-4b5b-11e5-95d0-001e8c717e5b";
    static final String GROUP_CORSAIRS_DN = "cn=corsairs,ou=groups,dc=example,dc=com";

    static final File OU_SUPER_FILE = new File(TEST_DIR, "ou-super.xml");
    static final String OU_SUPER_OID = "1d1e519e-0d22-11ea-8cdf-3f09f7f3a585";
    static final String OU_SUPER_DN_ORIG = "ou=Super,dc=example,dc=com";
    static final String OU_SUPER_DN_NORM = "ou=super,dc=example,dc=com";

    static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(NS_RI, "inetOrgPerson");
    static final QName RESOURCE_OPENDJ_GROUP_OBJECTCLASS = new QName(NS_RI, "groupOfUniqueNames");
    static final QName RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS = new QName(NS_RI, "posixAccount");
    static final String ATTRIBUTE_DESCRIPTION_NAME = "description";
    static final ItemName ATTRIBUTE_DESCRIPTION_QNAME = new ItemName(NS_RI, ATTRIBUTE_DESCRIPTION_NAME);

    static final File QUERY_COMPLEX_FILTER_FILE = new File(TEST_DIR, "query-complex-filter.xml");
    static final File QUERY_COMPLEX_FILTER_STARTS_WITH_FILE = new File(TEST_DIR, "query-complex-filter-starts-with.xml");
    static final File QUERY_ALL_ACCOUNTS_FILE = new File(TEST_DIR, "query-filter-all-accounts.xml");
    static final File QUERY_ALL_LDAP_GROUPS_FILE = new File(TEST_DIR, "query-filter-all-ldap-groups.xml");
    static final File QUERY_VANHELGEN_FILE = new File(TEST_DIR, "query-vanhelgen.xml");

    static final String OBJECT_CLASS_INETORGPERSON_NAME = "inetOrgPerson";
    static final QName OBJECT_CLASS_INETORGPERSON_QNAME = new QName(NS_RI, "inetOrgPerson");
    static final QName GROUP_UNIQUE_MEMBER_ATTR_QNAME = new QName(NS_RI, "uniqueMember");

    static final QName ASSOCIATION_GROUP_NAME = new QName(NS_RI, "group");

    MatchingRule<String> dnMatchingRule;

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceBean;
    protected PrismObject<ConnectorType> connector;

    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        resource = addResourceFromFile(getResourceOpenDjFile(), IntegrationTestTools.CONNECTOR_LDAP_TYPE, initResult);
        repoAddShadowFromFile(ACCOUNT_BAD_REPO_FILE, initResult);

        dnMatchingRule = matchingRuleRegistry.getMatchingRule(PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, DOMUtil.XSD_STRING);
    }

    @SafeVarargs
    protected final <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow.asObjectable(), attrName, expectedValues);
    }

    ItemName getPrimaryIdentifierQName() {
        return new ItemName(NS_RI, OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME);
    }

    ItemName getSecondaryIdentifierQName() {
        return OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER;
    }

    void doStartLdap() throws Exception {
        logger.info("------------------------------------------------------------------------------");
        logger.info("START:  {}", getClass().getSimpleName());
        logger.info("------------------------------------------------------------------------------");
        try {
            openDJController.startCleanServer();
        } catch (IOException ex) {
            logger.error("Couldn't start LDAP.", ex);
            throw ex;
        }
    }

    void doStopLdap() {
        openDJController.stop();
        logger.info("------------------------------------------------------------------------------");
        logger.info("STOP:  {}", getClass().getSimpleName());
        logger.info("------------------------------------------------------------------------------");
    }

    private @NotNull ResourceObjectDefinition getAccountDefaultDefinition() throws SchemaException, ConfigurationException {
        return MiscUtil.stateNonNull(
                Resource.of(resourceBean)
                        .getCompleteSchemaRequired()
                        .getObjectTypeDefinition(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT),
                "No account/default definition in %s", resourceBean);
    }

    private @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        return InternalsConfig.isShadowCachingOnByDefault() ?
                getAccountDefaultDefinition().getAttributeNames() :
                getAccountDefaultDefinition().getAllIdentifiersNames();
    }

    /** TODO reconcile with {@link #assertRepoShadow(String)} */
    RepoShadowAsserter<Void> assertRepoShadowNew(@NotNull String oid)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        return assertRepoShadow(oid, getCachedAccountAttributes());
    }
}
