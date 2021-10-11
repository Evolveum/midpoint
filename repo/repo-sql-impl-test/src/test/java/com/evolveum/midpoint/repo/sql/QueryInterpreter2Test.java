/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_ID;
import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.query.OrderDirection.ASCENDING;
import static com.evolveum.midpoint.prism.query.OrderDirection.DESCENDING;
import static com.evolveum.midpoint.schema.GetOperationOptions.createDistinct;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.util.QNameUtil.unqualify;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_OWNER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_CONSTRUCTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType.F_RESOURCE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_CREATE_APPROVER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_CREATOR_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.QueryEngine2;
import com.evolveum.midpoint.repo.sql.query2.RQueryImpl;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * @author lazyman
 * @author mederly
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryInterpreter2Test extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("./src/test/resources/query");

    private static final QName FOO_QNAME = new QName("http://midpoint.evolveum.com/blabla", "foo");
    private static final QName SHOE_SIZE_QNAME = new QName("http://example.com/xml/ns/mySchema", "shoeSize");
    private static final QName A1_QNAME = new QName("", "a1");
    private static final QName STRING_TYPE_QNAME = new QName("http://example.com/p", "stringType");
    private static final QName INT_TYPE_QNAME = new QName("http://example.com/p", "intType");
    private static final QName LONG_TYPE_QNAME = new QName("http://example.com/p", "longType");
    private static final QName WEAPON_QNAME = new QName("http://example.com/p", "weapon");
    private static final QName OVERRIDE_ACTIVATION_QNAME = new QName("http://example.com/p", "overrideActivation");
    private static final QName ACTIVATION_STATUS_TYPE_QNAME = new QName(SchemaConstants.NS_C, ActivationStatusType.class.getSimpleName());
    private static final QName SKIP_AUTOGENERATION_QNAME = new QName("http://example.com/p", "skipAutogeneration");

    private RExtItem fooDefinition;
    private RExtItem shoeSizeDefinition;
    private RExtItem a1Definition;
    private RExtItem stringTypeDefinition;
    private RExtItem intTypeDefinition;
    private RExtItem longTypeDefinition;
    private RExtItem weaponDefinition;
    private RExtItem overrideActivationDefinition;
    private RExtItem skipAutogenerationDefinition;

    @Override
    public void initSystem() throws Exception {
        List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(
                new File(FOLDER_BASIC, "objects.xml")).parseObjects();
        OperationResult result = new OperationResult("add objects");
        for (PrismObject<?> object : objects) {
            //noinspection unchecked
            repositoryService.addObject((PrismObject<? extends ObjectType>) object, null, result);
        }

        prepareItemDefinitions();

        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    private void prepareItemDefinitions() {
        DefinitionFactory factory = prismContext.definitionFactory();
        fooDefinition = extItemDictionary.createOrFindItemDefinition(factory.createPropertyDefinition(FOO_QNAME, DOMUtil.XSD_STRING), false);
        shoeSizeDefinition = extItemDictionary.createOrFindItemDefinition(factory.createPropertyDefinition(SHOE_SIZE_QNAME, DOMUtil.XSD_INT), false);
        a1Definition = extItemDictionary.createOrFindItemDefinition(factory.createPropertyDefinition(A1_QNAME, DOMUtil.XSD_STRING), false);
        stringTypeDefinition = extItemDictionary.findItemByDefinition(factory.createPropertyDefinition(STRING_TYPE_QNAME, DOMUtil.XSD_STRING));
        intTypeDefinition = extItemDictionary.findItemByDefinition(factory.createPropertyDefinition(INT_TYPE_QNAME, DOMUtil.XSD_INT));
        longTypeDefinition = extItemDictionary.findItemByDefinition(factory.createPropertyDefinition(LONG_TYPE_QNAME, DOMUtil.XSD_LONG));
        weaponDefinition = extItemDictionary.createOrFindItemDefinition(factory.createPropertyDefinition(WEAPON_QNAME, DOMUtil.XSD_STRING), false);
        overrideActivationDefinition = extItemDictionary.createOrFindItemDefinition(factory.createPropertyDefinition(OVERRIDE_ACTIVATION_QNAME, ACTIVATION_STATUS_TYPE_QNAME), false);
        skipAutogenerationDefinition = extItemDictionary.findItemByDefinition(factory.createPropertyDefinition(SKIP_AUTOGENERATION_QNAME, DOMUtil.XSD_BOOLEAN));
    }

    @Test
    public void test0001QueryNameNorm() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringNorm)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingNorm().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.nameCopy.norm = :norm";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0002QueryNameOrig() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingOrig().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.nameCopy.orig = :orig";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0003QueryNameStrict() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  ( u.nameCopy.orig = :orig and u.nameCopy.norm = :norm )";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0010QueryOrganizationNorm() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (organization, "...", PolyStringNorm)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("guľôčka v jamôčke").matchingNorm().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "where\n" +
                    "  o.norm = :norm";

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            assertEqualsIgnoreWhitespace(expected, real);

            assertEquals("Wrong parameter value", "gulocka v jamocke", rQuery.getQuerySource().getParameters().get("norm").getValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0011QueryOrganizationOrig() throws Exception {
        Session session = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingOrig().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "where\n" +
                    "  o.orig = :orig";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0012QueryOrganizationStrict() throws Exception {
        Session session = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf")
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingStrict().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "where\n" +
                    "  ( o.orig = :orig and o.norm = :norm )";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0020QueryTwoOrganizationsNormAnd() throws Exception {
        Session session = open();
        try {
            /*
             *  UserType: And (Equal (organization, 'asdf', PolyStringNorm),
             *                 Equal (organization, 'ghjk', PolyStringNorm))
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingNorm()
                    .and().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk").matchingNorm()
                    .build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "    left join u.organization o2\n" +
                    "where\n" +
                    "  ( o.norm = :norm and o2.norm = :norm2 )";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0021QueryTwoOrganizationsStrictOr() throws Exception {
        Session session = open();
        try {
            /*
             *  UserType: Or (Equal (organization, 'asdf'),
             *                Equal (organization, 'ghjk'))
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf")
                    .or().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk")
                    .build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "    left join u.organization o2\n" +
                    "where\n" +
                    "  ( ( o.orig = :orig and o.norm = :norm ) or\n" +
                    "  ( o2.orig = :orig2 and o2.norm = :norm2 ) )";

            // NOTE: this could be implemented more efficiently by using only one join... or the query itself can be formulated
            // via In filter (when available) or Exists filter (also, when available)

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0025QueryOrganizationOrigPolymorphic() throws Exception {
        Session session = open();
        try {
            /*
             *  ### object: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingOrig()
                    .build();
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.organization o2\n" +
                    "where\n" +
                    "  o2.orig = :orig";

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0030QueryTaskDependent() throws Exception {
        Session session = open();

        try {
            /*
             *  ### task: Equal (dependent, "123456")
             */
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_DEPENDENT).eq("123456")
                    .build();

            String expected = "select\n" +
                    "  t.oid, t.fullObject\n" +
                    "from\n" +
                    "  RTask t\n" +
                    "    left join t.dependent d\n" +
                    "where\n" +
                    "  d = :d";

            String real = getInterpretedQuery2(session, TaskType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void test0040QueryClob() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_DESCRIPTION).eq("aaa")
                    .build();

            //should throw exception, because description is lob and can't be queried
            getInterpretedQuery2(session, UserType.class, query);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0050QueryEnum() throws Exception {
        Session session = open();
        try {
            /*
             *  ### task: Equal (executionStatus, WAITING)
             */
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING)
                    .build();
            String real = getInterpretedQuery2(session, TaskType.class, query);

            String expected = "select\n" +
                    "  t.oid, t.fullObject\n" +
                    "from\n" +
                    "  RTask t\n" +
                    "where\n" +
                    "  t.executionStatus = :executionStatus\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0060QueryEnabled() throws Exception {
        Session session = open();
        try {
            /*
             *  ### task: Equal (activation/administrativeStatus, ENABLED)
             *  ==> from RUser u where u.activation.administrativeStatus = com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus.ENABLED
             */

            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-enabled.xml"));

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.activation.administrativeStatus = :administrativeStatus\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0070QueryGenericLong() throws Exception {
        Session session = open();
        try {
            /*
             *  ### generic: And (Equal (name, "generic object", PolyStringNorm),
             *                    Equal (c:extension/p:intType, 123))
             *  ==> from RGenericObject g
             *        left join g.longs l (l.ownerType = com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType.EXTENSION and l.name = 'http://example.com/p#intType')
             *      where
             *         g.name.norm = 'generic object' and
             *         l.value = 123
             */

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, GenericObjectType.class,
                    getQuery(new File(TEST_DIR, "query-and-generic.xml"), GenericObjectType.class), false, null);
            String real = realQuery.getQuery().getQueryString();

            String expected = "select\n" +
                    "  g.oid, g.fullObject\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.longs l with ( l.ownerType = :ownerType and l.itemId = :itemId )\n" +
                    "where\n" +
                    "  ( g.nameCopy.norm = :norm and l.value = :value )\n";

            assertEqualsIgnoreWhitespace(expected, real);

            assertEquals("Wrong property ID for 'intType'", intTypeDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0071QueryGenericLongTwice() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(F_NAME).eqPoly("generic object", "generic object").matchingNorm()
                    .and().item(F_EXTENSION, new QName("intType")).ge(100)
                    .and().item(F_EXTENSION, new QName("intType")).lt(200)
                    .and().item(F_EXTENSION, new QName("longType")).eq(335)
                    .build();

            RQuery realQuery = getInterpretedQuery2Whole(session, GenericObjectType.class, query, false, null);
            RootHibernateQuery source = ((RQueryImpl) realQuery).getQuerySource();
            String real = ((RQueryImpl) realQuery).getQuery().getQueryString();

            String expected = "select\n" +
                    "  g.oid, g.fullObject\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.longs l with ( l.ownerType = :ownerType and l.itemId = :itemId )\n" +
                    "    left join g.longs l2 with ( l2.ownerType = :ownerType2 and l2.itemId = :itemId2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    g.nameCopy.norm = :norm and\n" +
                    "    l.value >= :value and\n" +
                    "    l.value < :value2 and\n" +
                    "    l2.value = :value3\n" +
                    "  )";

            // note l and l2 cannot be merged as they point to different extension properties (intType, longType)
            assertEqualsIgnoreWhitespace(expected, real);

            assertEquals("Wrong property ID for 'intType'", intTypeDefinition.getId(), source.getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'longType'", longTypeDefinition.getId(), source.getParameters().get("itemId2").getValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0072QueryAccountByNonExistingAttribute() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-non-existing-attribute.xml"));
            String expected = "select\n" +
                    "  s.oid, s.fullObject\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and 1=0 )\n" +
                    "where\n" +
                    "  s2.value = :value\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0073QueryAccountByAttribute() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attribute.xml"));
            String expected = "select\n" +
                    "  s.oid, s.fullObject\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.itemId = :itemId )\n" +
                    "where\n" +
                    "  s2.value = :value\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0074QueryAccountByAttributeAndExtensionValue() throws Exception {
        Session session = open();
        try {
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, ShadowType.class,
                    getQuery(new File(TEST_DIR, "query-account-by-attribute-and-extension-value.xml"), ShadowType.class), false,
                    null);
            String expected = "select\n" +
                    "  s.oid, s.fullObject\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.itemId = :itemId )\n" +
                    "    left join s.longs l with ( l.ownerType = :ownerType2 and l.itemId = :itemId2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    s2.value = :value and\n" +
                    "    l.value = :value2\n" +
                    "  )";
            assertEqualsIgnoreWhitespace(expected, realQuery.getQuery().getQueryString());

            assertEquals("Wrong property ID for 'a1'", a1Definition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'shoeSize'", shoeSizeDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId2").getValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0076QueryOrComposite() throws Exception {
        Session session = open();
        try {
            /*
             * ### shadow:
             *      Or (
             *        Equal (intent, "some account type"),
             *        Equal (attributes/f:foo, "foo value"),
             *        Equal (extension/p:stringType, "uid=test,dc=example,dc=com"),
             *        Ref (resourceRef, d0db5be9-cb93-401f-b6c1-86ffffe4cd5e))
             *
             * ==> from RShadow r left join r.strings s1 where
             *       r.intent = 'some account type' or
             *         (s1.ownerType = RObjectExtensionType.ATTRIBUTES and s1.name = 'http://midpoint.evolveum.com/blabla#foo' and s1.value = 'foo value') or
             *         (s1.ownerType = RObjectExtensionType.EXTENSION and s1.name = 'http://example.com/p#stringType' and s1.value = 'uid=test,dc=example,dc=com') or
             *         (r.resourceRef.targetOid = 'd0db5be9-cb93-401f-b6c1-86ffffe4cd5e' and r.resourceRef.relation = '#' and r.resourceRef.type = '...#ResourceType')
             *
             *   [If we used AND instead of OR, this SHOULD BE left join r.strings s1, left join r.strings s2]
             */
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, ShadowType.class,
                    getQuery(new File(TEST_DIR, "query-or-composite.xml"), ShadowType.class), false, null);

            String expected = "select\n" +
                    "  s.oid, s.fullObject\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.itemId = :itemId )\n" +
                    "    left join s.strings s3 with ( s3.ownerType = :ownerType2 and s3.itemId = :itemId2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    s.intent = :intent or\n" +
                    "    s2.value = :value or\n" +
                    "    s3.value = :value2 or\n" +
                    "    (\n" +
                    "      s.resourceRef.targetOid = :targetOid and\n" +
                    "      s.resourceRef.relation in (:relation)\n" +
                    "    )\n" +
                    "  )\n";

            /*
                ownerType = ATTRIBUTES (com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType.ATTRIBUTES)
                name = http://midpoint.evolveum.com/blabla#foo
                value = foo value
                ownerType2 = com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType.EXTENSION
                name2 = http://example.com/p#stringType
                value2 = uid=test,dc=example,dc=com
                intent = some account type
                targetOid = d0db5be9-cb93-401f-b6c1-86ffffe4cd5e
                relation = ...
                type = com.evolveum.midpoint.repo.sql.data.common.other.RObjectType.RESOURCE
             */
            assertEqualsIgnoreWhitespace(expected, realQuery.getQuery().getQueryString());
            assertEquals("Wrong property ID for 'foo'", fooDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'stringType'", stringTypeDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId2").getValue());

            System.out.println("Query parameters: " + realQuery.getQuerySource().getParameters());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0080QueryExistsAssignment() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: Exists (assignment, Equal (activation/administrativeStatus = Enabled))
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                    .eq(ActivationStatusType.ENABLED)
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by u.nameCopy.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0081QueryExistsAssignmentWithRedundantBlock() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .block()
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                    .eq(ActivationStatusType.ENABLED)
                    .endBlock()
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by u.nameCopy.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0082QueryExistsAssignmentWithRedundantBlock2() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .block()
                    .exists(F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                    .eq(ActivationStatusType.ENABLED)
                    .endBlock()
                    .endBlock()
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by u.nameCopy.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0084QueryExistsWithAnd() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref("111")
                    .and()
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .build();

            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      s.resourceRef.targetOid = :targetOid and\n"
                    + "      s.resourceRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    s.pendingOperationCount > :pendingOperationCount\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void test0089QueryExistsAssignmentAll() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .exists(F_ASSIGNMENT).all()
                    .asc(F_NAME)
                    .build();

            query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));

            String real = getInterpretedQuery2(session, UserType.class, query);
            // this doesn't work as expected ... maybe inner join would be better! Until implemented, we should throw UOO
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "order by u.name.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0090QuerySingleAssignmentWithTargetAndTenant() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_TARGET_REF).ref("target-oid-123")
                    .and().item(AssignmentType.F_TENANT_REF).ref("tenant-oid-456")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      a.targetRef.targetOid = :targetOid and\n"
                    + "      a.targetRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      u.tenantRef.targetOid = :targetOid2 and\n"
                    + "      u.tenantRef.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0092QueryAssignmentsWithTargetAndTenant() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("target-oid-123")
                    .and().item(UserType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF).ref("tenant-oid-456")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n"
                    + "    left join u.assignments a2 with a2.assignmentOwner = :assignmentOwner2\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      a.targetRef.targetOid = :targetOid and\n"
                    + "      a.targetRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      a2.tenantRef.targetOid = :targetOid2 and\n"
                    + "      a2.tenantRef.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0100QueryObjectByName() throws Exception {
        Session session = open();

        try {
            /*
             * ### object: Equal (name, "cpt. Jack Sparrow")
             *             Order by name, ASC
             *
             * ==> from RObject o where name.orig = 'cpt. Jack Sparrow' and name.norm = 'cpt jack sparrow'
             *        order by name.orig asc
             */
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.name.orig = :orig and\n" +
                    "    o.name.norm = :norm\n" +
                    "  )\n" +
                    "order by o.name.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0110QueryUserByFullName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-fullName.xml"));
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.fullName.norm = :norm\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0112QueryUserSubstringFullName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-substring-fullName.xml"));
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  lower(u.fullName.norm) like :norm";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0114QueryUserByName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-name.xml"));
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.nameCopy.norm = :norm";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0116QuerySubstringMultivalued() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_EMPLOYEE_TYPE).contains("abc")
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.employeeType e\n" +
                    "where\n" +
                    "  e like :e\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0120QueryConnectorByType() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, ConnectorType.class,
                    new File(TEST_DIR, "query-connector-by-type.xml"));
            String expected = "select\n" +
                    "  c.oid, c.fullObject\n" +
                    "from\n" +
                    "  RConnector c\n" +
                    "where\n" +
                    "  c.connectorType = :connectorType\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0130QueryAccountByAttributesAndResourceRef() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attributes-and-resource-ref.xml"));
            String expected = "select\n" +
                    "  s.oid, s.fullObject\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.itemId = :itemId )\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      s.resourceRef.targetOid = :targetOid and\n" +
                    "      s.resourceRef.relation in (:relation)\n" +
                    "    ) and\n" +
                    "    s2.value = :value\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0140QueryUserAccountRef() throws Exception {
        Session session = open();
        try {
            /*
             * ### user: Ref (linkRef, 123)
             *
             * ==> select from RUser u left join u.linkRef l where
             *        l.targetOid = '123' and l.relation = '#'
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).ref("123")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.linkRef l\n" +
                    "where\n" +
                    "  (\n" +
                    "    l.targetOid = :targetOid and\n" +
                    "    l.relation in (:relation)\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0142QueryUserAccountRefNull() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  l is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0144QueryUserAccountRefNotNull() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .not().item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  not l is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0146QueryUserAccountRefByType() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).refType(ShadowType.COMPLEX_TYPE)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  (\n"
                    + "    l.relation in (:relation) and\n"
                    + "    l.type = :type\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0147QueryUserAccountRefByRelation() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).refRelation(prismContext.getDefaultRelation())
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  l.relation in (:relation)\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0148QueryUserAccountRefComplex() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue value1 = prismContext.itemFactory().createReferenceValue(null, ShadowType.COMPLEX_TYPE);
            PrismReferenceValue value2 = prismContext.itemFactory().createReferenceValue("abcdef", ShadowType.COMPLEX_TYPE);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).ref(value1, value2)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      l.relation in (:relation) and\n"
                    + "      l.type = :type\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      l.targetOid = :targetOid and\n"
                    + "      l.relation in (:relation2) and\n"
                    + "      l.type = :type2\n"
                    + "    )\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0150QueryUserAssignmentTargetRef() throws Exception {
        Session session = open();
        try {
            /*
             * ### user: Ref (assignment/targetRef, '123', RoleType)
             *
             * ==> select from RUser u left join u.assignments a where
             *        a.assignmentOwner = RAssignmentOwner.FOCUS and
             *        a.targetRef.targetOid = '123' and
             *        a.targetRef.relation = '#' and
             *        a.targetRef.type = RObjectType.ROLE
             */
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid("123");
            ort.setType(RoleType.COMPLEX_TYPE);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.targetRef.targetOid = :targetOid and\n" +
                    "    a.targetRef.relation in (:relation) and\n" +
                    "    a.targetRef.type = :type\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));
        } finally {
            close(session);
        }
    }

    @NotNull
    private List<String> getVariantsOfDefaultRelation() {
        return Arrays.asList("#", RUtil.qnameToString(unqualify(SchemaConstants.ORG_DEFAULT)), RUtil.qnameToString(SchemaConstants.ORG_DEFAULT));
    }

    @Test
    public void test0152QueryUserAssignmentTargetRefManagerStandardQualified() throws Exception {
        Session session = open();
        try {
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(SchemaConstants.ORG_MANAGER);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.targetRef.targetOid = :targetOid and\n" +
                    "    a.targetRef.relation in (:relation) and\n" +
                    "    a.targetRef.type = :type\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            System.out.println("relationParameter: " + relationParameter);
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));
        } finally {
            close(session);
        }
    }

    @Test
    public void test0153QueryUserAssignmentTargetRefManagerCustomQualified() throws Exception {
        Session session = open();
        try {
            QName auditorRelation = new QName("http://x/", "auditor");
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(auditorRelation);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.targetRef.targetOid = :targetOid and\n" +
                    "    a.targetRef.relation = :relation and\n" +
                    "    a.targetRef.type = :type\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);

            String relationParameter = (String) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value", RUtil.qnameToString(auditorRelation), relationParameter);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0154QueryUserAssignmentTargetRefManagerUnqualified() throws Exception {
        Session session = open();
        try {
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER));
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.targetRef.targetOid = :targetOid and\n" +
                    "    a.targetRef.relation in (:relation) and\n" +
                    "    a.targetRef.type = :type\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));
        } finally {
            close(session);
        }
    }

    @Test
    public void test0160QueryTrigger() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(ObjectType.F_TRIGGER, F_TIMESTAMP).le(thisScanTimestamp)
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.trigger t\n" +
                    "where\n" +
                    "  t.timestamp <= :timestamp\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0162QueryTriggerBeforeAfter() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar lastScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(ObjectType.F_TRIGGER)
                    .block()
                    .item(F_TIMESTAMP).gt(lastScanTimestamp)
                    .and().item(F_TIMESTAMP).le(thisScanTimestamp)
                    .endBlock()
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.trigger t\n" +
                    "where\n" +
                    "  ( t.timestamp > :timestamp and t.timestamp <= :timestamp2 )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0170QueryAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0180QueryInducementActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            /*
             * ### role: Equal (inducement/activation/administrativeStatus, ENABLED)
             *
             * ==> select from RRole r left join r.assignments a where
             *          a.assignmentOwner = RAssignmentOwner.ABSTRACT_ROLE and                  <--- this differentiates inducements from assignments
             *          a.activation.administrativeStatus = RActivationStatus.ENABLED
             */
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();

            String real = getInterpretedQuery2(session, RoleType.class, query);

            String expected = "select\n" +
                    "  r.oid, r.fullObject\n" +
                    "from\n" +
                    "  RRole r\n" +
                    "    left join r.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n";

            // assignmentOwner = com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner.ABSTRACT_ROLE
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0182QueryInducementAndAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            /*
             * ### Role: Or (Equal (assignment/activation/administrativeStatus, RActivationStatus.ENABLED),
             *               Equal (inducement/activation/administrativeStatus, RActivationStatus.ENABLED))
             */
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .or().item(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();
            String real = getInterpretedQuery2(session, RoleType.class, query);

            String expected = "select\n" +
                    "  r.oid, r.fullObject\n" +
                    "from\n" +
                    "  RRole r\n" +
                    "    left join r.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "    left join r.assignments a2 with a2.assignmentOwner = :assignmentOwner2\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.activation.administrativeStatus = :administrativeStatus or\n" +
                    "    a2.activation.administrativeStatus = :administrativeStatus2\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0190QueryUserByActivationDouble() throws Exception {
        Date NOW = new Date();

        Session session = open();
        try {
            /*
             * ### user: And (Equal (activation/administrativeStatus, RActivationStatus.ENABLED),
             *                Equal (activation/validFrom, '...'))
             *
             * ==> select u from RUser u where u.activation.administrativeStatus = RActivationStatus.ENABLED and
             *                                 u.activation.validFrom = ...
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .and().item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).eq(XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime()))
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.activation.administrativeStatus = :administrativeStatus and\n" +
                    "    u.activation.validFrom = :validFrom\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0200QueryTriggerTimestampDoubleWrong() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(ObjectType.F_TRIGGER, F_TIMESTAMP).gt(thisScanTimestamp)
                    .and().item(ObjectType.F_TRIGGER, F_TIMESTAMP).lt(thisScanTimestamp)
                    .build();
            logger.info(query.debugDump());
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            // correct translation but the filter is wrong: we need to point to THE SAME timestamp -> i.e. ForValue should be used here
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.trigger t\n" +
                    "    left join o.trigger t2\n" +
                    "where\n" +
                    "  (\n" +
                    "    t.timestamp > :timestamp and\n" +
                    "    t2.timestamp < :timestamp2\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0300CountObjectOrderByName() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .asc(F_NAME).build();

            String real = getInterpretedQuery2(session, UserType.class, query, true);
            String expected = "select\n" +
                    "  count(u.oid)\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.nameCopy.orig = :orig and\n" +
                    "    u.nameCopy.norm = :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0310CountObjectOrderByNameWithoutFilter() throws Exception {
        Session session = open();

        try {
            ObjectPaging paging = prismContext.queryFactory().createPaging(null, null, F_NAME, ASCENDING);
            ObjectQuery query = prismContext.queryFactory().createQuery(null, paging);

            String real = getInterpretedQuery2(session, ObjectType.class, query, true);
            String expected = "select\n" +
                    "  count(o.oid)\n" +
                    "from\n" +
                    "  RObject o\n";     // ordering does not make sense here
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    /**
     * Q{AND: (EQUALS: parent, PPV(null)),PAGING: O: 0,M: 5,BY: name, D:ASCENDING,
     */
    @Test
    public void test0320CountTaskOrderByName() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_PARENT).isNull()
                    .asc(F_NAME)
                    .build();
            String real = getInterpretedQuery2(session, TaskType.class, query, true);
            String expected = "select\n" +
                    "  count(t.oid)\n" +
                    "from\n" +
                    "  RTask t\n" +
                    "where\n" +
                    "  t.parent is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0330InOidTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .id("1", "2").build();

            String real = getInterpretedQuery2(session, ObjectType.class, query, false);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.oid in (:oid)\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0331InOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .id(new String[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test0332OwnerInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .ownerId(new String[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test0333LongInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .id(new long[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test0334LongOwnerInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .ownerId(new long[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test0335OwnerInOidTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("1", "2").build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "where\n" +
                    "  a.ownerOid in (:ownerOid)";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0340QueryOrgTreeFindOrgs() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isDirectChildOf("some oid")
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +

                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.targetOid = :orgOid)\n" +
                    "order by o.nameCopy.orig asc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0341QueryOrgTreeFindUsersRelationDefault() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isDirectChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_DEFAULT))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid = :orgOid)\n";

            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));

        } finally {
            close(session);
        }
    }

    @Test
    public void test0342QueryOrgTreeFindUsersRelationManager() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isDirectChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_MANAGER))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid = :orgOid)\n";

            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));

        } finally {
            close(session);
        }
    }

    @Test
    public void test0345QueryOrgAllLevels() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isChildOf(itemFactory().createReferenceValue("123"))
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +

                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "order by o.nameCopy.orig asc";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0346QueryOrgTreeFindUsersRelationDefault() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_DEFAULT))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"

                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n";
            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));

        } finally {
            close(session);
        }
    }

    @Test
    public void test0347QueryOrgTreeFindUsersRelationManager() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_MANAGER))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = rQuery.getQuery().getQueryString();
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"

                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n";

            assertEqualsIgnoreWhitespace(expected, real);

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));

        } finally {
            close(session);
        }
    }

    // MID-4337
    @Test
    public void test0350QuerySubtreeDistinctCount() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isChildOf(itemFactory().createReferenceValue("123"))
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query, true, distinct());
            // we probably do not need 'distinct' here
            String expected = "select\n"
                    + "  count(o.oid)\n"
                    + "from\n"
                    + "  ROrg o\n"
                    + "where\n"
                    + "  o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0355QueryRoots() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isRoot()
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select descendantOid from ROrgClosure group by descendantOid having count(descendantOid) = 1)\n" +
                    "order by o.nameCopy.orig asc";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0400ActivationQueryWrong() throws Exception {
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "    left join u.assignments a2 with a2.assignmentOwner = :assignmentOwner2\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.activation.validFrom <= :validFrom or\n" +
                    "    u.activation.validTo <= :validTo or\n" +
                    "    a.activation.validFrom <= :validFrom2 or\n" +
                    "    a2.activation.validTo <= :validTo2\n" +
                    "  )\n";

            // correct translation but probably not what the requester wants (use Exists instead)

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    // this one uses Exists to refer to the same value of assignment
    @Test
    public void test0405ActivationQueryCorrect() throws Exception {
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .or().exists(F_ASSIGNMENT)
                .block()
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .endBlock()
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.activation.validFrom <= :validFrom or\n" +
                    "    u.activation.validTo <= :validTo or\n" +
                    "    ( a.activation.validFrom <= :validFrom2 or\n" +
                    "      a.activation.validTo <= :validTo2 )\n" +
                    "  )\n";

            // correct translation but probably not what the requester wants (use ForValue instead)

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0410ActivationQueryWrong() throws Exception {
        XMLGregorianCalendar lastScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .block()
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).gt(lastScanTimestamp)
                .and().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .endBlock()
                .or().block()
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).gt(lastScanTimestamp)
                .and().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .endBlock()
                .or().block()
                .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).gt(lastScanTimestamp)
                .and().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .endBlock()
                .or().block()
                .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).gt(lastScanTimestamp)
                .and().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .endBlock()
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "    left join u.assignments a2 with a2.assignmentOwner = :assignmentOwner2\n" +
                    "    left join u.assignments a3 with a3.assignmentOwner = :assignmentOwner3\n" +
                    "    left join u.assignments a4 with a4.assignmentOwner = :assignmentOwner4\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      u.activation.validFrom > :validFrom and\n" +
                    "      u.activation.validFrom <= :validFrom2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      u.activation.validTo > :validTo and\n" +
                    "      u.activation.validTo <= :validTo2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      a.activation.validFrom > :validFrom3 and\n" +
                    "      a2.activation.validFrom <= :validFrom4\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      a3.activation.validTo > :validTo3 and\n" +
                    "      a4.activation.validTo <= :validTo4\n" +
                    "    )\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    // this one uses Exists to refer to the same value of assignment
    @Test
    public void test0415ActivationQueryCorrect() throws Exception {
        XMLGregorianCalendar lastScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .block()
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).gt(lastScanTimestamp)
                .and().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .endBlock()
                .or().block()
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).gt(lastScanTimestamp)
                .and().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .endBlock()
                .or()
                .exists(F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).gt(lastScanTimestamp)
                .and().item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or()
                .item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).gt(lastScanTimestamp)
                .and().item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .endBlock()
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      u.activation.validFrom > :validFrom and\n" +
                    "      u.activation.validFrom <= :validFrom2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      u.activation.validTo > :validTo and\n" +
                    "      u.activation.validTo <= :validTo2\n" +
                    "    ) or\n" +
                    "    ( (\n" +
                    "        a.activation.validFrom > :validFrom3 and\n" +
                    "        a.activation.validFrom <= :validFrom4\n" +
                    "      ) or\n" +
                    "      (\n" +
                    "        a.activation.validTo > :validTo3 and\n" +
                    "        a.activation.validTo <= :validTo4\n" +
                    "      )\n" +
                    "    )\n" +
                    "  )\n";

            // TODO rewrite with ForValue
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0500OrgQuery() throws Exception {
        File objects = new File("src/test/resources/orgstruct/org-monkey-island.xml");
        OperationResult opResult = new OperationResult("test500OrgQuery");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parserFor(objects).parseObjects();

        for (PrismObject<? extends Objectable> o : orgStruct) {
            //noinspection unchecked
            repositoryService.addObject((PrismObject<? extends ObjectType>) o, null, opResult);
        }
        opResult.computeStatusIfUnknown();
        assertTrue(opResult.isSuccess());

        checkQueryResult(ObjectType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.ONE_LEVEL, 4);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.ONE_LEVEL, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.ONE_LEVEL, 3);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.SUBTREE, 5);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.SUBTREE, 6);
        checkQueryResult(ObjectType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.SUBTREE, 11);
        checkQueryResult(ObjectType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.ONE_LEVEL, 4);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.ONE_LEVEL, 4);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.SUBTREE, 4);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.ONE_LEVEL, 0);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.SUBTREE, 0);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-200000000002", OrgFilter.Scope.ONE_LEVEL, 2);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-200000000002", OrgFilter.Scope.SUBTREE, 2);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-200000000001", OrgFilter.Scope.ONE_LEVEL, 1);
        checkQueryResult(UserType.class, "00000000-8888-6666-0000-200000000001", OrgFilter.Scope.SUBTREE, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000001", OrgFilter.Scope.ANCESTORS, 0);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000002", OrgFilter.Scope.ANCESTORS, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000003", OrgFilter.Scope.ANCESTORS, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000004", OrgFilter.Scope.ANCESTORS, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000005", OrgFilter.Scope.ANCESTORS, 2);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-100000000006", OrgFilter.Scope.ANCESTORS, 3);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-200000000000", OrgFilter.Scope.ANCESTORS, 0);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-200000000001", OrgFilter.Scope.ANCESTORS, 1);
        checkQueryResult(OrgType.class, "00000000-8888-6666-0000-200000000002", OrgFilter.Scope.ANCESTORS, 1);
    }

    private <T extends ObjectType> void checkQueryResult(Class<T> type, String oid, OrgFilter.Scope scope, int count)
            throws Exception {
        logger.info("checkQueryResult");

        ObjectQuery query = prismContext.queryFor(type)
                .isInScopeOf(oid, scope)
                .asc(F_NAME)
                .build();

        OperationResult result = new OperationResult("checkQueryResult");
        List<PrismObject<T>> objects = repositoryService.searchObjects(type, query, null, result);
        for (PrismObject<T> object : objects) {
            logger.info("{}", object.getOid());
        }
        assertEquals("Expected count doesn't match for searchObjects " + query, count, objects.size());

        result.computeStatusIfUnknown();
        assertTrue(result.isSuccess());

        int realCount = repositoryService.countObjects(type, query, null, result);
        assertEquals("Expected count doesn't match for countObjects " + query, count, realCount);

        result.computeStatusIfUnknown();
        assertTrue(result.isSuccess());
    }

    @Test
    public void test0510QueryNameAndOrg() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .and().isChildOf("12341234-1234-1234-1234-123412341234")
                    .asc(F_NAME)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      u.nameCopy.orig = :orig and\n" +
                    "      u.nameCopy.norm = :norm\n" +
                    "    ) and\n" +
                    "    u.oid in (select ref.ownerOid from RObjectReference ref " +
                    "               where ref.referenceType = 0 and " +
                    "               ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "  )\n" +
                    "order by u.nameCopy.orig asc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test
