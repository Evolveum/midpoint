/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.*;
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
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserType.F_EMPLOYEE_NUMBER;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import jakarta.persistence.EntityManager;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.query.QueryEngine;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query.RQueryImpl;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * Tests repository query interpreter.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryInterpreterTest extends BaseSQLRepoTest {

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
    public void test001QueryNameNorm() throws Exception {
        EntityManager em = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringNorm)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingNorm().build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select"
                    + " _u.oid, _u.fullObject"
                    + " from RUser _u"
                    + " where _u.nameCopy.norm = :norm");
        } finally {
            close(em);
        }
    }

    @Test
    public void test002QueryNameOrig() throws Exception {
        EntityManager em = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingOrig().build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select"
                    + " _u.oid, _u.fullObject"
                    + " from RUser _u"
                    + " where _u.nameCopy.orig = :orig");
        } finally {
            close(em);
        }
    }

    @Test
    public void test003QueryNameStrict() throws Exception {
        EntityManager em = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("asdf", "asdf").build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  ( _u.nameCopy.orig = :orig and _u.nameCopy.norm = :norm )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test005QueryOrganizationNorm() throws Exception {
        EntityManager em = open();

        try {
            /*
             *  ### user: Equal (organization, "...", PolyStringNorm)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("guľôčka v jamôčke").matchingNorm().build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.organization _o\n"
                    + "where\n"
                    + "  _o.norm = :norm");

            assertEquals("Wrong parameter value", "gulocka v jamocke", rQuery.getQuerySource().getParameters().get("norm").getValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test006QueryOrganizationOrig() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingOrig().build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.organization _o\n"
                    + "where\n"
                    + "  _o.orig = :orig");
        } finally {
            close(em);
        }
    }

    @Test
    public void test007QueryOrganizationStrict() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf")
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingStrict().build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.organization _o\n"
                    + "where\n"
                    + "  ( _o.orig = :orig and _o.norm = :norm )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test010QueryTwoOrganizationsNormAnd() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  UserType: And (Equal (organization, 'asdf', PolyStringNorm),
             *                 Equal (organization, 'ghjk', PolyStringNorm))
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingNorm()
                    .and().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk").matchingNorm()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.organization _o\n"
                    + "    left join _u.organization _o2\n"
                    + "where\n"
                    + "  ( _o.norm = :norm and _o2.norm = :norm2 )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test011QueryTwoOrganizationsStrictOr() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  UserType: Or (Equal (organization, 'asdf'),
             *                Equal (organization, 'ghjk'))
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf")
                    .or().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);

            // NOTE: this could be implemented more efficiently by using only one join... or the query itself can be formulated
            // via In filter (when available) or Exists filter (also, when available)
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.organization _o\n"
                    + "    left join _u.organization _o2\n"
                    + "where\n"
                    + "  ( ( _o.orig = :orig and _o.norm = :norm ) or\n"
                    + "  ( _o2.orig = :orig2 and _o2.norm = :norm2 ) )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test012QueryOrganizationOrigPolymorphic() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### object: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingOrig()
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "    left join _o.organization _o2\n"
                    + "where\n"
                    + "  _o2.orig = :orig");
        } finally {
            close(em);
        }
    }

    @Test
    public void test013QueryTaskDependent() throws Exception {
        EntityManager em = open();

        try {
            /*
             *  ### task: Equal (dependent, "123456")
             */
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_DEPENDENT).eq("123456")
                    .build();

            String real = getInterpretedQuery(em, TaskType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _t.oid, _t.fullObject\n"
                    + "from\n"
                    + "  RTask _t\n"
                    + "    left join _t.dependent _d\n"
                    + "where\n"
                    + "  _d = :_d");
        } finally {
            close(em);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void test014QueryClob() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_DESCRIPTION).eq("aaa")
                    .build();

            //should throw exception, because description is lob and can't be queried
            getInterpretedQuery(em, UserType.class, query);
        } finally {
            close(em);
        }
    }

    @Test
    public void test015QueryEnum() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### task: Equal (executionStatus, WAITING)
             */
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.WAITING)
                    .build();
            String real = getInterpretedQuery(em, TaskType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _t.oid, _t.fullObject\n"
                    + "from\n"
                    + "  RTask _t\n"
                    + "where\n"
                    + "  _t.executionStatus = :executionStatus\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test016QueryEnabled() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### task: Equal (activation/administrativeStatus, ENABLED)
             *  ==> from RUser _u where _u.activation.administrativeStatus = com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus.ENABLED
             */

            String real = getInterpretedQuery(em, UserType.class,
                    new File(TEST_DIR, "query-user-by-enabled.xml"));

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.activation.administrativeStatus = :administrativeStatus\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test017QueryGenericLong() throws Exception {
        EntityManager em = open();
        try {
            /*
             *  ### generic: And (Equal (name, "generic object", PolyStringNorm),
             *                    Equal (c:extension/p:intType, 123))
             *  ==> from RGenericObject _g
             *        left join _g.longs l (l.ownerType = com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType.EXTENSION and l.name = 'http://example.com/p#intType')
             *      where
             *         g.name.norm = 'generic object' and
             *         l.value = 123
             */

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, GenericObjectType.class,
                    getQuery(new File(TEST_DIR, "query-and-generic.xml"), GenericObjectType.class), false, null);
            String real = getQueryString(realQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _g.oid, _g.fullObject\n"
                    + "from\n"
                    + "  RGenericObject _g\n"
                    + "    left join _g.longs _l with ( _l.ownerType = :ownerType and _l.itemId = :itemId )\n"
                    + "where\n"
                    + "  ( _g.nameCopy.norm = :norm and _l.value = :value )\n");

            assertEquals("Wrong property ID for 'intType'",
                    intTypeDefinition.getId(),
                    realQuery.getQuerySource().getParameters().get("itemId").getValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test018QueryGenericLongTwice() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(F_NAME).eqPoly("generic object", "generic object").matchingNorm()
                    .and().item(F_EXTENSION, new QName("intType")).ge(100)
                    .and().item(F_EXTENSION, new QName("intType")).lt(200)
                    .and().item(F_EXTENSION, new QName("longType")).eq(335)
                    .build();

            RQuery realQuery = getInterpretedQueryWhole(em, GenericObjectType.class, query, false, null);
            HibernateQuery source = ((RQueryImpl) realQuery).getQuerySource();
            String real = getQueryString((RQueryImpl) realQuery);

            // note l and l2 cannot be merged as they point to different extension properties (intType, longType)
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _g.oid, _g.fullObject\n"
                    + "from\n"
                    + "  RGenericObject _g\n"
                    + "    left join _g.longs _l with ( _l.ownerType = :ownerType and _l.itemId = :itemId )\n"
                    + "    left join _g.longs _l2 with ( _l2.ownerType = :ownerType2 and _l2.itemId = :itemId2 )\n"
                    + "where\n"
                    + "  (\n"
                    + "    _g.nameCopy.norm = :norm and\n"
                    + "    _l.value >= :value and\n"
                    + "    _l.value < :value2 and\n"
                    + "    _l2.value = :value3\n"
                    + "  )");

            assertEquals("Wrong property ID for 'intType'", intTypeDefinition.getId(), source.getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'longType'", longTypeDefinition.getId(), source.getParameters().get("itemId2").getValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test019QueryAccountByNonExistingAttribute() throws Exception {
        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-non-existing-attribute.xml"));
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid, _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.strings _s2 with ( _s2.ownerType = :ownerType and 1=0 )\n"
                    + "where\n"
                    + "  _s2.value = :value\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test030QueryAccountByAttribute() throws Exception {
        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attribute.xml"));

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid, _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.strings _s2 with ( _s2.ownerType = :ownerType and _s2.itemId = :itemId )\n"
                    + "where\n"
                    + "  _s2.value = :value\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test031QueryAccountByAttributeAndExtensionValue() throws Exception {
        EntityManager em = open();
        try {
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, ShadowType.class,
                    getQuery(new File(TEST_DIR, "query-account-by-attribute-and-extension-value.xml"), ShadowType.class), false,
                    null);

            assertThat(getQueryString(realQuery))
                    .isEqualToIgnoringWhitespace("select\n"
                            + "  _s.oid, _s.fullObject\n"
                            + "from\n"
                            + "  RShadow _s\n"
                            + "    left join _s.strings _s2 with ( _s2.ownerType = :ownerType and _s2.itemId = :itemId )\n"
                            + "    left join _s.longs _l with ( _l.ownerType = :ownerType2 and _l.itemId = :itemId2 )\n"
                            + "where\n"
                            + "  (\n"
                            + "    _s2.value = :value and\n"
                            + "    _l.value = :value2\n"
                            + "  )");

            assertEquals("Wrong property ID for 'a1'", a1Definition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'shoeSize'", shoeSizeDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId2").getValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test033QueryOrComposite() throws Exception {
        EntityManager em = open();
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
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, ShadowType.class,
                    getQuery(new File(TEST_DIR, "query-or-composite.xml"), ShadowType.class), false, null);

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
            assertThat(getQueryString(realQuery))
                    .isEqualToIgnoringWhitespace("select\n" +
                            "  _s.oid, _s.fullObject\n" +
                            "from\n" +
                            "  RShadow _s\n" +
                            "    left join _s.strings _s2 with ( _s2.ownerType = :ownerType and _s2.itemId = :itemId )\n" +
                            "    left join _s.strings _s3 with ( _s3.ownerType = :ownerType2 and _s3.itemId = :itemId2 )\n" +
                            "where\n" +
                            "  (\n" +
                            "    _s.intent = :intent or\n" +
                            "    _s2.value = :value or\n" +
                            "    _s3.value = :value2 or\n" +
                            "    (\n" +
                            "      _s.resourceRef.targetOid = :targetOid and\n" +
                            "      _s.resourceRef.relation in (:relation)\n" +
                            "    )\n" +
                            "  )\n");
            assertEquals("Wrong property ID for 'foo'", fooDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
            assertEquals("Wrong property ID for 'stringType'", stringTypeDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId2").getValue());

            System.out.println("Query parameters: " + realQuery.getQuerySource().getParameters());
        } finally {
            close(em);
        }
    }

    @Test
    public void test040QueryExistsAssignment() throws Exception {
        EntityManager em = open();

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

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  _a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by _u.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test041QueryExistsAssignmentWithRedundantBlock() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .block()
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                    .eq(ActivationStatusType.ENABLED)
                    .endBlock()
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  _a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by _u.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test042QueryExistsAssignmentWithRedundantBlock2() throws Exception {
        EntityManager em = open();

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

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  _a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by _u.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test044QueryExistsWithAnd() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref("111")
                    .and()
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .build();

            String real = getInterpretedQuery(em, ShadowType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _s.resourceRef.targetOid = :targetOid and\n"
                    + "      _s.resourceRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    _s.pendingOperationCount > :pendingOperationCount\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void test049QueryExistsAssignmentAll() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .exists(F_ASSIGNMENT).all()
                    .asc(F_NAME)
                    .build();

            query.setFilter(ObjectQueryUtil.simplify(query.getFilter()));

            String real = getInterpretedQuery(em, UserType.class, query);
            // this doesn't work as expected ... maybe inner join would be better! Until implemented, we should throw UOO
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "order by _u.name.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test050QuerySingleAssignmentWithTargetAndTenant() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_TARGET_REF).ref("target-oid-123")
                    .and().item(AssignmentType.F_TENANT_REF).ref("tenant-oid-456")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a.targetRef.targetOid = :targetOid and\n"
                    + "      _a.targetRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      _u.tenantRef.targetOid = :targetOid2 and\n"
                    + "      _u.tenantRef.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test052QueryAssignmentsWithTargetAndTenant() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("target-oid-123")
                    .and().item(UserType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF).ref("tenant-oid-456")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                    + "    left join _u.assignments _a2 with _a2.assignmentOwner = :assignmentOwner2\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a.targetRef.targetOid = :targetOid and\n"
                    + "      _a.targetRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      _a2.tenantRef.targetOid = :targetOid2 and\n"
                    + "      _a2.tenantRef.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test060QueryObjectByName() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### object: Equal (name, "cpt. Jack Sparrow")
             *             Order by name, ASC
             *
             * ==> from RObject _o where name.orig = 'cpt. Jack Sparrow' and name.norm = 'cpt jack sparrow'
             *        order by name.orig asc
             */
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    _o.name.orig = :orig and\n" +
                    "    _o.name.norm = :norm\n" +
                    "  )\n" +
                    "order by _o.name.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test061QueryUserByFullName() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, UserType.class,
                    new File(TEST_DIR, "query-user-by-fullName.xml"));
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  _u.fullName.norm = :norm\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test062QueryUserSubstringFullName() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, UserType.class,
                    new File(TEST_DIR, "query-user-substring-fullName.xml"));
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  lower(_u.fullName.norm) like :norm escape '!'");
        } finally {
            close(em);
        }
    }

    @Test
    public void test064QueryUserByName() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, UserType.class,
                    new File(TEST_DIR, "query-user-by-name.xml"));
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  _u.nameCopy.norm = :norm");
        } finally {
            close(em);
        }
    }

    // TODO: This was UserType.F_EMPLOYEE_TYPE, changed to subtype, but this is probably wrong
    @Test
    public void test066QuerySubstringMultivalued() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_SUBTYPE).contains("abc")
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "    left join _o.subtype _s\n" +
                    "where\n" +
                    "  _s like :_s escape '!'\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test070QueryConnectorByType() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, ConnectorType.class,
                    new File(TEST_DIR, "query-connector-by-type.xml"));
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _c.oid, _c.fullObject\n" +
                    "from\n" +
                    "  RConnector _c\n" +
                    "where\n" +
                    "  _c.connectorType = :connectorType\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test071QueryAccountByAttributesAndResourceRef() throws Exception {
        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attributes-and-resource-ref.xml"));

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _s.oid, _s.fullObject\n" +
                    "from\n" +
                    "  RShadow _s\n" +
                    "    left join _s.strings _s2 with ( _s2.ownerType = :ownerType and _s2.itemId = :itemId )\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _s.resourceRef.targetOid = :targetOid and\n" +
                    "      _s.resourceRef.relation in (:relation)\n" +
                    "    ) and\n" +
                    "    _s2.value = :value\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test073QueryUserAccountRef() throws Exception {
        EntityManager em = open();
        try {
            /*
             * ### user: Ref (linkRef, 123)
             *
             * ==> select from RUser _u left join _u.linkRef _l where
             *        _l.targetOid = '123' and _l.relation = '#'
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).ref("123")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.linkRef _l\n" +
                    "where\n" +
                    "  (\n" +
                    "    _l.targetOid = :targetOid and\n" +
                    "    _l.relation in (:relation)\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test074QueryUserAccountRefNull() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  _l is null");
        } finally {
            close(em);
        }
    }

    @Test
    public void test075QueryUserAccountRefNotNull() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .not().item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  not _l is null");
        } finally {
            close(em);
        }
    }

    @Test
    public void test076QueryUserAccountRefByType() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).refType(ShadowType.COMPLEX_TYPE)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  (\n"
                    + "    _l.relation in (:relation) and\n"
                    + "    _l.targetType = :targetType\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test077QueryUserAccountRefByRelation() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).refRelation(prismContext.getDefaultRelation())
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  _l.relation in (:relation)\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test078QueryUserAccountRefComplex() throws Exception {
        EntityManager em = open();
        try {
            PrismReferenceValue value1 = prismContext.itemFactory().createReferenceValue(null, ShadowType.COMPLEX_TYPE);
            PrismReferenceValue value2 = prismContext.itemFactory().createReferenceValue("abcdef", ShadowType.COMPLEX_TYPE);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).ref(value1, value2)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _l.relation in (:relation) and\n"
                    + "      _l.targetType = :targetType\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      _l.targetOid = :targetOid and\n"
                    + "      _l.relation in (:relation2) and\n"
                    + "      _l.targetType = :targetType2\n"
                    + "    )\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test080QueryUserAssignmentTargetRef() throws Exception {
        EntityManager em = open();
        try {
            /*
             * ### user: Ref (assignment/targetRef, '123', RoleType)
             *
             * ==> select from RUser _u left join _u.assignments a where
             *        a.assignmentOwner = RAssignmentOwner.FOCUS and
             *        _a.targetRef.targetOid = '123' and
             *        _a.targetRef.relation = '#' and
             *        _a.targetRef.type = RObjectType.ROLE
             */
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid("123");
            ort.setType(RoleType.COMPLEX_TYPE);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.targetRef.targetOid = :targetOid and\n" +
                    "    _a.targetRef.relation in (:relation) and\n" +
                    "    _a.targetRef.targetType = :targetType\n" +
                    "  )\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));
        } finally {
            close(em);
        }
    }

    @NotNull
    private List<String> getVariantsOfDefaultRelation() {
        return Arrays.asList("#", RUtil.qnameToString(unqualify(SchemaConstants.ORG_DEFAULT)), RUtil.qnameToString(SchemaConstants.ORG_DEFAULT));
    }

    @Test
    public void test082QueryUserAssignmentTargetRefManagerStandardQualified() throws Exception {
        EntityManager em = open();
        try {
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(SchemaConstants.ORG_MANAGER);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.targetRef.targetOid = :targetOid and\n" +
                    "    _a.targetRef.relation in (:relation) and\n" +
                    "    _a.targetRef.targetType = :targetType\n" +
                    "  )\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            System.out.println("relationParameter: " + relationParameter);
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));
        } finally {
            close(em);
        }
    }

    @Test
    public void test083QueryUserAssignmentTargetRefManagerCustomQualified() throws Exception {
        EntityManager em = open();
        try {
            QName auditorRelation = new QName("http://x/", "auditor");
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(auditorRelation);
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.targetRef.targetOid = :targetOid and\n" +
                    "    _a.targetRef.relation = :relation and\n" +
                    "    _a.targetRef.targetType = :targetType\n" +
                    "  )\n");

            String relationParameter = (String) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value", RUtil.qnameToString(auditorRelation), relationParameter);
        } finally {
            close(em);
        }
    }

    @Test
    public void test084QueryUserAssignmentTargetRefManagerUnqualified() throws Exception {
        EntityManager em = open();
        try {
            ObjectReferenceType ort = new ObjectReferenceType()
                    .oid("123")
                    .type(OrgType.COMPLEX_TYPE)
                    .relation(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER));
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(ort.asReferenceValue())
                    .build();
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.targetRef.targetOid = :targetOid and\n" +
                    "    _a.targetRef.relation in (:relation) and\n" +
                    "    _a.targetRef.targetType = :targetType\n" +
                    "  )\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));
        } finally {
            close(em);
        }
    }

    @Test
    public void test086QueryTrigger() throws Exception {
        final Date now = new Date();

        EntityManager em = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(now.getTime());
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(ObjectType.F_TRIGGER, F_TIMESTAMP).le(thisScanTimestamp)
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "    left join _o.trigger _t\n" +
                    "where\n" +
                    "  _t.timestamp <= :timestamp\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test088QueryTriggerBeforeAfter() throws Exception {
        final Date now = new Date();

        EntityManager em = open();
        try {
            XMLGregorianCalendar lastScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(now.getTime());
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(now.getTime());

            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(ObjectType.F_TRIGGER)
                    .block()
                    .item(F_TIMESTAMP).gt(lastScanTimestamp)
                    .and().item(F_TIMESTAMP).le(thisScanTimestamp)
                    .endBlock()
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "    left join _o.trigger _t\n" +
                    "where\n" +
                    "  ( _t.timestamp > :timestamp and _t.timestamp <= :timestamp2 )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test089QueryAssignmentActivationAdministrativeStatus() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  _a.activation.administrativeStatus = :administrativeStatus\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test090QueryInducementActivationAdministrativeStatus() throws Exception {
        EntityManager em = open();
        try {
            /*
             * ### role: Equal (inducement/activation/administrativeStatus, ENABLED)
             *
             * ==> select from RRole _r left join _r.assignments a where
             *          a.assignmentOwner = RAssignmentOwner.ABSTRACT_ROLE and                  <--- this differentiates inducements from assignments
             *          _a.activation.administrativeStatus = RActivationStatus.ENABLED
             */
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();

            String real = getInterpretedQuery(em, RoleType.class, query);

            // assignmentOwner = com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner.ABSTRACT_ROLE
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _r.oid, _r.fullObject\n" +
                    "from\n" +
                    "  RRole _r\n" +
                    "    left join _r.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  _a.activation.administrativeStatus = :administrativeStatus\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test092QueryInducementAndAssignmentActivationAdministrativeStatus() throws Exception {
        EntityManager em = open();
        try {
            /*
             * ### Role: Or (Equal (assignment/activation/administrativeStatus, RActivationStatus.ENABLED),
             *               Equal (inducement/activation/administrativeStatus, RActivationStatus.ENABLED))
             */
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .or().item(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .build();
            String real = getInterpretedQuery(em, RoleType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _r.oid, _r.fullObject\n" +
                    "from\n" +
                    "  RRole _r\n" +
                    "    left join _r.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "    left join _r.assignments _a2 with _a2.assignmentOwner = :assignmentOwner2\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.activation.administrativeStatus = :administrativeStatus or\n" +
                    "    _a2.activation.administrativeStatus = :administrativeStatus2\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test094QueryUserByActivationDouble() throws Exception {
        Date now = new Date();

        EntityManager em = open();
        try {
            /*
             * ### user: And (Equal (activation/administrativeStatus, RActivationStatus.ENABLED),
             *                Equal (activation/validFrom, '...'))
             *
             * ==> select u from RUser _u where _u.activation.administrativeStatus = RActivationStatus.ENABLED and
             *                                 _u.activation.validFrom = ...
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ENABLED)
                    .and().item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).eq(XmlTypeConverter.createXMLGregorianCalendar(now.getTime()))
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.activation.administrativeStatus = :administrativeStatus and\n" +
                    "    _u.activation.validFrom = :validFrom\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test096QueryTriggerTimestampDoubleWrong() throws Exception {
        final Date now = new Date();

        EntityManager em = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(now.getTime());

            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(ObjectType.F_TRIGGER, F_TIMESTAMP).gt(thisScanTimestamp)
                    .and().item(ObjectType.F_TRIGGER, F_TIMESTAMP).lt(thisScanTimestamp)
                    .build();
            logger.info(query.debugDump());
            String real = getInterpretedQuery(em, ObjectType.class, query);

            // correct translation but the filter is wrong: we need to point to THE SAME timestamp -> i.e. ForValue should be used here

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "    left join _o.trigger _t\n" +
                    "    left join _o.trigger _t2\n" +
                    "where\n" +
                    "  (\n" +
                    "    _t.timestamp > :timestamp and\n" +
                    "    _t2.timestamp < :timestamp2\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test100CountObjectOrderByName() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .asc(F_NAME).build();

            String real = getInterpretedQuery(em, UserType.class, query, true);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  count(_u.oid)\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.nameCopy.orig = :orig and\n" +
                    "    _u.nameCopy.norm = :norm\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test102CountObjectOrderByNameWithoutFilter() throws Exception {
        EntityManager em = open();

        try {
            ObjectPaging paging = prismContext.queryFactory().createPaging(null, null, F_NAME, ASCENDING);
            ObjectQuery query = prismContext.queryFactory().createQuery(null, paging);

            String real = getInterpretedQuery(em, ObjectType.class, query, true);

            // ordering does not make sense here
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  count(_o.oid)\n" +
                    "from\n" +
                    "  RObject _o\n");
        } finally {
            close(em);
        }
    }

    /**
     * Q{AND: (EQUALS: parent, PPV(null)),PAGING: O: 0,M: 5,BY: name, D:ASCENDING,
     */
    @Test
    public void test104CountTaskOrderByName() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_PARENT).isNull()
                    .asc(F_NAME)
                    .build();
            String real = getInterpretedQuery(em, TaskType.class, query, true);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  count(_t.oid)\n" +
                    "from\n" +
                    "  RTask _t\n" +
                    "where\n" +
                    "  _t.parent is null");
        } finally {
            close(em);
        }
    }

    @Test
    public void test106InOidTest() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .id("1", "2").build();

            String real = getInterpretedQuery(em, ObjectType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  _o.oid in (:oid)\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test108InOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .id(new String[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter()));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test110OwnerInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .ownerId(new String[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter()));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test112LongInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .id(new long[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter()));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test114LongOwnerInOidEmptyTest() {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .ownerId(new long[0]).build();
        query.setFilter(ObjectQueryUtil.simplify(query.getFilter()));
        assertTrue("Wrongly reduced InOid filter: " + query.getFilter(), query.getFilter() instanceof NoneFilter);
    }

    @Test
    public void test116OwnerInOidTest() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("1", "2").build();

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "where\n" +
                    "  _a.ownerOid in (:ownerOid)");
        } finally {
            close(em);
        }
    }

    @Test
    public void test118QueryOrgTreeFindOrgs() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isDirectChildOf("some oid")
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery(em, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  ROrg _o\n" +
                    "where\n" +
                    "  _o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.targetOid = :orgOid)\n" +
                    "order by _o.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test120QueryOrgTreeFindUsersRelationDefault() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isDirectChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_DEFAULT))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid = :orgOid)\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));

        } finally {
            close(em);
        }
    }

    @Test
    public void test122QueryOrgTreeFindUsersRelationManager() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isDirectChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_MANAGER))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid = :orgOid)\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));

        } finally {
            close(em);
        }
    }

    @Test
    public void test124QueryOrgAllLevels() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isChildOf(itemFactory().createReferenceValue("123"))
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery(em, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  ROrg _o\n" +
                    "where\n" +
                    "  _o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "order by _o.nameCopy.orig asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test126QueryOrgTreeFindUsersRelationDefault() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_DEFAULT))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(getVariantsOfDefaultRelation()),
                    new HashSet<>(relationParameter));

        } finally {
            close(em);
        }
    }

    private String getQueryString(RQueryImpl rQuery) {
        return rQuery.getQuery().unwrap(Query.class).getQueryString();
    }

    @Test
    public void test128QueryOrgTreeFindUsersRelationManager() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .isChildOf(itemFactory().createReferenceValue("some oid").relation(SchemaConstants.ORG_MANAGER))
                    .build();

            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(rQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = 0 and ref.relation in (:relation) and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n");

            @SuppressWarnings("unchecked")
            Collection<String> relationParameter = (Collection<String>) rQuery.getQuerySource().getParameters().get("relation").getValue();
            assertEquals("Wrong relation parameter value",
                    new HashSet<>(Arrays.asList(
                            RUtil.qnameToString(QNameUtil.nullNamespace(SchemaConstants.ORG_MANAGER)),
                            RUtil.qnameToString(SchemaConstants.ORG_MANAGER))),
                    new HashSet<>(relationParameter));

        } finally {
            close(em);
        }
    }

    // MID-4337
    @Test
    public void test130QuerySubtreeDistinctCount() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isChildOf(itemFactory().createReferenceValue("123"))
                    .build();

            String real = getInterpretedQuery(em, OrgType.class, query, true, distinct());

            // we probably do not need 'distinct' here
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  count(_o.oid)\n"
                    + "from\n"
                    + "  ROrg _o\n"
                    + "where\n"
                    + "  _o.oid in (select ref.ownerOid from RObjectReference ref"
                    + "   where ref.referenceType = 0"
                    + "    and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test135QueryRoots() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(OrgType.class)
                    .isRoot()
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery(em, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  ROrg _o\n" +
                    "where\n" +
                    "  _o.oid in (select descendantOid from ROrgClosure group by descendantOid having count(descendantOid) = 1)\n" +
                    "order by _o.nameCopy.orig asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test140ActivationQueryWrong() throws Exception {
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .build();

        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, UserType.class, query, false);

            // correct translation but probably not what the requester wants (use Exists instead)
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "    left join _u.assignments _a2 with _a2.assignmentOwner = :assignmentOwner2\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.activation.validFrom <= :validFrom or\n" +
                    "    _u.activation.validTo <= :validTo or\n" +
                    "    _a.activation.validFrom <= :validFrom2 or\n" +
                    "    _a2.activation.validTo <= :validTo2\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    // this one uses Exists to refer to the same value of assignment
    @Test
    public void test141ActivationQueryCorrect() throws Exception {
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

        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, UserType.class, query, false);

            // correct translation but probably not what the requester wants (use ForValue instead)
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.activation.validFrom <= :validFrom or\n" +
                    "    _u.activation.validTo <= :validTo or\n" +
                    "    ( _a.activation.validFrom <= :validFrom2 or\n" +
                    "      _a.activation.validTo <= :validTo2 )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test142ActivationQueryWrong() throws Exception {
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

        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, UserType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "    left join _u.assignments _a2 with _a2.assignmentOwner = :assignmentOwner2\n" +
                    "    left join _u.assignments _a3 with _a3.assignmentOwner = :assignmentOwner3\n" +
                    "    left join _u.assignments _a4 with _a4.assignmentOwner = :assignmentOwner4\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _u.activation.validFrom > :validFrom and\n" +
                    "      _u.activation.validFrom <= :validFrom2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      _u.activation.validTo > :validTo and\n" +
                    "      _u.activation.validTo <= :validTo2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      _a.activation.validFrom > :validFrom3 and\n" +
                    "      _a2.activation.validFrom <= :validFrom4\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      _a3.activation.validTo > :validTo3 and\n" +
                    "      _a4.activation.validTo <= :validTo4\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    // this one uses Exists to refer to the same value of assignment
    @Test
    public void test143ActivationQueryCorrect() throws Exception {
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

        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, UserType.class, query, false);

            // TODO rewrite with ForValue
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _u.activation.validFrom > :validFrom and\n" +
                    "      _u.activation.validFrom <= :validFrom2\n" +
                    "    ) or\n" +
                    "    (\n" +
                    "      _u.activation.validTo > :validTo and\n" +
                    "      _u.activation.validTo <= :validTo2\n" +
                    "    ) or\n" +
                    "    ( (\n" +
                    "        _a.activation.validFrom > :validFrom3 and\n" +
                    "        _a.activation.validFrom <= :validFrom4\n" +
                    "      ) or\n" +
                    "      (\n" +
                    "        _a.activation.validTo > :validTo3 and\n" +
                    "        _a.activation.validTo <= :validTo4\n" +
                    "      )\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test150OrgQuery() throws Exception {
        File objects = new File("src/test/resources/orgstruct/org-monkey-island.xml");
        OperationResult opResult = createOperationResult();
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
    public void test151QueryNameAndOrg() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .and().isChildOf("12341234-1234-1234-1234-123412341234")
                    .asc(F_NAME)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _u.nameCopy.orig = :orig and\n" +
                    "      _u.nameCopy.norm = :norm\n" +
                    "    ) and\n" +
                    "    _u.oid in (select ref.ownerOid from RObjectReference ref " +
                    "               where ref.referenceType = 0 and " +
                    "               ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "  )\n" +
                    "order by _u.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test153QueryObjectSubstringName() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery objectQuery = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).startsWith("a").matchingOrig()
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, objectQuery);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  _o.name.orig like :orig escape '!'\n");

            OperationResult result = createOperationResult();
            int count = repositoryService.countObjects(ObjectType.class, objectQuery, null, result);
            assertEquals(3, count);

            objectQuery = prismContext.queryFor(ObjectType.class)
                    .item(F_NAME).containsPoly("a").matchingOrig()
                    .build();
            count = repositoryService.countObjects(ObjectType.class, objectQuery, null, result);
            assertEquals(24, count);

        } finally {
            close(em);
        }
    }

    @Test
    public void test154queryObjectClassTypeUser() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  _o.objectTypeClass = :objectTypeClass");
        } finally {
            close(em);
        }
    }

    @Test
    public void test155queryObjectClassTypeAbstractRole() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(AbstractRoleType.class)
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  _o.objectTypeClass in (:objectTypeClass)");
        } finally {
            close(em);
        }
    }

    @Test
    public void test158queryMetadataTimestamp() throws Exception {
        EntityManager em = open();

        try {
            XMLGregorianCalendar timeXml = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
            ObjectQuery query = prismContext.queryFor(ReportDataType.class)
                    .item(ReportDataType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP).le(timeXml)
                    .build();
            String real = getInterpretedQuery(em, ReportDataType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _r.oid, _r.fullObject\n" +
                    "from\n" +
                    "  RReportData _r\n" +
                    "where\n" +
                    "  _r.createTimestamp <= :createTimestamp");
        } finally {
            close(em);
        }
    }

    @Test
    public void test160QueryObjectTypeByTypeUserAndLocality() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "   exists (\n"
                    + "    select\n"
                    + "      1\n"
                    + "    from\n"
                    + "      RUser _ou\n"
                    + "    where\n"
                    + "      _ou.oid = _o.oid and\n"
                    + "      (\n"
                    + "        _ou.localityFocus.orig = :orig and\n"
                    + "        _ou.localityFocus.norm = :norm\n"
                    + "      )\n"
                    + "  )");
        } finally {
            close(em);
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
    public void test162QueryObjectTypeByTypeOrgAndLocality() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(OrgType.class)
                    .item(OrgType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  exists (\n"
                    + "    select\n"
                    + "      1\n"
                    + "    from\n"
                    + "      ROrg _oo\n"
                    + "    where\n"
                    + "      _oo.oid = _o.oid and\n"
                    + "      (\n"
                    + "        _oo.localityFocus.orig = :orig and\n"
                    + "        _oo.localityFocus.norm = :norm\n"
                    + "      )\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test164QueryObjectTypeByTypeAndExtensionAttribute() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_EXTENSION, new QName("http://example.com/p", "weapon")).eq("some weapon name")
                    .build();

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, ObjectType.class, query, false, null);
            assertThat(getQueryString(realQuery))
                    .isEqualToIgnoringWhitespace("select\n"
                            + "  _o.oid, _o.fullObject\n"
                            + "from\n"
                            + "  RObject _o\n"
                            + "where\n"
                            + "  exists (\n"
                            + "    select\n"
                            + "      1\n"
                            + "    from\n"
                            + "      RUser _ou\n"
                            + "        left join _ou.strings _os with (\n"
                            + "_os.ownerType = :ownerType and\n"
                            + "_os.itemId = :itemId\n"
                            + ")\n"
                            + "    where\n"
                            + "      _ou.oid = _o.oid and\n"
                            + "      _os.value = :value\n"
                            + "  )");
            assertEquals("Wrong property ID for 'weapon'", weaponDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());

        } finally {
            close(em);
        }
    }

    @Test
    public void test165QueryObjectOrderByExtensionItem() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    // Ordering supports only single-value extensions, e.g. "weapon" would not work here.
                    .asc(UserType.F_EXTENSION, new QName("http://example.com/p", "shipName"))
                    .build();

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            assertThat(getQueryString(realQuery))
                    .isEqualToIgnoringWhitespace("select\n"
                            + "  _u.oid,\n"
                            + "  _u.fullObject\n"
                            + "from\n"
                            + "  RUser _u\n"
                            + "    left join _u.strings _s with (\n"
                            + "_s.ownerType = :ownerType and\n"
                            + "1=0\n"
                            + ")\n"
                            + "order by _s.value asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test166QueryObjectTypeByTypeAndReference() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .item(UserType.F_LINK_REF).ref("123")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  exists (\n"
                    + "    select\n"
                    + "      1\n"
                    + "    from\n"
                    + "      RUser _ou\n"
                    + "        left join _ou.linkRef _ol\n"
                    + "    where\n"
                    + "      _ou.oid = _o.oid and\n"
                    + "      (\n"
                    + "        _ol.targetOid = :targetOid and\n"
                    + "        _ol.relation in (:relation)\n"
                    + "      )\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test170QueryObjectTypeByTypeComplex() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(UserType.class)
                    .block()
                    .item(UserType.F_LOCALITY).eqPoly("Caribbean", "caribbean")
                    .or().item(UserType.F_LOCALITY).eqPoly("Adriatic", "adriatic")
                    .endBlock()
                    .or().type(OrgType.class)
                    .block()
                    // TODO: Changed OrgType.F_ORG_TYPE to OrgType.F_SUBTYPE, but the rest of the test needs fixing
                    .item(OrgType.F_SUBTYPE).eq("functional")
                    .endBlock()
                    .or().type(ReportType.class)
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        RUser _ou\n"
                    + "      where\n"
                    + "        _ou.oid = _o.oid and\n"
                    + "        (\n"
                    + "          (\n"
                    + "            _ou.localityFocus.orig = :orig and\n"
                    + "            _ou.localityFocus.norm = :norm\n"
                    + "          ) or\n"
                    + "          (\n"
                    + "            _ou.localityFocus.orig = :orig2 and\n"
                    + "            _ou.localityFocus.norm = :norm2\n"
                    + "          )\n"
                    + "        )\n"
                    + "    ) or\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        ROrg _oo\n"
                    + "          left join _oo.subtype _os\n"
                    + "      where\n"
                    + "        _oo.oid = _o.oid and\n"
                    + "        _os = :_os\n"
                    + "    ) or\n"
                    + "    _o.objectTypeClass = :objectTypeClass\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test171QueryObjectTypeByTwoAbstractTypes() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .type(FocusType.class).block().endBlock()
                    .or().type(AbstractRoleType.class).block().endBlock()
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    _o.objectTypeClass in (:objectTypeClass) or\n"
                    + "    _o.objectTypeClass in (:objectTypeClass2)\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test173QueryObjectTypeByTypeAndReference() throws Exception {
        EntityManager em = open();
        try {
            PrismObjectDefinition<RoleType> roleDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .id("c0c010c0-d34d-b33f-f00d-111111111111")
                    .or().type(RoleType.class)
                    .item(roleDef, RoleType.F_ROLE_MEMBERSHIP_REF).ref("c0c010c0-d34d-b33f-f00d-111111111111")
                    .build();
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid,\n"
                    + "  _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    _o.oid in :oid or\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        RRole _or\n"
                    + "          left join _or.roleMembershipRef _or2\n"
                    + "      where\n"
                    + "        _or.oid = _o.oid and\n"
                    + "        (\n"
                    + "          _or2.targetOid = :targetOid and\n"
                    + "          _or2.relation in (:relation)\n"
                    + "        )\n"
                    + "    )\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test175QueryObjectTypeByTypeAndOwnerRefOverloaded() throws Exception {
        EntityManager em = open();
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
            String real = getInterpretedQuery(em, ObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    _o.oid in :oid or\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        RAccessCertificationCampaign _oa\n"
                    + "      where\n"
                    + "        _oa.oid = _o.oid and\n"
                    + "        (\n"
                    + "          _oa.ownerRefCampaign.targetOid = :targetOid and\n"
                    + "          _oa.ownerRefCampaign.relation in (:relation)\n"
                    + "        )\n"
                    + "    ) or\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        RAccessCertificationDefinition _oa\n"
                    + "      where\n"
                    + "        _oa.oid = _o.oid and\n"
                    + "        (\n"
                    + "          _oa.ownerRefDefinition.targetOid = :targetOid2 and\n"
                    + "          _oa.ownerRefDefinition.relation in (:relation2)\n"
                    + "        )\n"
                    + "    ) or\n"
                    + "    exists (\n"
                    + "      select\n"
                    + "        1\n"
                    + "      from\n"
                    + "        RTask _ot\n"
                    + "      where\n"
                    + "        _ot.oid = _o.oid and\n"
                    + "        (\n"
                    + "          _ot.ownerRefTask.targetOid = :targetOid3 and\n"
                    + "          _ot.ownerRefTask.relation in (:relation3)\n"
                    + "        )\n"
                    + "    )\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void test178QueryGenericClob() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "locations")).isNull()
                    .build();
            getInterpretedQuery(em, GenericObjectType.class, query);
        } catch (QueryException ex) {
            logger.info("Exception", ex);
            throw ex;
        } finally {
            close(em);
        }
    }

    @Test
    public void test180QueryGenericString() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "stringType")).eq("asdf")
                    .build();
            String real = getInterpretedQuery(em, GenericObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _g.oid, _g.fullObject\n" +
                    "from\n" +
                    "  RGenericObject _g\n" +
                    "    left join _g.strings _s with ( _s.ownerType = :ownerType and _s.itemId = :itemId )\n" +
                    "where\n" +
                    "  _s.value = :value\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test182QueryGenericBoolean() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery objectQuery = prismContext.queryFor(GenericObjectType.class)
                    .item(ObjectType.F_EXTENSION, SKIP_AUTOGENERATION_QNAME).eq(true)
                    .build();

            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, GenericObjectType.class, objectQuery, false,
                    null);

            assertThat(getQueryString(realQuery))
                    .isEqualToIgnoringWhitespace("select\n"
                            + "  _g.oid, _g.fullObject\n"
                            + "from\n"
                            + "  RGenericObject _g\n"
                            + "    left join _g.booleans _b with ( _b.ownerType = :ownerType and _b.itemId = :itemId )\n"
                            + "where\n"
                            + "  _b.value = :value\n");
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
            close(em);
        }
    }

    @Test
    public void test184QueryAssignmentExtensionBoolean() throws Exception {
        EntityManager em = open();
        try {
            PrismPropertyDefinition<?> propDef = prismContext.definitionFactory().createPropertyDefinition(
                    SKIP_AUTOGENERATION_QNAME, DOMUtil.XSD_BOOLEAN);

            ObjectQuery objectQuery = prismContext.queryFor(UserType.class)
                    .itemWithDef(propDef, F_ASSIGNMENT, AssignmentType.F_EXTENSION, SKIP_AUTOGENERATION_QNAME).eq(true)
                    .build();

            String real = getInterpretedQuery(em, UserType.class, objectQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                    + "    left join _a.extension _e\n"
                    + "    left join _e.booleans _b with _b.itemId = :itemId\n"
                    + "where\n"
                    + "  _b.value = :value");

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
            close(em);
        }
    }

    @Test
    public void test185QueryExtensionEnum() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_EXTENSION, new QName("overrideActivation")).eq(ActivationStatusType.ENABLED)
                    .build();
            RQueryImpl realQuery = (RQueryImpl) getInterpretedQueryWhole(em, UserType.class, query, false, null);
            String real = getQueryString(realQuery);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.strings _s with (\n"
                    + "          _s.ownerType = :ownerType and\n"
                    + "          _s.itemId = :itemId\n"
                    + ")\n"
                    + "where\n"
                    + "  _s.value = :value\n");
            assertEquals("Wrong property ID for 'overrideActivation'", overrideActivationDefinition.getId(), realQuery.getQuerySource().getParameters().get("itemId").getValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test186QueryExtensionRef() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                    .item(F_EXTENSION, new QName("referenceType")).ref("123")
                    .build();
            String real = getInterpretedQuery(em, GenericObjectType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _g.oid, _g.fullObject\n"
                    + "from\n"
                    + "  RGenericObject _g\n"
                    + "    left join _g.references _r with (\n"
                    + "       _r.ownerType = :ownerType and\n"
                    + "       _r.itemId = :itemId\n"
                    + "    )\n"
                    + "where\n"
                    + "  (\n"
                    + "    _r.value = :value and\n"
                    + "    _r.relation in (:relation)\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test200QueryCertCaseAll() throws Exception {
        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, (ObjectQuery) null, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test202QueryCertWorkItemAll() throws Exception {
        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, (ObjectQuery) null, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test204QueryCertWorkItemAllOrderByCampaignName() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery q = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .asc(PrismConstants.T_PARENT, PrismConstants.T_PARENT, F_NAME)
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, q, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.owner _o\n"
                    + "    left join _o.owner _o2\n"
                    + "order by _o2.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test206QueryCertCaseOwner() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("123456")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "where\n" +
                    "  _a.ownerOid in :ownerOid");
        } finally {
            close(em);
        }
    }

    @Test
    public void test208QueryCertCaseOwnerAndTarget() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .ownerId("123456")
                    .and().item(AccessCertificationCaseType.F_TARGET_REF).ref("1234567890")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.ownerOid in :ownerOid and\n" +
                    "    (\n" +
                    "      _a.targetRef.targetOid = :targetOid and\n" +
                    "      _a.targetRef.relation in (:relation)\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test210QueryCertCaseReviewer() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref("1234567890")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOid, _a.id, _a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase _a\n"
                    + "    left join _a.workItems _w\n"
                    + "    left join _w.assigneeRef _a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a2.targetOid = :targetOid and\n"
                    + "    _a2.relation in (:relation)\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test212QueryCertWorkItemReviewers() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1")
                    .or().item(F_ASSIGNEE_REF).ref("oid2")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.assigneeRef _a2\n"
                    + "    left join _a.assigneeRef _a3\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a2.targetOid = :targetOid and\n"
                    + "      _a2.relation in (:relation)\n"
                    + "    ) or\n"
                    + "    (\n"
                    + "      _a3.targetOid = :targetOid2 and\n"
                    + "      _a3.relation in (:relation2)\n"
                    + "    )\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test216QueryCertCasesByCampaignOwner() throws Exception {
        EntityManager em = open();
        try {
            PrismReferenceValue ownerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .exists(T_PARENT)
                    .block()
                    .id(123456L)
                    .or().item(F_OWNER_REF).ref(ownerRef)
                    .endBlock()
                    .build();

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.owner _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    _o.oid in :oid or\n" +
                    "    (\n" +
                    "      _o.ownerRefCampaign.targetOid = :targetOid and\n" +
                    "      _o.ownerRefCampaign.relation in (:relation) and\n" +
                    "      _o.ownerRefCampaign.targetType = :targetType\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test218QueryCertCaseReviewerAndEnabled() throws Exception {
        EntityManager em = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .build();

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.workItems _w\n" +
                    "    left join _w.assigneeRef _a2\n" +
                    "    left join _a.owner _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _a2.targetOid = :targetOid and\n" +
                    "      _a2.relation in (:relation) and\n" +
                    "      _a2.targetType = :targetType\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      _a.stageNumber = _o.stageNumber or\n" +
                    "      (\n" +
                    "        _a.stageNumber is null and\n" +
                    "        _o.stageNumber is null\n" +
                    "      )\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test220QueryCertWorkItemReviewersMulti() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1", "oid2")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.assigneeRef _a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a2.targetOid in (:targetOid) and\n"
                    + "    _a2.relation in (:relation)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test230QueryCertCaseReviewerAndEnabledByDeadlineAndOidAsc() throws Exception {
        EntityManager em = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();

            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .asc(F_CURRENT_STAGE_DEADLINE).asc(T_ID)
                    .build();

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.workItems _w\n" +
                    "    left join _w.assigneeRef _a2\n" +
                    "    left join _a.owner _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _a2.targetOid = :targetOid and\n" +
                    "      _a2.relation in (:relation) and\n" +
                    "      _a2.targetType = :targetType\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      _a.stageNumber = _o.stageNumber or\n" +
                    "      (\n" +
                    "        _a.stageNumber is null and\n" +
                    "        _o.stageNumber is null\n" +
                    "      )\n" +
                    "    )\n" +
                    "  )\n" +
                    "order by _a.reviewDeadline asc, _a.id asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test232QueryCertWorkItemReviewerAndEnabledByDeadlineAndOidAsc() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .item(F_ASSIGNEE_REF).ref("oid1", "oid2")
                    .and().item(F_CLOSE_TIMESTAMP).isNull()
                    .asc(PrismConstants.T_PARENT, F_CURRENT_STAGE_DEADLINE).asc(T_ID)
                    .build();

            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.assigneeRef _a2\n"
                    + "    left join _a.owner _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a2.targetOid in (:targetOid) and\n"
                    + "      _a2.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    _a.closeTimestamp is null\n"
                    + "  )\n"
                    + "order by _o.reviewDeadline asc, _a.id asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test234QueryCertCaseReviewerAndEnabledByRequestedDesc() throws Exception {
        EntityManager em = open();
        try {
            PrismReferenceValue assigneeRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(F_WORK_ITEM, F_ASSIGNEE_REF).ref(assigneeRef)
                    .and().item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .and().item(T_PARENT, F_STATE).eq(IN_REVIEW_STAGE)
                    .desc(F_CURRENT_STAGE_CREATE_TIMESTAMP)
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.workItems _w\n" +
                    "    left join _w.assigneeRef _a2\n" +
                    "    left join _a.owner _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      _a2.targetOid = :targetOid and\n" +
                    "      _a2.relation in (:relation) and\n" +
                    "      _a2.targetType = :targetType\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      _a.stageNumber = _o.stageNumber or\n" +
                    "      (\n" +
                    "        _a.stageNumber is null and\n" +
                    "        _o.stageNumber is null\n" +
                    "      )\n" +
                    "    ) and\n" +
                    "    _o.state = :state\n" +
                    "  )\n" +
                    "order by _a.reviewRequestedTimestamp desc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test300QueryWorkItemsForCase() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .exists(PrismConstants.T_PARENT)
                    .block()
                    .id(1)
                    .and().ownerId("123456")
                    .endBlock()
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.owner _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    _o.id in :id and\n"
                    + "    _o.ownerOid in :ownerOid\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test302QueryWorkItemsForCampaign() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                    .exists(PrismConstants.T_PARENT)
                    .ownerId("campaignOid1")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationWorkItemType.class, query, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOwnerOid,\n"
                    + "  _a.ownerId,\n"
                    + "  _a.id\n"
                    + "from\n"
                    + "  RAccessCertificationWorkItem _a\n"
                    + "    left join _a.owner _o\n"
                    + "where\n"
                    + "    _o.ownerOid in :ownerOid\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test320AssignmentQuery() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                    .ownerId("1", "2").build();

            String real = getInterpretedQuery(em, AssignmentType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOid,\n"
                    + "  _a.id,\n"
                    + "  _a.order,\n"
                    + "  _a.lifecycleState,\n"
                    + "  _a.activation,\n"
                    + "  _a.targetRef,\n"
                    + "  _a.tenantRef,\n"
                    + "  _a.orgRef,\n"
                    + "  _a.resourceRef,\n"
                    + "  _a.createTimestamp,\n"
                    + "  _a.creatorRef,\n"
                    + "  _a.createChannel,\n"
                    + "  _a.modifyTimestamp,\n"
                    + "  _a.modifierRef,\n"
                    + "  _a.modifyChannel\n"
                    + "from\n"
                    + "  RAssignment _a\n"
                    + "where\n"
                    + "  _a.ownerOid in (:ownerOid)");
        } finally {
            close(em);
        }
    }

    @Test
    public void test321AssignmentQueryWithRoleTypeAndAnyRelation() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF).ref(prismContext.itemFactory()
                            .createReferenceValue(null, RoleType.COMPLEX_TYPE)
                            // any skips the relation condition, otherwise default is implied
                            .relation(Q_ANY))
                    .and().ownerId("1", "2")
                    .build();

            String real = getInterpretedQuery(em, AssignmentType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOid,\n"
                    + "  _a.id,\n"
                    + "  _a.order,\n"
                    + "  _a.lifecycleState,\n"
                    + "  _a.activation,\n"
                    + "  _a.targetRef,\n"
                    + "  _a.tenantRef,\n"
                    + "  _a.orgRef,\n"
                    + "  _a.resourceRef,\n"
                    + "  _a.createTimestamp,\n"
                    + "  _a.creatorRef,\n"
                    + "  _a.createChannel,\n"
                    + "  _a.modifyTimestamp,\n"
                    + "  _a.modifierRef,\n"
                    + "  _a.modifyChannel\n"
                    + "from\n"
                    + "  RAssignment _a\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.targetRef.targetType = :targetType and\n"
                    + "    _a.ownerOid in (:ownerOid)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test322AssignmentQueryByTargetName() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF).ref(prismContext.itemFactory()
                            .createReferenceValue(null, RoleType.COMPLEX_TYPE)
                            // any skips the relation condition, otherwise default is implied
                            .relation(Q_ANY))
                    .and().item(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME)
                    .eq("objectname")
                    .and().ownerId("1", "2")
                    .asc(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME)
                    .build();

            String real = getInterpretedQuery(em, AssignmentType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                      _a.ownerOid,
                      _a.id,
                      _a.order,
                      _a.lifecycleState,
                      _a.activation,
                      _a.targetRef,
                      _a.tenantRef,
                      _a.orgRef,
                      _a.resourceRef,
                      _a.createTimestamp,
                      _a.creatorRef,
                      _a.createChannel,
                      _a.modifyTimestamp,
                      _a.modifierRef,
                      _a.modifyChannel
                    from
                      RAssignment _a
                        left join RObject _t on _a.targetRef.target = _t
                    where
                      (
                        _a.targetRef.targetType = :targetType and
                        (
                          _t.name.orig = :orig and
                          _t.name.norm = :norm
                        ) and
                        _a.ownerOid in (:ownerOid)
                      )
                    order by _t.name.orig asc""");
        } finally {
            close(em);
        }
    }

    /**
     * Disabled because of the exception:
     *
     * Could not resolve attribute 'nameCopy' of 'com.evolveum.midpoint.repo.sql.data.common.RObject'
     * due to the attribute being declared in multiple sub types: ['com.evolveum.midpoint.repo.sql.data.common.RForm',
     * 'com.evolveum.midpoint.repo.sql.data.common.RUser']
     *
     * (Hibernate 6?)
     */
    @Test(enabled = false)
    public void test400DereferenceLink() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### UserType: linkRef/@/name contains 'test.com'
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME).containsPoly("test.com")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                      _u.oid, _u.fullObject
                    from
                      RUser _u
                        left join _u.linkRef _l
                        left join RShadow _t on _l.target = _t
                    where
                      (
                        _t.nameCopy.orig like :orig escape '!' and
                        _t.nameCopy.norm like :norm escape '!'
                      )
                    """);
        } finally {
            close(em);
        }
    }

    @Test
    public void test402DereferenceLinkedResourceName() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### UserType: linkRef/@/resourceRef/@/name contains 'CSV' (norm)
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE,
                            ShadowType.F_RESOURCE_REF, PrismConstants.T_OBJECT_REFERENCE,
                            F_NAME).containsPoly("CSV").matchingNorm()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                      _u.oid, _u.fullObject
                    from
                      RUser _u
                        left join _u.linkRef _l
                        left join RShadow _t on _l.target = _t
                        left join RObject _t2 on _t.resourceRef.target = _t2
                    where
                      _t2.name.norm like :norm escape '!'
                    """);

        } finally {
            close(em);
        }
    }

    /** See {@link SearchTest#test981SearchByArchetypeName()} */
    @Test
    public void test402DereferenceArchetypeName() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ARCHETYPE_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME).eqPoly("System")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                      _u.oid,
                      _u.fullObject
                    from
                      RUser _u
                        left join _u.archetypeRef _a
                        left join RArchetype _t on _a.target = _t
                    where
                      (
                        _t.nameCopy.orig = :orig and
                        _t.nameCopy.norm = :norm
                      )""");
        } finally {
            close(em);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class) // at this time
    public void test404DereferenceAssignedRoleType() throws Exception {
        EntityManager em = open();

        try {
            /*
             * This fails, as prism nor query interpreter expect that targetRef is RoleType/RRole.
             * Prism should implement something like "searching for proper root" when dereferencing "@".
             * QI should implement the proper root search not only at the query root, but always after a "@".
             *
             * ### UserType: assignment/targetRef/@/identifier
             */

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, RoleType.F_IDENTIFIER).eq("type1")
                    .build();
            getInterpretedQuery(em, UserType.class, query);

        } finally {
            close(em);
        }
    }

    @Test
    public void test420CaseParentFilter() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### AccessCertificationCaseType: Equal(../name, 'Campaign 1')
             */

            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(T_PARENT, F_NAME).eq("Campaign 1")
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.owner _o\n" +
                    "where\n" +
                    "  (\n" +
                    "    _o.nameCopy.orig = :orig and\n" +
                    "    _o.nameCopy.norm = :norm\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test500OrderBySingleton() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### UserType: order by activation/administrativeStatus
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    null,
                    prismContext.queryFactory().createPaging(
                            ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                            ASCENDING));

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "order by _u.activation.administrativeStatus asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test510OrderByParentCampaignName() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by ../name desc
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    prismContext.queryFactory().createPaging(ItemPath.create(T_PARENT, F_NAME), DESCENDING));

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "    left join _a.owner _o\n" +
                    "order by _o.nameCopy.orig desc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test520OrderByTargetName() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by targetRef/@/name
             */

            ObjectQuery query = prismContext.queryFactory().createQuery(
                    prismContext.queryFactory().createPaging(ItemPath.create(
                            AccessCertificationCaseType.F_TARGET_REF,
                            PrismConstants.T_OBJECT_REFERENCE, F_NAME), ASCENDING));

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                      _a.ownerOid, _a.id, _a.fullObject
                    from
                      RAccessCertificationCase _a
                        left join RObject _t on _a.targetRef.target = _t
                    order by _t.name.orig asc
                    """);
        } finally {
            close(em);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    // should fail, as Equals supports single-value right side only
    // TODO this should be perhaps checked in EqualFilter
    public void test550EqualsMultivalue() throws Exception {
        EntityManager em = open();

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

            getInterpretedQuery(em, UserType.class, query);
        } finally {
            close(em);
        }
    }

    @Test
    public void test555PreferredLanguageEqualsCostCenter() throws Exception {
        EntityManager em = open();

        try {
            /*
             * ### User: preferredLanguage = costCenter
             */
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_PREFERRED_LANGUAGE).eq().item(UserType.F_COST_CENTER)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.preferredLanguage = _u.costCenter or\n" +
                    "    (\n" +
                    "      _u.preferredLanguage is null and\n" +
                    "      _u.costCenter is null\n" +
                    "    )\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test560DecisionsNotAnswered() throws Exception {
        EntityManager em = open();

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

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOid, _a.id, _a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase _a\n"
                    + "    left join _a.workItems _w\n"
                    + "    left join _w.assigneeRef _a2\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a2.targetOid = :targetOid and\n"
                    + "      _a2.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      _w.stageNumber = _a.stageNumber or\n"
                    + "      (\n"
                    + "        _w.stageNumber is null and\n"
                    + "        _a.stageNumber is null\n"
                    + "      )\n"
                    + "    ) and\n"
                    + "      _w.outcome is null\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test562DecisionsNotAnsweredOrderBy() throws Exception {
        EntityManager em = open();

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

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.ownerOid, _a.id, _a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCase _a\n"
                    + "    left join _a.workItems _w\n"
                    + "    left join _w.assigneeRef _a2\n"
                    + "    left join _a.owner _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _a2.targetOid = :targetOid and\n"
                    + "      _a2.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    (\n"
                    + "      _w.stageNumber = _a.stageNumber or\n"
                    + "      (\n"
                    + "        _w.stageNumber is null and\n"
                    + "        _a.stageNumber is null\n"
                    + "      )\n"
                    + "    ) and\n"
                    + "      _w.outcome is null\n"
                    + "  )\n"
                    + "order by _o.nameCopy.orig asc, _a.id asc, _a.ownerOid asc\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test563ResourceRef() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_ASSIGNMENT, F_CONSTRUCTION, F_RESOURCE_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    _a.resourceRef.targetOid = :targetOid and\n" +
                    "    _a.resourceRef.relation in (:relation)\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test565CreatorRef() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_METADATA, F_CREATOR_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  (\n" +
                    "    _u.creatorRef.targetOid = :targetOid and\n" +
                    "    _u.creatorRef.relation in (:relation)\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test568CreateApproverRef() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_METADATA, F_CREATE_APPROVER_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "    left join _u.createApproverRef _c\n" +
                    "where\n" +
                    "  (\n" +
                    "    _c.targetOid = :targetOid and\n" +
                    "    _c.relation in (:relation)\n" +
                    "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test570FullTextSimple() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("Peter")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.textInfoItems _t\n"
                    + "where\n"
                    + "  _t.text like :text escape '!'");
        } finally {
            close(em);
        }
    }

    // adapt the test after query interpreter is optimized (when searching for empty text)
    @Test
    public void test571FullTextEmpty() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("\t\t\t")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.textInfoItems _t\n"
                    + "where\n"
                    + "  _t.text like :text escape '!'");
        } finally {
            close(em);
        }
    }

    @Test
    public void test575FullTextMulti() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .fullText("\t\nPeter\t\tMravec\t")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.textInfoItems _t\n"
                    + "    left join _u.textInfoItems _t2\n"
                    + "where\n"
                    + "  (\n"
                    + "    _t.text like :text escape '!' and\n"
                    + "    _t2.text like :text2 escape '!'\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test580RedundantBlock() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .item(ShadowType.F_NAME).eqPoly("aaa")
                    .endBlock()
                    .build();

            String real = getInterpretedQuery(em, ShadowType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid, _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "where\n"
                    + "  (\n"
                    + "    _s.nameCopy.orig = :orig and\n"
                    + "    _s.nameCopy.norm = :norm\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test582TwoRedundantBlocks() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .block()
                    .item(ShadowType.F_NAME).eqPoly("aaa")
                    .endBlock()
                    .endBlock()
                    .build();

            String real = getInterpretedQuery(em, ShadowType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid, _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "where\n"
                    + "  (\n"
                    + "    _s.nameCopy.orig = :orig and\n"
                    + "    _s.nameCopy.norm = :norm\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test584RedundantBlocksAndExists() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .block()
                    .item(ShadowType.F_RESOURCE_REF).ref("111")
                    .and()
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .endBlock()
                    .build();

            String real = getInterpretedQuery(em, ShadowType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _s.resourceRef.targetOid = :targetOid and\n"
                    + "      _s.resourceRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    _s.pendingOperationCount > :pendingOperationCount\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test600AvailabilityStatus() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ResourceType.class)
                    .item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS).eq(AvailabilityStatusType.UP)
                    .build();
            String real = getInterpretedQuery(em, ResourceType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _r.oid, _r.fullObject\n"
                    + "from\n"
                    + "  RResource _r\n"
                    + "where\n"
                    + "  _r.operationalState.lastAvailabilityStatus = :lastAvailabilityStatus\n");
        } finally {
            close(em);
        }

    }

    @Test
    public void test602NullRefSingle() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ResourceType.class)
                    .item(ResourceType.F_CONNECTOR_REF).isNull()
                    .build();
            String real = getInterpretedQuery(em, ResourceType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _r.oid, _r.fullObject\n"
                    + "from\n"
                    + "  RResource _r\n"
                    + "where\n"
                    + "  _r.connectorRef is null");
        } finally {
            close(em);
        }
    }

    // the same as test074QueryUserAccountRefNull, but keeping because of test structure
    @Test
    public void test604NullRefMulti() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.linkRef _l\n"
                    + "where\n"
                    + "  _l is null\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test606NullEqSingle() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_EMPLOYEE_NUMBER).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.employeeNumber is null");
        } finally {
            close(em);
        }
    }

    @Test
    public void test610NullEqMulti() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_SUBTYPE).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "    left join _u.subtype _s\n"
                    + "where\n"
                    + "  _s is null");
        } finally {
            close(em);
        }
    }

    @Test
    public void test612AbstractRoleParameters() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(RoleType.class)
                    .item(RoleType.F_RISK_LEVEL).eq("critical")
                    .and().item(RoleType.F_IDENTIFIER).eq("001")
                    .and().item(RoleType.F_DISPLAY_NAME).eqPoly("aaa", "aaa").matchingNorm()
                    .build();
            String real = getInterpretedQuery(em, RoleType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _r.oid, _r.fullObject\n"
                    + "from\n"
                    + "  RRole _r\n"
                    + "where\n"
                    + "  (\n"
                    + "    _r.riskLevel = :riskLevel and\n"
                    + "    _r.identifier = :identifier and\n"
                    + "    _r.displayName.norm = :norm\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test614ExistsShadowPendingOperation() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ShadowType.class)
                    .exists(ShadowType.F_PENDING_OPERATION)
                    .build();
            String real = getInterpretedQuery(em, ShadowType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "where\n"
                    + "  _s.pendingOperationCount > :pendingOperationCount");
        } finally {
            close(em);
        }
    }

    @Test
    public void test616OperationFatalError() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .build();
            String real = getInterpretedQuery(em, ShadowType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.operationExecutions _o\n"
                    + "where\n"
                    + "  _o.status = :status\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test617OperationFatalErrorTimestampSort() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .desc(F_OPERATION_EXECUTION, OperationExecutionType.F_TIMESTAMP)
                    .build();
            String real = getInterpretedQuery(em, ShadowType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.operationExecutions _o\n"
                    + "where\n"
                    + "  _o.status = :status\n"
                    + "order by _o.timestamp desc");
        } finally {
            close(em);
        }
    }

    @Test(description = "MID-6561, reproducible on Oracle and SQL Server")
    public void test618OperationFatalErrorTimestampSortExistsDistinct() throws Exception {
        EntityManager em = open();
        try {
            given("a task errors query with exists-to-many relation ordered by timestamp");
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(ObjectType.F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_TASK_REF).ref("some-task-oid")
                    .and()
                    .block().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.FATAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.PARTIAL_ERROR)
                    .or().item(OperationExecutionType.F_STATUS)
                    .eq(OperationResultStatusType.WARNING)
                    .endBlock()
                    .endBlock()
                    // timestamp is behind that to-many relation
                    .desc(F_OPERATION_EXECUTION, OperationExecutionType.F_TIMESTAMP)
                    .build();

            when("the query is executed using distinct");
            String real = getInterpretedQuery(em, ShadowType.class, query, false, distinct());

            then("expected HQL is generated");
            SqlRepositoryConfiguration config = getConfiguration();
            if (config.isUsingOracle() || config.isUsingSQLServer()) {
                // this specifically is the fixed version for MID-6561
                assertThat(real).isEqualToIgnoringWhitespace("select\n"
                        + "  _s.oid,\n"
                        + "  _s.fullObject\n"
                        + "from\n"
                        + "  RShadow _s\n"
                        + "    left join _s.operationExecutions _o\n"
                        + "where\n"
                        + "  (\n"
                        + "    (\n"
                        + "      _o.taskRef.targetOid = :targetOid2 and\n"
                        + "      _o.taskRef.relation in (:relation2)\n"
                        + "    ) and\n"
                        + "    (\n"
                        + "      _o.status = :status4 or\n"
                        + "      _o.status = :status5 or\n"
                        + "      _o.status = :status6\n"
                        + "    )\n"
                        + "  ) and\n"
                        + "  _s.oid in (\n"
                        + "    select distinct\n"
                        + "      _s.oid\n"
                        + "    from\n"
                        + "      RShadow _s\n"
                        + "        left join _s.operationExecutions _o\n"
                        + "    where\n"
                        + "      (\n"
                        + "        (\n"
                        + "          _o.taskRef.targetOid = :targetOid and\n"
                        + "          _o.taskRef.relation in (:relation)\n"
                        + "        ) and\n"
                        + "        (\n"
                        + "          _o.status = :status or\n"
                        + "          _o.status = :status2 or\n"
                        + "          _o.status = :status3\n"
                        + "        )\n"
                        + "      ))\n"
                        + "order by _o.timestamp desc");
            } else {
                assertThat(real).isEqualToIgnoringWhitespace("select distinct\n"
                        + "  _s.oid,\n"
                        + "  _s.fullObject,\n"
                        + "  _o.timestamp\n"
                        + "from\n"
                        + "  RShadow _s\n"
                        + "    left join _s.operationExecutions _o\n"
                        + "where\n"
                        + "  (\n"
                        + "    (\n"
                        + "      _o.taskRef.targetOid = :targetOid and\n"
                        + "      _o.taskRef.relation in (:relation)\n"
                        + "    ) and\n"
                        + "    (\n"
                        + "      _o.status = :status or\n"
                        + "      _o.status = :status2 or\n"
                        + "      _o.status = :status3\n"
                        + "    )\n"
                        + "  )\n"
                        + "order by _o.timestamp desc");
            }
        } finally {
            close(em);
        }
    }

    @Test
    public void test619OperationSuccessForGivenTask() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_TASK_REF).ref("oid1")
                    .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.SUCCESS)
                    .endBlock()
                    .build();
            String real = getInterpretedQuery(em, ShadowType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.operationExecutions _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    (\n"
                    + "      _o.taskRef.targetOid = :targetOid and\n"
                    + "      _o.taskRef.relation in (:relation)\n"
                    + "    ) and\n"
                    + "    _o.status = :status\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test620OperationLastFailures() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .exists(F_OPERATION_EXECUTION)
                    .block()
                    .item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                    .and().item(OperationExecutionType.F_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .endBlock()
                    .build();
            String real = getInterpretedQuery(em, ShadowType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _s.oid,\n"
                    + "  _s.fullObject\n"
                    + "from\n"
                    + "  RShadow _s\n"
                    + "    left join _s.operationExecutions _o\n"
                    + "where\n"
                    + "  (\n"
                    + "    _o.status = :status and\n"
                    + "    _o.timestamp <= :timestamp\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test621PersonaRef() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(FocusType.class)
                    .item(FocusType.F_PERSONA_REF).ref("123456")
                    .build();
            String real = getInterpretedQuery(em, FocusType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _f.oid,\n"
                    + "  _f.fullObject\n"
                    + "from\n"
                    + "  RFocus _f\n"
                    + "    left join _f.personaRef _p\n"
                    + "where\n"
                    + "  (\n"
                    + "    _p.targetOid = :targetOid and\n"
                    + "    _p.relation in (:relation)\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test622IgnorableDistinctAndOrderBy() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .asc(UserType.F_NAME)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query, false, distinct());

            assertThat(real).isEqualToIgnoringWhitespace("select _u.oid,\n"
                    + "  _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "order by _u.nameCopy.orig asc\n");
        } finally {
            close(em);
        }
    }

    // TODO: Changed F_EMPLOYEE_TYPE to F_SUBTYPE, but the expected query still needs fixing
    @Test
    public void test623ApplicableDistinctAndOrderBy() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).startsWith("e")
                .asc(UserType.F_NAME)
                .build();

        EntityManager em = open();
        try {
            String real = getInterpretedQuery(em, UserType.class, query, false, distinct());
            String expected;
            SqlRepositoryConfiguration config = getConfiguration();
            if (config.isUsingOracle() || config.isUsingSQLServer()) {
                expected = "select\n"
                        + "  _u.oid,\n"
                        + "  _u.fullObject\n"
                        + "from\n"
                        + "  RUser _u\n"
                        + "where\n"
                        + "  _u.oid in ("
                        + "    select distinct\n"
                        + "      _u.oid\n"
                        + "    from\n"
                        + "      RUser _u left join _u.subtype _s where _s like :_s escape '!')\n"
                        + "order by _u.nameCopy.orig asc";
            } else {
                expected = "select distinct\n"
                        + "  _u.oid,\n"
                        + "  _u.fullObject,\n"
                        + "  _u.nameCopy.orig\n"
                        + "from\n"
                        + "  RUser _u left join _u.subtype _s where _s like :_s escape '!'\n"
                        + "order by _u.nameCopy.orig asc\n";
            }
            assertThat(real).isEqualToIgnoringWhitespace(expected);
        } finally {
            close(em);
        }

        SearchResultList<PrismObject<UserType>> objects = repositoryService
                .searchObjects(UserType.class, query, null, new OperationResult("dummy"));
        System.out.println("objects: " + objects);
        // just to know if the execution was successful
    }

    @Test
    public void test624DistinctUserWithAssignment() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("123456")
                    .asc(UserType.F_NAME)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query, false, distinct());
            String expected;
            SqlRepositoryConfiguration config = getConfiguration();
            if (config.isUsingOracle() || config.isUsingSQLServer()) {
                expected = "select\n"
                        + "  _u.oid,\n"
                        + "  _u.fullObject\n"
                        + "from\n"
                        + "  RUser _u\n"
                        + "where\n"
                        + "  _u.oid in (\n"
                        + "    select distinct\n"
                        + "      _u.oid\n"
                        + "    from\n"
                        + "      RUser _u\n"
                        + "        left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                        + "    where\n"
                        + "      (\n"
                        + "        _a.targetRef.targetOid = :targetOid and\n"
                        + "        _a.targetRef.relation in (:relation)\n"
                        + "      ))\n"
                        + "order by _u.nameCopy.orig asc";
            } else {
                expected = "select distinct\n"
                        + "  _u.oid,\n"
                        + "  _u.fullObject,\n"
                        + "  _u.nameCopy.orig\n"
                        + "from\n"
                        + "  RUser _u\n"
                        + "    left join _u.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                        + "where\n"
                        + "  (\n"
                        + "    _a.targetRef.targetOid = :targetOid and\n"
                        + "    _a.targetRef.relation in (:relation)\n"
                        + "  )\n"
                        + "order by _u.nameCopy.orig asc\n";
            }
            assertThat(real).isEqualToIgnoringWhitespace(expected);
        } finally {
            close(em);
        }
    }

    @Test
    public void test625CampaignEndTimestamp() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                    .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                    .and().item(AccessCertificationCampaignType.F_END_TIMESTAMP).lt(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()))
                    .and().not().id(new String[0])
                    .maxSize(10)
                    .build();
            query.setFilter(ObjectQueryUtil.simplify(query.getFilter())); // necessary to remove "not oid()" clause
            String real = getInterpretedQuery(em, AccessCertificationCampaignType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.oid,\n"
                    + "  _a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCampaign _a\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.state = :state and\n"
                    + "    _a.end < :end\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test626CampaignEndTimestamp2() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                    .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                    .and().not().id("10-10-10-10-10")   // hoping there are not many of these
                    .desc(AccessCertificationCampaignType.F_END_TIMESTAMP)
                    .offset(100)
                    .maxSize(20)
                    .build();
            String real = getInterpretedQuery(em, AccessCertificationCampaignType.class, query);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _a.oid,\n"
                    + "  _a.fullObject\n"
                    + "from\n"
                    + "  RAccessCertificationCampaign _a\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.state = :state and\n"
                    + "    not _a.oid in :oid\n"
                    + "  )\n"
                    + "order by _a.end desc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test627IgnorableDistinctWithCount() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query, true, distinct());

            assertThat(real).isEqualToIgnoringWhitespace("select count(_u.oid) from RUser _u");
        } finally {
            close(em);
        }
    }

    // TODO: Changed F_EMPLOYEE_TYPE to F_SUBTYPE, but the expected query still needs fixing
    @Test
    public void test628ApplicableDistinctWithCount() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_SUBTYPE).startsWith("a")
                    .build();
            String real = getInterpretedQuery(em, UserType.class, query, true, distinct());

            assertThat(real).isEqualToIgnoringWhitespace("select"
                    + " count(distinct _u.oid)"
                    + " from RUser _u"
                    + " left join _u.subtype _s"
                    + " where _s like :_s escape '!'");
        } finally {
            close(em);
        }
    }

    @Test
    public void test630OidEqTest() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(PrismConstants.T_ID).eq("1")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  _o.oid = :oid\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test631OwnerOidEqTest() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(PrismConstants.T_PARENT, PrismConstants.T_ID).eq("1")
                    .build();

            String real = getInterpretedQuery(em, AccessCertificationCaseType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _a.ownerOid, _a.id, _a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase _a\n" +
                    "where\n" +
                    "  _a.ownerOid = :ownerOid");
        } finally {
            close(em);
        }
    }

    @Test
    public void test632OidGeLtTest() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(PrismConstants.T_ID).ge("1")
                    .and().item(PrismConstants.T_ID).lt("2")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _o.oid, _o.fullObject\n" +
                    "from\n" +
                    "  RObject _o\n" +
                    "where\n" +
                    "  ( _o.oid >= :oid and _o.oid < :oid2 )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test700QueryOrderByNameOrigLimit20() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class).asc(UserType.F_NAME).maxSize(20).build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "order by _u.nameCopy.orig asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test702QueryOrderByNameOrigLimit20() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(RoleType.class).asc(RoleType.F_NAME).maxSize(20).build();

            String real = getInterpretedQuery(em, RoleType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _r.oid, _r.fullObject\n" +
                    "from\n" +
                    "  RRole _r\n" +
                    "order by\n" +
                    "_r.nameCopy.orig asc");
        } finally {
            close(em);
        }
    }

    @Test
    public void test704QueryTasksForArchetypeRef() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(AssignmentHolderType.F_ARCHETYPE_REF).ref("oid1")
                    .build();

            String real = getInterpretedQuery(em, TaskType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _t.oid,\n"
                    + "  _t.fullObject\n"
                    + "from\n"
                    + "  RTask _t\n"
                    + "    left join _t.archetypeRef _a\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.targetOid = :targetOid and\n"
                    + "    _a.relation in (:relation)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test706QuerySearchForFocusType() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, FocusType.class, (ObjectQuery) null);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _f.oid,\n"
                    + "  _f.fullObject\n"
                    + "from\n"
                    + "  RFocus _f");
        } finally {
            close(em);
        }
    }

    @Test
    public void test708QuerySearchForAssignmentHolderType() throws Exception {
        EntityManager em = open();

        try {
            RQueryImpl rQuery = (RQueryImpl) getInterpretedQueryWhole(em, AssignmentHolderType.class, null, false, null);
            String real = getQueryString(rQuery);
            System.out.println("Query parameters:\n" + rQuery.getQuerySource().getParameters());

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "where\n"
                    + "  _o.objectTypeClass in (:objectTypeClass)");

        } finally {
            close(em);
        }
    }

    @Test
    public void test720QuerySearchForObjectType() throws Exception {
        EntityManager em = open();

        try {
            String real = getInterpretedQuery(em, ObjectType.class, (ObjectQuery) null);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o");
        } finally {
            close(em);
        }
    }

    @Test
    public void test725QueryNameNormAsString() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).eq("asdf").matchingNorm().build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  _u.nameCopy.norm = :norm");
        } finally {
            close(em);
        }
    }

    @Test
    public void test730QueryWorkItemsByAssignee() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery q = prismContext.queryFor(CaseWorkItemType.class)
                    .item(CaseWorkItemType.F_ASSIGNEE_REF).ref("123")
                    .build();
            String real = getInterpretedQuery(em, CaseWorkItemType.class, q, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _c.ownerOid,\n"
                    + "  _c.id\n"
                    + "from\n"
                    + "  RCaseWorkItem _c\n"
                    + "    left join _c.assigneeRef _a\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.targetOid = :targetOid and\n"
                    + "    _a.relation in (:relation)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test732QueryWorkItemsByCandidate() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery q = prismContext.queryFor(CaseWorkItemType.class)
                    .item(CaseWorkItemType.F_CANDIDATE_REF).ref("123")
                    .build();
            String real = getInterpretedQuery(em, CaseWorkItemType.class, q, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _c.ownerOid,\n"
                    + "  _c.id\n"
                    + "from\n"
                    + "  RCaseWorkItem _c\n"
                    + "    left join _c.candidateRef _c2\n"
                    + "where\n"
                    + "  (\n"
                    + "    _c2.targetOid = :targetOid and\n"
                    + "    _c2.relation in (:relation)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    // MID-5515
    @Test
    public void test733QueryNameNull() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery q = prismContext.queryFor(UserType.class)
                    .item(F_NAME).isNull()
                    .build();
            String real = getInterpretedQuery(em, UserType.class, q, false);

            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid,\n"
                    + "  _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  1=0\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test734QueryPolicySituationOnObject() throws Exception {
        EntityManager em = open();
        try {
            ObjectQuery query = prismContext.queryFor(ObjectType.class)
                    .item(F_POLICY_SITUATION)
                    .eq("policy-situation-URL")
                    .build();

            String real = getInterpretedQuery(em, ObjectType.class, query, false);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _o.oid, _o.fullObject\n"
                    + "from\n"
                    + "  RObject _o\n"
                    + "    left join _o.policySituation _p\n"
                    + "where\n"
                    + "  _p = :_p");
        } finally {
            close(em);
        }
    }

    @Test
    public void test735QueryPasswordCreateTimestamp() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
                            PasswordType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                    .gt(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())).build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n" +
                    "  _u.oid, _u.fullObject\n" +
                    "from\n" +
                    "  RUser _u\n" +
                    "where\n" +
                    "  _u.passwordCreateTimestamp > :passwordCreateTimestamp");
        } finally {
            close(em);
        }
    }

    @Test
    public void test736QueryPolyStringGtOrig() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).gt("J").matchingOrig()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.nameCopy.orig > :orig\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test737QueryPolyStringGtNorm() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).gt("j").matchingNorm()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid, _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  _u.nameCopy.norm > :norm");
        } finally {
            close(em);
        }
    }

    @Test
    public void test738QueryPolyStringGtStrict() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).gt("j").matchingStrict()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid,\n"
                    + "  _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  (\n"
                    + "    _u.nameCopy.orig > :orig and\n"
                    + "    _u.nameCopy.norm > :norm\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    // For polystrings, default matching is the strict one.
    @Test
    public void test739QueryPolyStringGtDefault() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_NAME).gt("j")
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid,\n"
                    + "  _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  (\n"
                    + "    _u.nameCopy.orig > :orig and\n"
                    + "    _u.nameCopy.norm > :norm\n"
                    + "  )\n");
        } finally {
            close(em);
        }
    }

    @Test
    public void test740QueryStringGtIgnoreCase() throws Exception {
        EntityManager em = open();

        try {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(F_EMPLOYEE_NUMBER).gt("j").matchingCaseIgnore()
                    .build();

            String real = getInterpretedQuery(em, UserType.class, query);
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _u.oid,\n"
                    + "  _u.fullObject\n"
                    + "from\n"
                    + "  RUser _u\n"
                    + "where\n"
                    + "  lower(_u.employeeNumber) > :employeeNumber");
        } finally {
            close(em);
        }
    }

    // reproduced MID-6501
    @Test
    public void test800QueryAssignmentPathOnNonFocusTypes() throws Exception {
        EntityManager em = open();

        try {
            given("a Case (non-focus object type) query with assignment attribute");
            ObjectQuery query = prismContext.queryFor(CaseType.class)
                    .exists(F_ASSIGNMENT)
                    .item(AssignmentType.F_TARGET_REF).ref("target-oid-123")
                    .build();

            when("the query is executed");
            String real = getInterpretedQuery(em, CaseType.class, query);

            then("expected HQL is generated");
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _c.oid,\n"
                    + "  _c.fullObject\n"
                    + "from\n"
                    + "  RCase _c\n"
                    + "    left join _c.assignments _a with _a.assignmentOwner = :assignmentOwner\n"
                    + "where\n"
                    + "  (\n"
                    + "    _a.targetRef.targetOid = :targetOid and\n"
                    + "    _a.targetRef.relation in (:relation)\n"
                    + "  )");
        } finally {
            close(em);
        }
    }

    @Test
    public void test801QueryTaskRecurrence() throws Exception {
        EntityManager em = open();

        try {
            given();
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_SCHEDULE, ScheduleType.F_RECURRENCE)
                    .eq(TaskRecurrenceType.RECURRING)
                    .build();

            when("the query is executed");
            String real = getInterpretedQuery(em, TaskType.class, query);

            then("expected HQL is generated");
            assertThat(real).isEqualToIgnoringWhitespace("select\n"
                    + "  _t.oid,\n"
                    + "  _t.fullObject\n"
                    + "from\n"
                    + "  RTask _t\n"
                    + "where\n"
                    + "  _t.recurrence = :recurrence");
        } finally {
            close(em);
        }
    }

    /** @see SearchTest#test943MultiValueRefTargetWithTargetTypeSpecificCondition() */
    @Test
    public void test802MultiValueRefTargetWithTargetTypeSpecificCondition() throws Exception {
        EntityManager em = open();

        try {
            given();
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(UserType.F_LINK_REF, T_OBJECT_REFERENCE, ShadowType.F_RESOURCE_REF).ref("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2")
                    .build();

            when("the query is executed");
            String real = getInterpretedQuery(em, UserType.class, query);

            then("expected HQL is generated");
            assertThat(real).isEqualToIgnoringWhitespace("""
                    select
                        _u.oid,
                        _u.fullObject
                    from
                        RUser _u
                          left join _u.linkRef _l
                          left join RShadow _t on _l.target = _t
                    where
                        (
                          _t.resourceRef.targetOid = :targetOid and
                          _t.resourceRef.relation in (:relation)
                        )""");
        } finally {
            close(em);
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> distinct() {
        return createCollection(createDistinct());
    }

    private SqlRepositoryConfiguration getConfiguration() {
        return sqlRepositoryService.sqlConfiguration();
    }

    // TODO negative tests - order by entity, reference, any, collection
    // TODO implement checks for "order by" for non-singletons

    private <T extends Containerable> String getInterpretedQuery(
            EntityManager em, Class<T> type, File file) throws Exception {
        return getInterpretedQuery(em, type, file, false);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Containerable> String getInterpretedQuery(
            EntityManager em, Class<T> type, File file, boolean interpretCount) throws Exception {
        ObjectQuery query = getQuery(file, type);
        return getInterpretedQuery(em, type, query, interpretCount);
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

    private <T extends Containerable> String getInterpretedQuery(EntityManager em, Class<T> type, ObjectQuery query) throws Exception {
        return getInterpretedQuery(em, type, query, false);
    }

    private <T extends Containerable> String getInterpretedQuery(EntityManager em, Class<T> type, ObjectQuery query,
            boolean interpretCount) throws Exception {
        return getInterpretedQuery(em, type, query, interpretCount, null);
    }

    private <T extends Containerable> String getInterpretedQuery(EntityManager em, Class<T> type, ObjectQuery query,
            boolean interpretCount, Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
        RQuery rQuery = getInterpretedQueryWhole(em, type, query, interpretCount, options);
        return getQueryString((RQueryImpl) rQuery);
    }

    @NotNull
    private <T extends Containerable> RQuery getInterpretedQueryWhole(EntityManager em, Class<T> type, ObjectQuery query,
            boolean interpretCount, Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {
        if (query != null) {
            logger.info("QUERY TYPE TO CONVERT :\n{}", (query.getFilter() != null ? query.getFilter().debugDump(3) : null));
        }

        QueryEngine engine = new QueryEngine(baseHelper.getConfiguration(), extItemDictionary, prismContext, relationRegistry);
        RQuery rQuery = engine.interpret(query, type, options, interpretCount, em);
        //just test if DB will handle it or throws some exception
        if (interpretCount) {
            rQuery.uniqueResult();
        } else {
            rQuery.list();
        }
        return rQuery;
    }
}