//    public void test0520QueryEmployeeTypeAndOrgType() throws Exception {
//        Session session = open();
//
//        try {
//            EqualFilter nameFilter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext,
//                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));
//
//            EqualFilter numberFilter = EqualFilter.createEqual(UserType.F_EMPLOYEE_NUMBER, UserType.class, prismContext,
//                    null, "123");
//
//            EqualsFilter orgTypeFilter = EqualsFilter.createEqual(OrgType.F_ORG_TYPE, OrgType.class, prismContext,
//                    null, "orgtypevalue");
//
//            ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(
//                    AndFilter.createAnd(nameFilter, numberFilter)//,
////                    orgTypeFilter
//            ));
//            query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));
//
//            String real = getInterpretedQuery2(session, ObjectType.class, query);
//
//            LOGGER.info("real query>\n{}", new Object[]{real});
//        } finally {
//            close(session);
//        }
//    }

    @Test
    public void test0530queryObjectSubstringName() throws Exception {
        Session session = open();

        try {
            ObjectQuery objectQuery = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).startsWith("a").matchingOrig()
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, objectQuery);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.name.orig like :orig\n";
            assertEqualsIgnoreWhitespace(expected, real);

            OperationResult result = new OperationResult("test0540queryObjectClassTypeUser");
            int count = repositoryService.countObjects(ObjectType.class, objectQuery, null, result);
            assertEquals(3, count);

            objectQuery = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).containsPoly("a").matchingOrig()
                    .build();
            count = repositoryService.countObjects(ObjectType.class, objectQuery, null, result);
            assertEquals(23, count);

        } finally {
            close(session);
        }
    }

    @Test
    public void test0540queryObjectClassTypeUser() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.objectTypeClass = :objectTypeClass\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0550queryObjectClassTypeAbstractRole() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(AbstractRoleType.class)
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.objectTypeClass in (:objectTypeClass)";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0560queryMetadataTimestamp() throws Exception {
        Session session = open();

        try {
            XMLGregorianCalendar timeXml = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
            ObjectQuery query = prismContext.queryFor(ReportOutputType.class)
                    .item(ReportOutputType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP).le(timeXml)
                    .build();
            String real = getInterpretedQuery2(session, ReportOutputType.class, query);
            String expected = "select\n" +
                    "  r.oid, r.fullObject\n" +
                    "from\n" +
                    "  RReportOutput r\n" +
                    "where\n" +
                    "  r.createTimestamp <= :createTimestamp";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0570QueryObjectTypeByTypeUserAndLocality() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +                       // TODO - why not RUser here? we unnecessarily join all of RObject subtypes...
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      o.localityFocus.orig = :orig and\n" +
                    "      o.localityFocus.norm = :norm\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
            checkQueryTypeAlias(hqlToSql(real), "m_user", "localityFocus_orig", "localityFocus_norm");
        } finally {
            close(session);
        }
    }

    /**
     * This checks aliases, if they were generated correctly for query. Alias for table as "table" parameter
     * must be used for columns in "properties" parameter.
     */
    private void checkQueryTypeAlias(String query, String table, String... properties) {
        logger.info("SQL generated = {}", query);

        String[] array = query.split(" ");
        String alias = null;
        for (int i = 0; i < array.length; i++) {
            if (table.equals(array[i])) {
                alias = array[i + 1];
                break;
            }
        }
        AssertJUnit.assertNotNull(alias);

        for (String property : properties) {
            for (String token : array) {
                if (token.endsWith(property + "=?") && !token.startsWith(alias + ".")) {
                    fail("Property '" + property + "' doesn't have proper alias '"
                            + alias + "' in token '" + token + "'");
                }
            }
        }
    }

    @Test
    public void test0575QueryObjectypeByTypeOrgAndLocality() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(OrgType.class)
                    .item(OrgType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      o.localityFocus.orig = :orig and\n" +
                    "      o.localityFocus.norm = :norm\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
            checkQueryTypeAlias(hqlToSql(real), "m_org", "localityFocus_orig", "localityFocus_norm");
        } finally {
            close(session);
        }
    }

    @Test
    public void test0580QueryObjectypeByTypeAndExtensionAttribute() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_EXTENSION, new QName("http://example.com/p", "weapon")).eq("some weapon name")
                    .build();

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, ObjectType.class, query, false, null);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.strings s with ( s.ownerType = :ownerType and s.itemId = :itemId )\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    s.value = :value\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, realQuery.getQuery().getQueryString());
            assertEquals("Wrong property ID for 'weapon'", weaponDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());

        } finally {
            close(session);
        }
    }

    @Test
    public void test0590QueryObjectypeByTypeAndReference() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_LINK_REF).ref("123")
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.linkRef l\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      l.targetOid = :targetOid and\n" +
                    "      l.relation in (:relation)\n" +
                    "    )\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0600QueryObjectypeByTypeComplex() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .block()
                    .item(UserType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .or().item(UserType.F_LOCALITY).eqPoly("Adriatic", "adriatic")
                    .endBlock()
                    .or().type(OrgType.class)
                    .block()
                    .item(OrgType.F_ORG_TYPE).eq("functional")
                    .endBlock()
                    .or().type(ReportType.class)
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.orgType o2\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      o.objectTypeClass = :objectTypeClass and\n" +
                    "      (\n" +
                    "        (\n" +
                    "          o.localityFocus.orig = :orig and\n" +
                    "          o.localityFocus.norm = :norm\n" +
                    "        ) or\n" +
                    "        (\n" +
                    "          o.localityFocus.orig = :orig2 and\n" +
                    "          o.localityFocus.norm = :norm2\n" +
                    "        )\n" +
                    "      )\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      o.objectTypeClass = :objectTypeClass2 and\n" +
                    "      o2 = :o2\n" +
                    "    ) or\n" +
                    "    o.objectTypeClass = :objectTypeClass3\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0601QueryObjectypeByTwoAbstractTypes() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(FocusType.class).block().endBlock()
                    .or().type(AbstractRoleType.class).block().endBlock()
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n"
                    + "  o.oid, o.fullObject\n"
                    + "from\n"
                    + "  RObject o\n"
                    + "where\n"
                    + "  (\n"
                    + "    o.objectTypeClass in (:objectTypeClass) or\n"
                    + "    o.objectTypeClass in (:objectTypeClass2)\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0605QueryObjectTypeByTypeAndReference() throws Exception {
        Session session = open();
        try {
            PrismObjectDefinition<RoleType> roleDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .id("c0c010c0-d34d-b33f-f00d-111111111111")
                    .or().type(RoleType.class)
                    .item(roleDef, RoleType.F_ROLE_MEMBERSHIP_REF).ref("c0c010c0-d34d-b33f-f00d-111111111111")
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n"
                    + "  o.oid,\n"
                    + "  o.fullObject\n"
                    + "from\n"
                    + "  RObject o\n"
                    + "    left join o.roleMembershipRef r\n"
                    + "where\n"
                    + "  (\n"
                    + "    o.oid in :oid or\n"
                    + "    (\n"
                    + "      o.objectTypeClass = :objectTypeClass and\n"
                    + "      (\n"
                    + "        r.targetOid = :targetOid and\n"
                    + "        r.relation in (:relation)\n"
                    + "      )\n"
                    + "    )\n"
                    + "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0606QueryObjectTypeByTypeAndOwnerRefOverloaded() throws Exception {
        Session session = open();
        try {
            PrismObjectDefinition<AccessCertificationCampaignType> campaignDef =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccessCertificationCampaignType.class);
            PrismObjectDefinition<AccessCertificationDefinitionType> definitionDef =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccessCertificationDefinitionType.class);
            PrismObjectDefinition<TaskType> taskDef =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);

            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .id("c0c010c0-d34d-b33f-f00d-111111111111")
                    .or().type(AccessCertificationCampaignType.class).item(campaignDef, AccessCertificationCampaignType.F_OWNER_REF).ref("campaign-owner-oid")
                    .or().type(AccessCertificationDefinitionType.class).item(definitionDef, AccessCertificationDefinitionType.F_OWNER_REF).ref("definition-owner-oid")
                    .or().type(TaskType.class).item(taskDef, AccessCertificationDefinitionType.F_OWNER_REF).ref("task-owner-oid")
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n"
                    + "  o.oid,\n"
                    + "  o.fullObject\n"
                    + "from\n"
                    + "  RObject o\n"
                    + "where\n"
                    + "  (\n"
                    + "    o.oid in :oid or\n"
                    + "    (\n"
                    + "      o.objectTypeClass = :objectTypeClass and\n"
                    + "      (\n"
                    + "        o.ownerRefCampaign.targetOid = :targetOid and\n"
                    + "        o.ownerRefCampaign.relation in (:relation)\n"
                    + "      )\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      o.objectTypeClass = :objectTypeClass2 and\n"
                    + "      (\n"
                    + "        o.ownerRefDefinition.targetOid = :targetOid2 and\n"
                    + "        o.ownerRefDefinition.relation in (:relation2)\n"
                    + "      )\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      o.objectTypeClass = :objectTypeClass3 and\n"
                    + "      (\n"
                    + "        o.ownerRefTask.targetOid = :targetOid3 and\n"
                    + "        o.ownerRefTask.relation in (:relation3)\n"
                    + "      )\n"
                    + "    )\n"
                    + "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void test0610QueryGenericClob() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "locations")).isNull()
                    .build();
            getInterpretedQuery2(session, GenericObjectType.class, query);
        } catch (QueryException ex) {
            logger.info("Exception", ex);
            throw ex;
        } finally {
            close(session);
        }
    }

    @Test
    public void test0620QueryGenericString() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "stringType")).eq("asdf")
                    .build();
            String real = getInterpretedQuery2(session, GenericObjectType.class, query);
            String expected = "select\n" +
                    "  g.oid, g.fullObject\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.strings s with ( s.ownerType = :ownerType and s.itemId = :itemId )\n" +
                    "where\n" +
                    "  s.value = :value\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test(enabled = false)
//    public void atest100() throws Exception {
//        Session session = open();
//
//        try {
//            String expected = null;//HibernateToSqlTranslator.toSql(main);
//
//            List<EqualFilter> secondaryEquals = new ArrayList<>();
//            EqualFilter eq = EqualFilter.createEqual(ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_UID),
//                    new PrismPropertyDefinitionImpl(SchemaConstantsGenerated.ICF_S_UID, DOMUtil.XSD_STRING, prismContext),
//                    "8daaeeae-f0c7-41c9-b258-2a3351aa8876");
//            secondaryEquals.add(eq);
//            eq = EqualFilter.createEqual(ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_NAME),
//                    new PrismPropertyDefinitionImpl(SchemaConstantsGenerated.ICF_S_NAME, DOMUtil.XSD_STRING, prismContext),
//                    "some-name");
//            secondaryEquals.add(eq);
//
//            OrFilter secondaryIdentifierFilter = OrFilter.createOr((List) secondaryEquals);
//            RefFilter ref = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
//                    prismContext, "ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2");
//
//            AndFilter filter = AndFilter.createAnd(ref, secondaryIdentifierFilter);
//            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
//            LOGGER.debug("Query\n{}", query);
//
//            QueryEngine engine = new QueryEngine(repositoryService.getConfiguration(), prismContext);
//            RQuery rQuery = engine.interpret(query, ShadowType.class, null, false, session);
//            RQueryCriteriaImpl rci = (RQueryCriteriaImpl) rQuery;
//            System.out.println(rci);
//            System.out.println(rci.getCriteria());
//            //just test if DB will handle it or throws some exception
//            List l = rQuery.list();
//            LOGGER.info(">>>>>>>>asdfasdfasdfasdf{}",l.size());
//
//            String real = getInterpretedQuery2(session, ShadowType.class, query);
//
//            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
//            AssertJUnit.assertEquals(expected, real);
//        } finally {
//            close(session);
//        }
//    }

    @Test
    public void test0630QueryGenericBoolean() throws Exception {
        Session session = open();
        try {
            ObjectQuery objectQuery = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, SKIP_AUTOGENERATION_QNAME).eq(true)
                    .build();

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, GenericObjectType.class, objectQuery, false,
                    null);
            String expected = "select\n" +
                    "  g.oid, g.fullObject\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.booleans b with ( b.ownerType = :ownerType and b.itemId = :itemId )\n" +
                    "where\n" +
                    "  b.value = :value\n";

            assertEqualsIgnoreWhitespace(expected, realQuery.getQuery().getQueryString());
            assertEquals("Wrong property ID for 'skipAutogeneration'", skipAutogenerationDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());

            OperationResult result = new OperationResult("search");
            List<PrismObject<GenericObjectType>> objects = repositoryService.searchObjects(GenericObjectType.class,
                    objectQuery, null, result);
            result.computeStatus();
            assertTrue(result.isSuccess());

            AssertJUnit.assertNotNull(objects);
            assertEquals(1, objects.size());

            PrismObject<GenericObjectType> obj = objects.get(0);
            assertEquals(GenericObjectType.class, obj.getCompileTimeClass());

            result = new OperationResult("count");
            long count = repositoryService.countObjects(GenericObjectType.class, objectQuery, null, result);
            result.computeStatus();
            assertTrue(result.isSuccess());
            assertEquals(1, count);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0640queryAssignmentExtensionBoolean() throws Exception {
        Session session = open();
        try {
            PrismPropertyDefinition<?> propDef = prismContext.definitionFactory().createPropertyDefinition(
                    SKIP_AUTOGENERATION_QNAME, DOMUtil.XSD_BOOLEAN);

            ObjectQuery objectQuery = prismContext.queryFor(UserType.class)
                    .itemWithDef(propDef, F_ASSIGNMENT, AssignmentType.F_EXTENSION, SKIP_AUTOGENERATION_QNAME).eq(true)
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, objectQuery);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "    left join a.extension e\n" +
                    "    left join e.booleans b with b.itemId = :itemId\n" +
                    "where\n" +
                    "  b.value = :value";
            assertEqualsIgnoreWhitespace(expected, real);

            // include dependency on for this code org.hibernate.javax.persistence:hibernate-jpa-2.1-api:jar:1.0.0.Final:compile
//            CriteriaQuery<RAssignment> aQ = session.getCriteriaBuilder().createQuery(RAssignment.class);
//            aQ.select(aQ.from(RAssignment.class));
//            List<RAssignment> aList = session.createQuery(aQ).getResultList();
//            System.out.println("RAssignment: " + aList);
//
//            CriteriaQuery<RAssignmentExtension> aeQ = session.getCriteriaBuilder().createQuery(RAssignmentExtension.class);
//            aeQ.select(aeQ.from(RAssignmentExtension.class));
//            List<RAssignmentExtension> aeList = session.createQuery(aeQ).getResultList();
//            System.out.println("RAssignmentExtension: " + aeList);
//
//            CriteriaQuery<RAExtBoolean> aebQ = session.getCriteriaBuilder().createQuery(RAExtBoolean.class);
//            aebQ.select(aebQ.from(RAExtBoolean.class));
//            List<RAExtBoolean> aebList = session.createQuery(aebQ).getResultList();
//            System.out.println("RAExtBoolean: " + aebList);
//
            OperationResult result = new OperationResult("search");
            List<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class,
                    objectQuery, null, result);
            result.computeStatus();
            assertTrue(result.isSuccess());

            AssertJUnit.assertNotNull(objects);
            assertEquals(1, objects.size());

            PrismObject<UserType> obj = objects.get(0);
            assertEquals(UserType.class, obj.getCompileTimeClass());

            result = new OperationResult("count");
            long count = repositoryService.countObjects(UserType.class, objectQuery, null, result);
            result.computeStatus();
            assertTrue(result.isSuccess());
            assertEquals(1, count);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0650QueryExtensionEnum() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_EXTENSION, new QName("overrideActivation")).eq(ActivationStatusType.ENABLED)
                    .build();
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQuery2Whole(session, UserType.class, query, false, null);
            String real = realQuery.getQuery().getQueryString();
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.strings s with (\n"
                    + "          s.ownerType = :ownerType and\n"
                    + "          s.itemId = :itemId\n"
                    + ")\n"
                    + "where\n"
                    + "  s.value = :value\n";
            assertEqualsIgnoreWhitespace(expected, real);
            assertEquals("Wrong property ID for 'overrideActivation'", overrideActivationDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test0660QueryExtensionRef() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(F_EXTENSION, new QName("referenceType")).ref("123")
                    .build();
            String real = getInterpretedQuery2(session, GenericObjectType.class, query);
            String expected = "select\n"
                    + "  g.oid,\n"
                    + "  g.fullObject\n"
                    + "from\n"
                    + "  RGenericObject g\n"
                    + "    left join g.references r with (\n"
                    + "       r.ownerType = :ownerType and\n"
                    + "       r.itemId = :itemId\n"
                    + "    )\n"
                    + "where\n"
                    + "  (\n"
                    + "    r.value = :value and\n"
                    + "    r.relation in (:relation)\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0700QueryCertCaseAll() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, (ObjectQuery) null, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0705QueryCertWorkItemAll() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, (ObjectQuery) null, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0707QueryCertWorkItemAllOrderByCampaignName() throws Exception {
        Session session = open();
        try {
            ObjectQuery q = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .asc(PrismConstants.T_PARENT, PrismConstants.T_PARENT, F_NAME)
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, q, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n"
                    + "    left join a.owner o\n"
                    + "    left join o.owner o2\n"
                    + "order by o2.nameCopy.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0710QueryCertCaseOwner() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("123456")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "where\n" +
                    "  a.ownerOid in :ownerOid";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0715QueryWorkItemsForCase() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .exists(PrismConstants.T_PARENT)
                    .block()
                    .id(1)
                    .and().ownerId("123456")
                    .endBlock()
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, query, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n"
                    + "    left join a.owner o\n"
                    + "where\n"
                    + "  (\n"
                    + "    o.id in :id and\n"
                    + "    o.ownerOid in :ownerOid\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0717QueryWorkItemsForCampaign() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .exists(PrismConstants.T_PARENT)
                    .ownerId("campaignOid1")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, query, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n"
                    + "    left join a.owner o\n"
                    + "where\n"
                    + "    o.ownerOid in :ownerOid\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0720QueryCertCaseOwnerAndTarget() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("123456")
                    .and().item(AccessCertificationCaseType.F_TARGET_REF).ref("1234567890")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.ownerOid in :ownerOid and\n" +
                    "    (\n" +
                    "      a.targetRef.targetOid = :targetOid and\n" +
                    "      a.targetRef.relation in (:relation)\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0730QueryCertCaseReviewer() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref("1234567890")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n"
                    + "  a.ownerOid, a.id, a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase a\n"
                    + "    left join a.workItems w\n"
                    + "    left join w.assigneeRef a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    a2.targetOid = :targetOid and\n"
                    + "    a2.relation in (:relation)\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0735QueryCertWorkItemReviewers() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1")
                    .or().item(F_ASSIGNEE_REF).ref("oid2")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, query, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n"
                    + "    left join a.assigneeRef a2\n"
                    + "    left join a.assigneeRef a3\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      a2.targetOid = :targetOid and\n"
                    + "      a2.relation in (:relation)\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      a3.targetOid = :targetOid2 and\n"
                    + "      a3.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0737QueryCertWorkItemReviewersMulti() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1", "oid2")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, query, false);
            String expected = "select\n"
                    + "  a.ownerOwnerOid,\n"
                    + "  a.ownerId,\n"
                    + "  a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem a\n"
                    + "    left join a.assigneeRef a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    a2.targetOid in (:targetOid) and\n"
                    + "    a2.relation in (:relation)\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0740QueryCertCasesByCampaignOwner() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue ownerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .exists(T_PARENT)
                    .block()
                    .id(123456L)
                    .or().item(F_OWNER_REF).ref(ownerRef)
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.oid in :oid or\n" +
                    "    (\n" +
                    "      o.ownerRefCampaign.targetOid = :targetOid and\n" +
                    "      o.ownerRefCampaign.relation in (:relation) and\n" +
                    "      o.ownerRefCampaign.type = :type\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0735QueryCertCaseReviewerAndEnabled() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected =
                    "select\n" +
                            "  a.ownerOid, a.id, a.fullObject\n" +
                            "from\n" +
                            "  RAccessCertificationCase a\n" +
                            "    left join a.workItems w\n" +
                            "    left join w.assigneeRef a2\n" +
                            "    left join a.owner o\n" +
                            "where\n" +
                            "  (\n" +
                            "    (\n" +
                            "      a2.targetOid = :targetOid and\n" +
                            "      a2.relation in (:relation) and\n" +
                            "      a2.type = :type\n" +
                            "    ) and\n" +
                            "    (\n" +
                            "      a.stageNumber = o.stageNumber or\n" +
                            "      (\n" +
                            "        a.stageNumber is null and\n" +
                            "        o.stageNumber is null\n" +
                            "      )\n" +
                            "    )\n" +
                            "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0745QueryCertCaseReviewerAndEnabledByDeadlineAndOidAsc() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();

            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .asc(F_CURRENT_STAGE_DEADLINE).asc(T_ID)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected =
                    "select\n" +
                            "  a.ownerOid, a.id, a.fullObject\n" +
                            "from\n" +
                            "  RAccessCertificationCase a\n" +
                            "    left join a.workItems w\n" +
                            "    left join w.assigneeRef a2\n" +
                            "    left join a.owner o\n" +
                            "where\n" +
                            "  (\n" +
                            "    (\n" +
                            "      a2.targetOid = :targetOid and\n" +
                            "      a2.relation in (:relation) and\n" +
                            "      a2.type = :type\n" +
                            "    ) and\n" +
                            "    (\n" +
                            "      a.stageNumber = o.stageNumber or\n" +
                            "      (\n" +
                            "        a.stageNumber is null and\n" +
                            "        o.stageNumber is null\n" +
                            "      )\n" +
                            "    )\n" +
                            "  )\n" +
                            "order by a.reviewDeadline asc, a.id asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0746QueryCertWorkItemReviewerAndEnabledByDeadlineAndOidAsc() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1", "oid2")
                    .and().item(F_CLOSE_TIMESTAMP).isNull()
                    .asc(PrismConstants.T_PARENT, F_CURRENT_STAGE_DEADLINE).asc(T_ID)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationWorkItemType.class, query, false);
            String expected =
                    "select\n"
                            + "  a.ownerOwnerOid,\n"
                            + "  a.ownerId,\n"
                            + "  a.id\n"
                            + "from\n"
                            + "  RAccessCertificationWorkItem a\n"
                            + "    left join a.assigneeRef a2\n"
                            + "    left join a.owner o\n"
                            + "where\n"
                            + "  (\n"
                            + "    (\n"
                            + "      a2.targetOid in (:targetOid) and\n"
                            + "      a2.relation in (:relation)\n"
                            + "    ) and\n"
                            + "    a.closeTimestamp is null\n"
                            + "  )\n"
                            + "order by o.reviewDeadline asc, a.id asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0747QueryCertCaseReviewerAndEnabledByRequestedDesc() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .and().item(T_PARENT, F_STATE).eq(IN_REVIEW_STAGE)
                    .desc(F_CURRENT_STAGE_CREATE_TIMESTAMP)
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);

            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.workItems w\n" +
                    "    left join w.assigneeRef a2\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      a2.targetOid = :targetOid and\n" +
                    "      a2.relation in (:relation) and\n" +
                    "      a2.type = :type\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      a.stageNumber = o.stageNumber or\n" +
                    "      (\n" +
                    "        a.stageNumber is null and\n" +
                    "        o.stageNumber is null\n" +
                    "      )\n" +
                    "    ) and\n" +
                    "    o.state = :state\n" +
                    "  )\n" +
                    "order by a.reviewRequestedTimestamp desc";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0750DereferenceLink() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: linkRef/@/name contains 'test.com'
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME).containsPoly("test.com")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.linkRef l\n" +
                    "    left join l.target t\n" +
                    "where\n" +
                    "  (\n" +
                    "    t.nameCopy.orig like :orig and\n" +
                    "    t.nameCopy.norm like :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0752DereferenceLinkedResourceName() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: linkRef/@/resourceRef/@/name contains 'CSV' (norm)
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE,
                            ShadowType.F_RESOURCE_REF, PrismConstants.T_OBJECT_REFERENCE,
                            F_NAME).containsPoly("CSV").matchingNorm()
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.linkRef l\n" +
                    "    left join l.target t\n" +
                    "    left join t.resourceRef.target t2\n" +
                    "where\n" +
                    "  t2.name.norm like :norm\n";
            assertEqualsIgnoreWhitespace(expected, real);

        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)             // at this time
    public void test0760DereferenceAssignedRoleType() throws Exception {
        Session session = open();

        try {
            /*
             * This fails, as prism nor query interpreter expect that targetRef is RoleType/RRole.
             * Prism should implement something like "searching for proper root" when dereferencing "@".
             * QI should implement the proper root search not only at the query root, but always after a "@".
             *
             * ### UserType: assignment/targetRef/@/roleType
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, RoleType.F_ROLE_TYPE).eq("type1")
                    .build();
            getInterpretedQuery2(session, UserType.class, query);

        } finally {
            close(session);
        }
    }

    @Test
    public void test0770CaseParentFilter() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: Equal(../name, 'Campaign 1')
             */

            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(T_PARENT, F_NAME).eq("Campaign 1")
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.nameCopy.orig = :orig and\n" +
                    "    o.nameCopy.norm = :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0800OrderBySingleton() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: order by activation/administrativeStatus
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    null,
                    prismContext.queryFactory().createPaging(
                            ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                            ASCENDING));

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "order by u.activation.administrativeStatus asc";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0810OrderByParentCampaignName() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by ../name desc
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    prismContext.queryFactory().createPaging(ItemPath.create(T_PARENT, F_NAME), DESCENDING));

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.owner o\n" +
                    "order by o.nameCopy.orig desc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0820OrderByTargetName() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by targetRef/@/name
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    prismContext.queryFactory().createPaging(ItemPath.create(
                            AccessCertificationCaseType.F_TARGET_REF,
                            PrismConstants.T_OBJECT_REFERENCE, F_NAME), ASCENDING));

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.targetRef.target t\n" +
                    "order by t.name.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    // should fail, as Equals supports single-value right side only
    // TODO this should be perhaps checked in EqualFilter
    public void test0900EqualsMultivalue() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: preferredLanguage = 'SK', 'HU'
             */
            MutablePrismPropertyDefinition<String> multivalDef = prismContext.definitionFactory().createPropertyDefinition(UserType.F_PREFERRED_LANGUAGE,
                    DOMUtil.XSD_STRING);
            multivalDef.setMaxOccurs(-1);
            PrismProperty<String> multivalProperty = multivalDef.instantiate();
            multivalProperty.addRealValue("SK");
            multivalProperty.addRealValue("HU");

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_PREFERRED_LANGUAGE).eq(multivalProperty)
                    .build();

            getInterpretedQuery2(session, UserType.class, query);
//            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0910PreferredLanguageEqualsCostCenter() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: preferredLanguage = costCenter
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_PREFERRED_LANGUAGE).eq().item(UserType.F_COST_CENTER)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.preferredLanguage = u.costCenter or\n" +
                    "    (\n" +
                    "      u.preferredLanguage is null and\n" +
                    "      u.costCenter is null\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(enabled = false)
    // different types of properties that are to be compared (polystring vs string in this case) are not supported yet
    public void test0915OrganizationEqualsCostCenter() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: organization = costCenter
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eq(UserType.F_COST_CENTER)
                    .build();
            getInterpretedQuery2(session, UserType.class, query);
//            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0920DecisionsNotAnswered() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccCertCase: Exists (decision: assigneeRef = XYZ and stage = ../stage and response is null or response = NO_RESPONSE)
             */
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .exists(F_WORK_ITEM)
                    .block()
                    .item(F_ASSIGNEE_REF).ref("123456")
                    .and().item(F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCaseType.F_STAGE_NUMBER)
                    .and().item(F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME).isNull()
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n"
                    + "  a.ownerOid, a.id, a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase a\n"
                    + "    left join a.workItems w\n"
                    + "    left join w.assigneeRef a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      a2.targetOid = :targetOid and\n"
                    + "      a2.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      w.stageNumber = a.stageNumber or\n"
                    + "      (\n"
                    + "        w.stageNumber is null and\n"
                    + "        a.stageNumber is null\n"
                    + "      )\n"
                    + "    ) and\n"
                    + "      w.outcome is null\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0925DecisionsNotAnsweredOrderBy() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .exists(F_WORK_ITEM)
                    .block()
                    .item(F_ASSIGNEE_REF).ref("123456")
                    .and().item(F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCaseType.F_STAGE_NUMBER)
                    .and().item(F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME).isNull()
                    .endBlock()
                    .asc(T_PARENT, F_NAME)
                    .asc(T_ID)
                    .asc(T_PARENT, T_ID)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n"
                    + "  a.ownerOid, a.id, a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase a\n"
                    + "    left join a.workItems w\n"
                    + "    left join w.assigneeRef a2\n"
                    + "    left join a.owner o\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      a2.targetOid = :targetOid and\n"
                    + "      a2.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      w.stageNumber = a.stageNumber or\n"
                    + "      (\n"
                    + "        w.stageNumber is null and\n"
                    + "        a.stageNumber is null\n"
                    + "      )\n"
                    + "    ) and\n"
                    + "      w.outcome is null\n"
                    + "  )\n"
                    + "order by o.nameCopy.orig asc, a.id asc, a.ownerOid asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0930ResourceRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, F_CONSTRUCTION, F_RESOURCE_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.resourceRef.targetOid = :targetOid and\n" +
                    "    a.resourceRef.relation in (:relation)\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0932CreatorRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_METADATA, F_CREATOR_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.creatorRef.targetOid = :targetOid and\n" +
                    "    u.creatorRef.relation in (:relation)\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0934CreateApproverRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_METADATA, F_CREATE_APPROVER_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.createApproverRef c\n" +
                    "where\n" +
                    "  (\n" +
                    "    c.targetOid = :targetOid and\n" +
                    "    c.relation in (:relation)\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0940FullTextSimple() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("Peter")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.textInfoItems t\n"
                    + "where\n"
                    + "  t.text like :text";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    // adapt the test after query interpreter is optimized (when searching for empty text)
    @Test
    public void test0941FullTextEmpty() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("\t\t\t")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.textInfoItems t\n"
                    + "where\n"
                    + "  t.text like :text";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0945FullTextMulti() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("\t\nPeter\t\tMravec\t")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.textInfoItems t\n"
                    + "    left join u.textInfoItems t2\n"
                    + "where\n"
                    + "  (\n"
                    + "    t.text like :text and\n"
                    + "    t2.text like :text2\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0950RedundantBlock() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .item(ShadowType.F_NAME).eqPoly("aaa")
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "where\n"
                    + "  (\n"
                    + "    s.nameCopy.orig = :orig and\n"
                    + "    s.nameCopy.norm = :norm\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0952TwoRedundantBlocks() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .block()
                    .item(ShadowType.F_NAME).eqPoly("aaa")
                    .endBlock()
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "where\n"
                    + "  (\n"
                    + "    s.nameCopy.orig = :orig and\n"
                    + "    s.nameCopy.norm = :norm\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test0954RedundantBlocksAndExists() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .item(ShadowType.F_RESOURCE_REF).ref("111")
                    .and()
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      s.resourceRef.targetOid = :targetOid and\n"
                    + "      s.resourceRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    s.pendingOperationCount > :pendingOperationCount\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test
//    public void test1100ProcessStartTimestamp() throws Exception {
//        Session session = open();
//
//        try {
//            ObjectQuery query = prismContext.queryFor(TaskType.class)
//                    .item(F_WORKFLOW_CONTEXT, F_REQUESTOR_REF).ref("123456")
//                    .and().not().item(F_WORKFLOW_CONTEXT, F_CASE_OID).isNull()
//                    .desc(F_WORKFLOW_CONTEXT, F_START_TIMESTAMP)
//                    .build();
//            String real = getInterpretedQuery2(session, TaskType.class, query);
//            String expected = "select\n"
//                    + "  t.oid, t.fullObject\n"
//                    + "from\n"
//                    + "  RTask t\n"
//                    + "where\n"
//                    + "  (\n"
//                    + "    (\n"
//                    + "      t.wfRequesterRef.targetOid = :targetOid and\n"
//                    + "      t.wfRequesterRef.relation in (:relation)\n"
//                    + "    ) and\n"
//                    + "    not t.wfProcessInstanceId is null\n"
//                    + "  )\n"
//                    + "order by t.wfStartTimestamp desc";
//            assertEqualsIgnoreWhitespace(expected, real);
//        } finally {
//            close(session);
//        }
//
//    }

    @Test
    public void test1110AvailabilityStatus() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ResourceType.class)
                    .item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS).eq(AvailabilityStatusType.UP)
                    .build();
            String real = getInterpretedQuery2(session, ResourceType.class, query);
            String expected = "select\n"
                    + "  r.oid, r.fullObject\n"
                    + "from\n"
                    + "  RResource r\n"
                    + "where\n"
                    + "  r.operationalState.lastAvailabilityStatus = :lastAvailabilityStatus\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }

    }

    @Test
    public void test1120NullRefSingle() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ResourceType.class)
                    .item(ResourceType.F_CONNECTOR_REF).isNull()
                    .build();
            String real = getInterpretedQuery2(session, ResourceType.class, query);
            String expected = "select\n"
                    + "  r.oid, r.fullObject\n"
                    + "from\n"
                    + "  RResource r\n"
                    + "where\n"
                    + "  r.connectorRef is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test   // the same as test0142QueryUserAccountRefNull, but keeping because of test structure
    public void test1130NullRefMulti() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.linkRef l\n"
                    + "where\n"
                    + "  l is null\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1140NullEqSingle() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_EMPLOYEE_NUMBER).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid, u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  u.employeeNumber is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1150NullEqMulti() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_EMPLOYEE_TYPE).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "    left join u.employeeType e\n"
                    + "where\n"
                    + "  e is null";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1160AbstractRoleParameters() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(RoleType.F_RISK_LEVEL).eq("critical")
                    .and().item(RoleType.F_IDENTIFIER).eq("001")
                    .and().item(RoleType.F_DISPLAY_NAME).eqPoly("aaa", "aaa").matchingNorm()
                    .build();
            String real = getInterpretedQuery2(session, RoleType.class, query);
            String expected = "select\n"
                    + "  r.oid, r.fullObject\n"
                    + "from\n"
                    + "  RRole r\n"
                    + "where\n"
                    + "  (\n"
                    + "    r.riskLevel = :riskLevel and\n"
                    + "    r.identifier = :identifier and\n"
                    + "    r.displayName.norm = :norm\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1170ExistsShadowPendingOperation() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .build();
            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "where\n"
                    + "  s.pendingOperationCount > :pendingOperationCount";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1180OperationFatalError() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .build();
            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "    left join s.operationExecutions o\n"
                    + "where\n"
                    + "  o.status = :status\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1182OperationFatalErrorTimestampSort() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .desc(F_OPERATION_EXECUTION, OperationExecutionType.F_TIMESTAMP)
                    .build();
            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "    left join s.operationExecutions o\n"
                    + "where\n"
                    + "  o.status = :status\n"
                    + "order by o.timestamp desc";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1190OperationSuccessForGivenTask() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_TASK_REF).ref("oid1")
                    .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.SUCCESS)
                    .endBlock()
                    .build();
            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "    left join s.operationExecutions o\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      o.taskRef.targetOid = :targetOid and\n"
                    + "      o.taskRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    o.status = :status\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1200OperationLastFailures() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                    .and().item(OperationExecutionType.F_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .endBlock()
                    .build();
            String real = getInterpretedQuery2(session, ShadowType.class, query);
            String expected = "select\n"
                    + "  s.oid,\n"
                    + "  s.fullObject\n"
                    + "from\n"
                    + "  RShadow s\n"
                    + "    left join s.operationExecutions o\n"
                    + "where\n"
                    + "  (\n"
                    + "    o.status = :status and\n"
                    + "    o.timestamp <= :timestamp\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1210PersonaRef() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(FocusType.class)
                    .item(FocusType.F_PERSONA_REF).ref("123456")
                    .build();
            String real = getInterpretedQuery2(session, FocusType.class, query);
            String expected = "select\n"
                    + "  f.oid,\n"
                    + "  f.fullObject\n"
                    + "from\n"
                    + "  RFocus f\n"
                    + "    left join f.personaRef p\n"
                    + "where\n"
                    + "  (\n"
                    + "    p.targetOid = :targetOid and\n"
                    + "    p.relation in (:relation)\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1220IgnorableDistinctAndOrderBy() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .asc(UserType.F_NAME)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query, false, distinct());
            String expected;
            expected = "select u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "order by u.nameCopy.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1230ApplicableDistinctAndOrderBy() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_TYPE).startsWith("e")
                .asc(UserType.F_NAME)
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false, distinct());
            String expected;
            SqlRepositoryConfiguration config = getConfiguration();
            if (config.isUsingOracle() || config.isUsingSQLServer()) {
                expected = "select\n"
                        + "  u.oid,\n"
                        + "  u.fullObject\n"
                        + "from\n"
                        + "  RUser u\n"
                        + "where\n"
                        + "  u.oid in ("
                        + "    select distinct\n"
                        + "      u.oid\n"
                        + "    from\n"
                        + "      RUser u left join u.employeeType e where e like :e)\n"
                        + "order by u.nameCopy.orig asc";
            } else {
                expected = "select distinct\n"
                        + "  u.oid,\n"
                        + "  u.fullObject,\n"
                        + "  u.nameCopy.orig\n"
                        + "from\n"
                        + "  RUser u left join u.employeeType e where e like :e\n"
                        + "order by u.nameCopy.orig asc\n";
            }
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }

        SearchResultList<PrismObject<UserType>> objects = repositoryService
                .searchObjects(UserType.class, query, null, new OperationResult("dummy"));
        System.out.println("objects: " + objects);
        // just to know if the execution was successful
    }

    @Test
    public void test1240DistinctUserWithAssignment() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("123456")
                    .asc(UserType.F_NAME)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query, false, distinct());
            String expected;
            SqlRepositoryConfiguration config = getConfiguration();
            if (config.isUsingOracle() || config.isUsingSQLServer()) {
                expected = "select\n"
                        + "  u.oid,\n"
                        + "  u.fullObject\n"
                        + "from\n"
                        + "  RUser u\n"
                        + "where\n"
                        + "  u.oid in (\n"
                        + "    select distinct\n"
                        + "      u.oid\n"
                        + "    from\n"
                        + "      RUser u\n"
                        + "        left join u.assignments a with a.assignmentOwner = :assignmentOwner\n"
                        + "    where\n"
                        + "      (\n"
                        + "        a.targetRef.targetOid = :targetOid and\n"
                        + "        a.targetRef.relation in (:relation)\n"
                        + "      ))\n"
                        + "order by u.nameCopy.orig asc";
            } else {
                expected = "select distinct\n"
                        + "  u.oid,\n"
                        + "  u.fullObject,\n"
                        + "  u.nameCopy.orig\n"
                        + "from\n"
                        + "  RUser u\n"
                        + "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n"
                        + "where\n"
                        + "  (\n"
                        + "    a.targetRef.targetOid = :targetOid and\n"
                        + "    a.targetRef.relation in (:relation)\n"
                        + "  )\n"
                        + "order by u.nameCopy.orig asc\n";
            }
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1250CampaignEndTimestamp() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                    .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                    .and().item(AccessCertificationCampaignType.F_END_TIMESTAMP).lt(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()))
                    .and().not().id(new String[0])
                    .maxSize(10)
                    .build();
            query.setFilter(ObjectQueryUtil.simplify(query.getFilter(), prismContext));           // necessary to remove "not oid()" clause
            String real = getInterpretedQuery2(session, AccessCertificationCampaignType.class, query);
            String expected = "select\n"
                    + "  a.oid,\n"
                    + "  a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCampaign a\n"
                    + "where\n"
                    + "  (\n"
                    + "    a.state = :state and\n"
                    + "    a.end < :end\n"
                    + "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1260CampaignEndTimestamp2() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                    .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                    .and().not().id("10-10-10-10-10")   // hoping there are not many of these
                    .desc(AccessCertificationCampaignType.F_END_TIMESTAMP)
                    .offset(100)
                    .maxSize(20)
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCampaignType.class, query);
            String expected = "select\n"
                    + "  a.oid,\n"
                    + "  a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCampaign a\n"
                    + "where\n"
                    + "  (\n"
                    + "    a.state = :state and\n"
                    + "    not a.oid in :oid\n"
                    + "  )\n"
                    + "order by a.end desc";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1270IgnorableDistinctWithCount() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query, true, distinct());
            String expected = "select count(u.oid) from RUser u";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1280ApplicableDistinctWithCount() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_EMPLOYEE_TYPE).startsWith("a")
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, query, true, distinct());
            String expected = "select count(distinct u.oid) from RUser u left join u.employeeType e where e like :e";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1300OidEqTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(PrismConstants.T_ID).eq("1")
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query, false);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.oid = :oid\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1310OwnerOidEqTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(PrismConstants.T_PARENT, PrismConstants.T_ID).eq("1")
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.ownerOid, a.id, a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "where\n" +
                    "  a.ownerOid = :ownerOid";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1320OidGeLtTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(PrismConstants.T_ID).ge("1")
                    .and().item(PrismConstants.T_ID).lt("2")
                    .build();

            String real = getInterpretedQuery2(session, ObjectType.class, query, false);
            String expected = "select\n" +
                    "  o.oid, o.fullObject\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  ( o.oid >= :oid and o.oid < :oid2 )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> distinct() {
        return createCollection(createDistinct());
    }

    private SqlRepositoryConfiguration getConfiguration() {
        return sqlRepositoryService.getConfiguration();
    }

    // TODO negative tests - order by entity, reference, any, collection
    // TODO implement checks for "order by" for non-singletons

    private <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, File file) throws
            Exception {
        return getInterpretedQuery2(session, type, file, false);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, File file,
            boolean interpretCount) throws Exception {
        ObjectQuery query = getQuery(file, type);
        return getInterpretedQuery2(session, type, query, interpretCount);
    }

    @Nullable
    private ObjectQuery getQuery(File file, Class<? extends Containerable> type)
            throws SchemaException, IOException {
        QueryType queryType = PrismTestUtil.parseAtomicValue(file, QueryType.COMPLEX_TYPE);

        logger.info("QUERY TYPE TO CONVERT : {}", ObjectQueryUtil.dump(queryType, prismContext));

        try {
            return getQueryConverter().createObjectQuery(type, queryType);
        } catch (Exception ex) {
            logger.info("error while converting query: " + ex.getMessage(), ex);
            return null;
        }
    }

    private QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }

    private <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, ObjectQuery query) throws Exception {
        return getInterpretedQuery2(session, type, query, false);
    }

    private <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, ObjectQuery query,
            boolean interpretCount) throws Exception {
        return getInterpretedQuery2(session, type, query, interpretCount, null);
    }

    private <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, ObjectQuery query,
            boolean interpretCount, Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
        RQuery rQuery = getInterpretedQuery2Whole(session, type, query, interpretCount, options);
        return ((RQueryImpl) rQuery).getQuery().getQueryString();
    }

    @NotNull
    private <T extends Containerable> RQuery getInterpretedQuery2Whole(Session session, Class<T> type, ObjectQuery query,
            boolean interpretCount, Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {
        if (query != null) {
            logger.info("QUERY TYPE TO CONVERT :\n{}", (query.getFilter() != null ? query.getFilter().debugDump(3) : null));
        }

        QueryEngine2 engine = new QueryEngine2(baseHelper.getConfiguration(), extItemDictionary, prismContext, relationRegistry);
        RQuery rQuery = engine.interpret(query, type, options, interpretCount, session);
        //just test if DB will handle it or throws some exception
        if (interpretCount) {
            rQuery.uniqueResult();
        } else {
            rQuery.list();
        }
        return rQuery;
    }

    private void assertEqualsIgnoreWhitespace(String expected, String real) {
        logger.info("exp. query>\n{}\nreal query>\n{}", expected, real);
        String expNorm = StringUtils.normalizeSpace(expected);
        String realNorm = StringUtils.normalizeSpace(real);
        if (!expNorm.equals(realNorm)) {
            String m = "Generated query is not correct. Expected:\n" + expected + "\nActual:\n" + real + "\n\nNormalized versions:\n\n" +
                    "Expected: " + expNorm + "\nActual:   " + realNorm + "\n";
            logger.error("{}", m);
            throw new AssertionError(m);
        }
    }

    @Test
    public void test1400QueryOrderByNameOrigLimit20() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class).asc(UserType.F_NAME).maxSize(20).build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "order by\n" +
                    "u.nameCopy.orig asc";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1410QueryOrderByNameOrigLimit20() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(RoleType.class).asc(RoleType.F_NAME).maxSize(20).build();

            String expected = "select\n" +
                    "  r.oid, r.fullObject\n" +
                    "from\n" +
                    "  RRole r\n" +
                    "order by\n" +
                    "r.nameCopy.orig asc";

            String real = getInterpretedQuery2(session, RoleType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1420QueryTasksForArchetypeRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(AssignmentHolderType.F_ARCHETYPE_REF).ref("oid1")
                    .build();

            String expected = "select\n"
                    + "  t.oid,\n"
                    + "  t.fullObject\n"
                    + "from\n"
                    + "  RTask t\n"
                    + "    left join t.archetypeRef a\n"
                    + "where\n"
                    + "  (\n"
                    + "    a.targetOid = :targetOid and\n"
                    + "    a.relation in (:relation)\n"
                    + "  )";

            String real = getInterpretedQuery2(session, TaskType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1430QuerySearchForFocusType() throws Exception {
        Session session = open();

        try {
            String expected = "select\n"
                    + "  f.oid,\n"
                    + "  f.fullObject\n"
                    + "from\n"
                    + "  RFocus f";

            String real = getInterpretedQuery2(session, FocusType.class, (ObjectQuery) null);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1432QuerySearchForAssignmentHolderType() throws Exception {
        Session session = open();

        try {
            String expected = "select\n"
                    + "  o.oid,\n"
                    + "  o.fullObject\n"
                    + "from\n"
                    + "  RObject o\n"
                    + "where\n"
                    + "  o.objectTypeClass in (:objectTypeClass)";

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQuery2Whole(session, AssignmentHolderType.class, null, false, null);
            String real = rQuery.getQuery().getQueryString();
            System.out.println("Query parameters:\n" + rQuery.getQuerySource().getParameters());

            assertEqualsIgnoreWhitespace(expected, real);

        } finally {
            close(session);
        }
    }

    @Test
    public void test1434QuerySearchForObjectType() throws Exception {
        Session session = open();

        try {
            String expected = "select\n"
                    + "  o.oid,\n"
                    + "  o.fullObject\n"
                    + "from\n"
                    + "  RObject o";

            String real = getInterpretedQuery2(session, ObjectType.class, (ObjectQuery) null);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1435QueryNameNormAsString() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eq("asdf").matchingNorm().build();

            String expected = "select\n" +
                    "  u.oid, u.fullObject\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.nameCopy.norm = :norm";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1440QueryWorkItemsByAssignee() throws Exception {
        Session session = open();
        try {
            ObjectQuery q = prismContext.queryFor(CaseWorkItemType.class)
                    .item(CaseWorkItemType.F_ASSIGNEE_REF).ref("123")
                    .build();
            String real = getInterpretedQuery2(session, CaseWorkItemType.class, q, false);
            String expected = "select\n"
                    + "  c.ownerOid,\n"
                    + "  c.id\n"
                    + "from\n"
                    + "  RCaseWorkItem c\n"
                    + "    left join c.assigneeRef a\n"
                    + "where\n"
                    + "  (\n"
                    + "    a.targetOid = :targetOid and\n"
                    + "    a.relation in (:relation)\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test1442QueryWorkItemsByCandidate() throws Exception {
        Session session = open();
        try {
            ObjectQuery q = prismContext.queryFor(CaseWorkItemType.class)
                    .item(CaseWorkItemType.F_CANDIDATE_REF).ref("123")
                    .build();
            String real = getInterpretedQuery2(session, CaseWorkItemType.class, q, false);
            String expected = "select\n"
                    + "  c.ownerOid,\n"
                    + "  c.id\n"
                    + "from\n"
                    + "  RCaseWorkItem c\n"
                    + "    left join c.candidateRef c2\n"
                    + "where\n"
                    + "  (\n"
                    + "    c2.targetOid = :targetOid and\n"
                    + "    c2.relation in (:relation)\n"
                    + "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    // MID-5515
    @Test
    public void test1443QueryNameNull() throws Exception {
        Session session = open();
        try {
            ObjectQuery q = prismContext.queryFor(UserType.class)
                    .item(F_NAME).isNull()
                    .build();
            String real = getInterpretedQuery2(session, UserType.class, q, false);
            String expected = "select\n"
                    + "  u.oid,\n"
                    + "  u.fullObject\n"
                    + "from\n"
                    + "  RUser u\n"
                    + "where\n"
                    + "  1=0\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    //@Test
    @UnusedTestElement
    public void test1443QueryWithMultiValueItemPathForOrdering() throws Exception {
        Session session = open();
        try {
            ObjectQuery q = prismContext.queryFor(ObjectType.class)
                    .exists(F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_TASK_REF).ref("00000000-0000-0000-0000-000000000006")
                    .and()
                    .block().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.PARTIAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.WARNING)
                    .endBlock()
                    .endBlock()
                    .build();
            List<ObjectOrdering> orderings = Collections.singletonList(
                    prismContext.queryFactory().createOrdering(
                            ItemPath.create("operationExecution", "timestamp"), OrderDirection.ASCENDING));
            ObjectPaging paging = prismContext.queryFactory().createPaging(1, 20, orderings);
            q.setPaging(paging);

            String real = getInterpretedQuery2(session, ObjectType.class, q, false);
            String expected = "";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }
}
