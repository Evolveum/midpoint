/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.QueryEngine2;
import com.evolveum.midpoint.repo.sql.query2.RQueryImpl;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_ID;
import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.query.OrderDirection.ASCENDING;
import static com.evolveum.midpoint.prism.query.OrderDirection.DESCENDING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_DECISION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_REVIEWER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_REVIEW_DEADLINE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_CONSTRUCTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType.F_RESOURCE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_CREATE_APPROVER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_CREATOR_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_EXTENSION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_METADATA;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

/**
 * @author lazyman
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryInterpreter2Test extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreter2Test.class);
    private static final File TEST_DIR = new File("./src/test/resources/query");

    private static final QName SKIP_AUTOGENERATION = new QName("http://example.com/p", "skipAutogeneration");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        List<PrismObject<? extends Objectable>> objects = prismContext.parseObjects(
                new File(FOLDER_BASIC, "objects.xml"));
        OperationResult result = new OperationResult("add objects");
        for (PrismObject object : objects) {
            repositoryService.addObject(object, null, result);
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test001QueryNameNorm() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringNorm)
             */
            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingNorm().build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.name.norm = :norm";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test002QueryNameOrig() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectFilter filter = EqualFilter.createEqual(F_NAME, UserType.class, prismContext,
                    PolyStringOrigMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_NAME).eqPoly("asdf", "asdf").matchingOrig().build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.name.orig = :orig";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test003QueryNameStrict() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (name, "asdf", PolyStringOrig)
             */
            ObjectFilter filter = EqualFilter.createEqual(F_NAME, UserType.class, prismContext,
                    null, new PolyString("asdf", "asdf"));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_NAME).eqPoly("asdf", "asdf").build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  ( u.name.orig = :orig and u.name.norm = :norm )";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test010QueryOrganizationNorm() throws Exception {
        Session session = open();

        try {
            /*
             *  ### user: Equal (organization, "asdf", PolyStringNorm)
             */
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    PolyStringNormMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingNorm().build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.organization o\n" +
                    "where\n" +
                    "  o.norm = :norm";

            String real = getInterpretedQuery2(session, UserType.class, query);
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test011QueryOrganizationOrig() throws Exception {
        Session session = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    PolyStringOrigMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingOrig().build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
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
    public void test012QueryOrganizationStrict() throws Exception {
        Session session = open();
        try {
            /*
             *  ### user: Equal (organization, "asdf")
             */

            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    null, new PolyString("asdf", "asdf"));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingStrict().build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
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
    public void test020QueryTwoOrganizationsNormAnd() throws Exception {
        Session session = open();
        try {
            /*
             *  UserType: And (Equal (organization, 'asdf', PolyStringNorm),
             *                 Equal (organization, 'ghjk', PolyStringNorm))
             */
            ObjectFilter filter = AndFilter.createAnd(
                    EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext, PolyStringNormMatchingRule.NAME, new PolyString("asdf", "asdf")),
                    EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext, PolyStringNormMatchingRule.NAME, new PolyString("ghjk", "ghjk")));

            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf").matchingNorm()
                    .and().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk").matchingNorm()
                    .build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
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
    public void test021QueryTwoOrganizationsStrictOr() throws Exception {
        Session session = open();
        try {
            /*
             *  UserType: Or (Equal (organization, 'asdf'),
             *                Equal (organization, 'ghjk'))
             */
            ObjectFilter filter = OrFilter.createOr(
                    EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext, null, new PolyString("asdf", "asdf")),
                    EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext, null, new PolyString("ghjk", "ghjk")));

            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_ORGANIZATION).eqPoly("asdf", "asdf")
                    .or().item(UserType.F_ORGANIZATION).eqPoly("ghjk", "ghjk")
                    .build();

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
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
    public void test025QueryOrganizationOrigPolymorphic() throws Exception {
        Session session = open();
        try {
            /*
             *  ### object: Equal (organization, "asdf", PolyStringOrig)
             */
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    PolyStringOrigMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String expected = "select\n" +
                    "  o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount\n" +
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
    public void test030QueryTaskDependent() throws Exception {
        Session session = open();

        try {
            /*
             *  ### task: Equal (dependent, "123456")
             */
            ObjectFilter filter = EqualFilter.createEqual(TaskType.F_DEPENDENT, TaskType.class, prismContext, null, "123456");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String expected = "select\n" +
                    "  t.fullObject, t.stringsCount, t.longsCount, t.datesCount, t.referencesCount, t.polysCount, t.booleansCount\n" +
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
    public void test040QueryClob() throws Exception {
        Session session = open();

        try {
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_DESCRIPTION, UserType.class, prismContext, null, "aaa");
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_DESCRIPTION).eq("aaa")
                    .build();

            //should throw exception, because description is lob and can't be queried
            getInterpretedQuery2(session, UserType.class, query);
        } finally {
            close(session);
        }
    }

    @Test
    public void test050QueryEnum() throws Exception {
        Session session = open();
        try {
            /*
             *  ### task: Equal (executionStatus, WAITING)
             */
            ObjectFilter filter = EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS, TaskType.class, prismContext,
                    null, TaskExecutionStatusType.WAITING);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, TaskType.class, query);

            String expected = "select\n" +
                    "  t.fullObject,\n" +
                    "  t.stringsCount,\n" +
                    "  t.longsCount,\n" +
                    "  t.datesCount,\n" +
                    "  t.referencesCount,\n" +
                    "  t.polysCount,\n" +
                    "  t.booleansCount\n" +
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
    public void test060QueryEnabled() throws Exception {
        Session session = open();
        try {
            /*
             *  ### task: Equal (activation/administrativeStatus, ENABLED)
             *  ==> from RUser u where u.activation.administrativeStatus = com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus.ENABLED
             */

            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-enabled.xml"));

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test070QueryGenericLong() throws Exception {
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

            String real = getInterpretedQuery2(session, GenericObjectType.class,
                    new File(TEST_DIR, "query-and-generic.xml"));

            String expected = "select\n" +
                    "  g.fullObject, g.stringsCount, g.longsCount, g.datesCount, g.referencesCount, g.polysCount, g.booleansCount\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.longs l with ( l.ownerType = :ownerType and l.name = :name )\n" +
                    "where\n" +
                    "  ( g.name.norm = :norm and l.value = :value )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test071QueryGenericLongTwice() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
                    .item(F_NAME).eqPoly("generic object", "generic object").matchingNorm()
                    .and().item(F_EXTENSION, new QName("intType")).ge(100)
                    .and().item(F_EXTENSION, new QName("intType")).lt(200)
                    .and().item(F_EXTENSION, new QName("longType")).eq(335)
                    .build();

            String real = getInterpretedQuery2(session, GenericObjectType.class, query);

            String expected = "select\n" +
                    "  g.fullObject, g.stringsCount, g.longsCount, g.datesCount, g.referencesCount, g.polysCount, g.booleansCount\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.longs l with ( l.ownerType = :ownerType and l.name = :name )\n" +
                    "    left join g.longs l2 with ( l2.ownerType = :ownerType2 and l2.name = :name2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    g.name.norm = :norm and\n" +
                    "    l.value >= :value and\n" +
                    "    l.value < :value2 and\n" +
                    "    l2.value = :value3\n" +
                    "  )";

            // note l and l2 cannot be merged as they point to different extension properties (intType, longType)
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test072QueryAccountByAttribute() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attribute.xml"));
            String expected = "select\n" +
                    "  s.fullObject, s.stringsCount, s.longsCount, s.datesCount, s.referencesCount, s.polysCount, s.booleansCount\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.name = :name )\n" +
                    "where\n" +
                    "  s2.value = :value\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test074QueryAccountByAttributeAndExtensionValue() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attribute-and-extension-value.xml"));
            String expected = "select\n" +
                    "  s.fullObject, s.stringsCount, s.longsCount, s.datesCount, s.referencesCount, s.polysCount, s.booleansCount\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.name = :name )\n" +
                    "    left join s.longs l with ( l.ownerType = :ownerType2 and l.name = :name2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    s2.value = :value and\n" +
                    "    l.value = :value2\n" +
                    "  )";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test076QueryOrComposite() throws Exception {
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
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-or-composite.xml"));

            String expected = "select\n" +
                    "  s.fullObject, s.stringsCount, s.longsCount, s.datesCount, s.referencesCount, s.polysCount, s.booleansCount\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.name = :name )\n" +
                    "    left join s.strings s3 with ( s3.ownerType = :ownerType2 and s3.name = :name2 )\n" +
                    "where\n" +
                    "  (\n" +
                    "    s.intent = :intent or\n" +
                    "    s2.value = :value or\n" +
                    "    s3.value = :value2 or\n" +
                    "    (\n" +
                    "      s.resourceRef.targetOid = :targetOid and\n" +
                    "      s.resourceRef.relation = :relation and\n" +
                    "      s.resourceRef.type = :type\n" +
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
                relation = #
                type = com.evolveum.midpoint.repo.sql.data.common.other.RObjectType.RESOURCE
             */
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test080QueryExistsAssignment() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: Exists (assignment, Equal (activation/administrativeStatus = Enabled))
             */
            ExistsFilter filter = ExistsFilter.createExists(new ItemPath(F_ASSIGNMENT), UserType.class, prismContext,
                    EqualFilter.createEqual(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                            AssignmentType.class, prismContext, null, ActivationStatusType.ENABLED));
            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);
            query0.setPaging(ObjectPaging.createPaging(F_NAME, ASCENDING));

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .exists(F_ASSIGNMENT)
                        .item(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                            .eq(ActivationStatusType.ENABLED)
                    .asc(F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  a.activation.administrativeStatus = :administrativeStatus\n" +
                    "order by u.name.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test100QueryObjectByName() throws Exception {
        Session session = open();

        try {
            /*
             * ### object: Equal (name, "cpt. Jack Sparrow")
             *             Order by name, ASC
             *
             * ==> from RObject o where name.orig = 'cpt. Jack Sparrow' and name.norm = 'cpt jack sparrow'
             *        order by name.orig asc
             */
            EqualFilter filter = EqualFilter.createEqual(F_NAME, ObjectType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            query.setPaging(ObjectPaging.createPaging(null, null, F_NAME, ASCENDING));

            String real = getInterpretedQuery2(session, ObjectType.class, query);
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
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
    public void test110QueryUserByFullName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-fullName.xml"));
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test112QueryUserSubstringFullName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-substring-fullName.xml"));
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test114QueryUserByName() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-name.xml"));
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  u.name.norm = :norm";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test120QueryConnectorByType() throws Exception {
        Session session = open();

        try {
            String real = getInterpretedQuery2(session, ConnectorType.class,
                    new File(TEST_DIR, "query-connector-by-type.xml"));
            String expected = "select\n" +
                    "  c.fullObject,\n" +
                    "  c.stringsCount,\n" +
                    "  c.longsCount,\n" +
                    "  c.datesCount,\n" +
                    "  c.referencesCount,\n" +
                    "  c.polysCount,\n" +
                    "  c.booleansCount\n" +
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
    public void test130QueryAccountByAttributesAndResourceRef() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attributes-and-resource-ref.xml"));
            String expected = "select\n" +
                    "  s.fullObject,\n" +
                    "  s.stringsCount,\n" +
                    "  s.longsCount,\n" +
                    "  s.datesCount,\n" +
                    "  s.referencesCount,\n" +
                    "  s.polysCount,\n" +
                    "  s.booleansCount\n" +
                    "from\n" +
                    "  RShadow s\n" +
                    "    left join s.strings s2 with ( s2.ownerType = :ownerType and s2.name = :name )\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      s.resourceRef.targetOid = :targetOid and\n" +
                    "      s.resourceRef.relation = :relation and\n" +
                    "      s.resourceRef.type = :type\n" +
                    "    ) and\n" +
                    "    s2.value = :value\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test140QueryUserAccountRef() throws Exception {
        Session session = open();
        try {
            /*
             * ### user: Ref (linkRef, 123)
             *
             * ==> select from RUser u left join u.linkRef l where
             *        l.targetOid = '123' and l.relation = '#'
             */
            RefFilter filter = RefFilter.createReferenceEqual(UserType.F_LINK_REF, UserType.class, prismContext, "123");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

//            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
//                    .item(UserType.F_LINK_REF).ref("123")
//                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.linkRef l\n" +
                    "where\n" +
                    "  (\n" +
                    "    l.targetOid = :targetOid and\n" +
                    "    l.relation = :relation\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test150QueryUserAssignmentTargetRef() throws Exception {
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
            RefFilter filter = RefFilter.createReferenceEqual(
                    new ItemPath(F_ASSIGNMENT, AssignmentType.F_TARGET_REF),
                    UserType.class, prismContext, ort.asReferenceValue());
            String real = getInterpretedQuery2(session, UserType.class, ObjectQuery.createObjectQuery(filter));
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
        } finally {
            close(session);
        }
    }


    @Test
    public void test160QueryTrigger() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
            ItemPath triggerPath = new ItemPath(ObjectType.F_TRIGGER, F_TIMESTAMP);
            ObjectFilter filter = LessFilter.createLess(triggerPath, objectDef, thisScanTimestamp, true);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            String expected = "select\n" +
                    "  o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount\n" +
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
    public void test162QueryTriggerBeforeAfter() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar lastScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                    .exists(ObjectType.F_TRIGGER)
                    .block()
                        .item(F_TIMESTAMP).gt(lastScanTimestamp)
                        .and().item(F_TIMESTAMP).le(thisScanTimestamp)
                    .endBlock()
                    .build();
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            String expected = "select\n" +
                    "  o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount\n" +
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
    public void test170QueryAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
            ItemPath activationPath = new ItemPath(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            ObjectFilter filter = EqualFilter.createEqual(activationPath, objectDef, ActivationStatusType.ENABLED);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test180QueryInducementActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            /*
             * ### role: Equal (inducement/activation/administrativeStatus, ENABLED)
             *
             * ==> select from RRole r left join r.assignments a where
             *          a.assignmentOwner = RAssignmentOwner.ABSTRACT_ROLE and                  <--- this differentiates inducements from assignments
             *          a.activation.administrativeStatus = RActivationStatus.ENABLED
             */
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(RoleType.class);
            ItemPath activationPath = new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            ObjectFilter filter = EqualFilter.createEqual(activationPath, objectDef, ActivationStatusType.ENABLED);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, RoleType.class, query);

            String expected = "select\n" +
                    "  r.fullObject,\n" +
                    "  r.stringsCount,\n" +
                    "  r.longsCount,\n" +
                    "  r.datesCount,\n" +
                    "  r.referencesCount,\n" +
                    "  r.polysCount,\n" +
                    "  r.booleansCount\n" +
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
    public void test182QueryInducementAndAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            /*
             * ### Role: Or (Equal (assignment/activation/administrativeStatus, RActivationStatus.ENABLED),
             *               Equal (inducement/activation/administrativeStatus, RActivationStatus.ENABLED))
             */
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(RoleType.class);

            //filter1
            ItemPath activationPath1 = new ItemPath(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            ObjectFilter filter1 = EqualFilter.createEqual(activationPath1, objectDef, ActivationStatusType.ENABLED);

            //filter2
            ItemPath activationPath2 = new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            ObjectFilter filter2 = EqualFilter.createEqual(activationPath2, objectDef, ActivationStatusType.ENABLED);

            ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(filter1, filter2));
            String real = getInterpretedQuery2(session, RoleType.class, query);

            String expected = "select\n" +
                    "  r.fullObject,\n" +
                    "  r.stringsCount,\n" +
                    "  r.longsCount,\n" +
                    "  r.datesCount,\n" +
                    "  r.referencesCount,\n" +
                    "  r.polysCount,\n" +
                    "  r.booleansCount\n" +
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
    public void test190QueryUserByActivationDouble() throws Exception {
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
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
            ObjectFilter filter1 = EqualFilter.createEqual(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), objectDef,
                    ActivationStatusType.ENABLED);
            ObjectFilter filter2 = EqualFilter.createEqual(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM), objectDef,
                    XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime()));

            ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filter1, filter2));
            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
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
    public void test200QueryTriggerTimestampDoubleWrong() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
            ItemPath triggerPath = new ItemPath(ObjectType.F_TRIGGER, F_TIMESTAMP);
            ObjectFilter greater = GreaterFilter.createGreater(triggerPath, objectDef, thisScanTimestamp, false);
            ObjectFilter lesser = LessFilter.createLess(triggerPath, objectDef, thisScanTimestamp, false);
            AndFilter and = AndFilter.createAnd(greater, lesser);
            LOGGER.info(and.debugDump());

            ObjectQuery query = ObjectQuery.createObjectQuery(and);
            String real = getInterpretedQuery2(session, ObjectType.class, query);

            // correct translation but the filter is wrong: we need to point to THE SAME timestamp -> i.e. ForValue should be used here
            String expected = "select\n" +
                    "  o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount\n" +
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
    public void test300CountObjectOrderByName() throws Exception {
        Session session = open();

        try {
            EqualFilter filter = EqualFilter.createEqual(F_NAME, UserType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));

            ObjectQuery query0 = ObjectQuery.createObjectQuery(filter,
                    ObjectPaging.createPaging(F_NAME, ASCENDING));

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_NAME).eqPoly("cpt. Jack Sparrow", "cpt jack sparrow")
                    .asc(F_NAME).build();

            String real = getInterpretedQuery2(session, UserType.class, query, true);
            String expected = "select\n" +
                    "  count(*)\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.name.orig = :orig and\n" +
                    "    u.name.norm = :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test310CountObjectOrderByNameWithoutFilter() throws Exception {
        Session session = open();

        try {
            ObjectPaging paging = ObjectPaging.createPaging(null, null, F_NAME, ASCENDING);
            ObjectQuery query = ObjectQuery.createObjectQuery(null, paging);

            String real = getInterpretedQuery2(session, ObjectType.class, query, true);
            String expected = "select\n" +
                    "  count(*)\n" +
                    "from\n" +
                    "  RObject o\n";     // ordering does not make sense here
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    /**
     * Q{AND: (EQUALS: parent, PPV(null)),PAGING: O: 0,M: 5,BY: name, D:ASCENDING,
     *
     * @throws Exception
     */
    @Test
    public void test320CountTaskOrderByName() throws Exception {
        Session session = open();

        try {
            EqualFilter filter = EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, prismContext, null);

            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            query.setPaging(ObjectPaging.createPaging(null, null, F_NAME, ASCENDING));

            String real = getInterpretedQuery2(session, TaskType.class, query, true);
            String expected = "select\n" +
                    "  count(*)\n" +
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
    public void test330InOidTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                    .id("1", "2").build();

            String real = getInterpretedQuery2(session, ObjectType.class, query, false);
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.oid in :oid\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test335OwnerInOidTest() throws Exception {
        Session session = open();
        try {
            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .ownerId("1", "2").build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.fullObject\n" +
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
    public void test340QueryOrgTreeFindOrgs() throws Exception {
        Session session = open();

        try {
            OrgFilter orgFilter = OrgFilter.createOrg("some oid", OrgFilter.Scope.ONE_LEVEL);
            ObjectQuery query0 = ObjectQuery.createObjectQuery(orgFilter);
            query0.setPaging(ObjectPaging.createPaging(null, null, F_NAME, ASCENDING));
            query0.setUseNewQueryInterpreter(true);

            ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
                    .isDirectChildOf("some oid")
                    .asc(ObjectType.F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner.OBJECT_PARENT_ORG and ref.targetOid = :orgOid)\n" +
                    "order by o.name.orig asc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test345QueryOrgAllLevels() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
                    .isChildOf(new PrismReferenceValue("123"))
                    .asc(ObjectType.F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select ref.ownerOid from RObjectReference ref where ref.referenceType = com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner.OBJECT_PARENT_ORG and ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "order by o.name.orig asc";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test348QueryRoots() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
                    .isRoot()
                    .asc(ObjectType.F_NAME)
                    .build();

            String real = getInterpretedQuery2(session, OrgType.class, query);

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, query, null, result);

            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  ROrg o\n" +
                    "where\n" +
                    "  o.oid in (select descendantOid from ROrgClosure group by descendantOid having count(descendantOid) = 1)\n" +
                    "order by o.name.orig asc";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test
//    public void asdf() throws Exception {
//        Session session = open();
//        try {
//            Criteria main = session.createCriteria(RUser.class, "u");
//            Criteria a = main.createCriteria("assignments", "a");
//            a.add(Restrictions.eq("a.assignmentOwner", RAssignmentOwner.FOCUS));
//            Criteria e = a.createCriteria("a.extension");
//
//            Criteria s = e.createCriteria("strings", "s");
//
//            Conjunction c2 = Restrictions.conjunction();
//            c2.add(Restrictions.eq("s.extensionType", RAssignmentExtensionType.EXTENSION));
//            c2.add(Restrictions.eq("s.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
//            c2.add(Restrictions.eq("s.value", "uid=jbond,ou=People,dc=example,dc=com"));
//
//            Conjunction c1 = Restrictions.conjunction();
//            c1.add(Restrictions.eq("a.targetRef.targetOid", "1234"));
//            c1.add(Restrictions.eq("a.targetRef.type", RObjectType.ORG));
//
//            main.add(Restrictions.and(c1, c2));
//
//            main.setProjection(Projections.property("u.fullObject"));
//
//            String expected = HibernateToSqlTranslator.toSql(main);
//            LOGGER.info(">>> >>> {}", expected);
//        } finally {
//            close(session);
//        }
//    }

    @Test
    public void test400ActivationQueryWrong() throws Exception {
        PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        OrFilter filter = OrFilter.createOr(
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                        thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                        thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                        focusObjectDef, thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO),
                        focusObjectDef, thisScanTimestamp, true)
        );
        ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

        ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
                .item(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM).le(thisScanTimestamp)
                .or().item(F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).le(thisScanTimestamp)
                .build();

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test405ActivationQueryCorrect() throws Exception {
        PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<AssignmentType> assignmentDef = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class);

        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        OrFilter filter = OrFilter.createOr(
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                        thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                        thisScanTimestamp, true),
                ExistsFilter.createExists(new ItemPath(F_ASSIGNMENT), focusObjectDef,
                        OrFilter.createOr(
                                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                        assignmentDef, thisScanTimestamp, true),
                                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                        assignmentDef, thisScanTimestamp, true))));
        ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

        ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
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
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test410ActivationQueryWrong() throws Exception {
        PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        XMLGregorianCalendar lastScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        OrFilter filter = OrFilter.createOr(
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                                lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                                thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                                lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                                thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                focusObjectDef, lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                focusObjectDef, thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                focusObjectDef, lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                focusObjectDef, thisScanTimestamp, true)
                )
        );
        ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

        ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
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

        //QueryBuilder.queryFor(UserType.class, prismContext);

        Session session = open();
        try {
            String real = getInterpretedQuery2(session, UserType.class, query, false);

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test415ActivationQueryCorrect() throws Exception {
        PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<AssignmentType> assignmentDef = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class);

        XMLGregorianCalendar lastScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());
        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        OrFilter filter = OrFilter.createOr(
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                                lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                                thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                                lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                                thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        ExistsFilter.createExists(new ItemPath(F_ASSIGNMENT), focusObjectDef,
                                OrFilter.createOr(
                                        AndFilter.createAnd(
                                                GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                                        assignmentDef, lastScanTimestamp, false),
                                                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                                        assignmentDef, thisScanTimestamp, true)
                                        ),
                                        AndFilter.createAnd(
                                                GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                                        assignmentDef, lastScanTimestamp, false),
                                                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                                        assignmentDef, thisScanTimestamp, true))))
                )
        );
        ObjectQuery query0 = ObjectQuery.createObjectQuery(filter);

        ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
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
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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
    public void test500OrgQuery() throws Exception {
        File objects = new File("src/test/resources/orgstruct/org-monkey-island.xml");
        OperationResult opResult = new OperationResult("test500OrgQuery");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parseObjects(objects);

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }
        opResult.computeStatusIfUnknown();
        AssertJUnit.assertTrue(opResult.isSuccess());

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
    }

    private <T extends ObjectType> void checkQueryResult(Class<T> type, String oid, OrgFilter.Scope scope, int count)
            throws Exception {
        LOGGER.info("checkQueryResult");

        OrgFilter orgFilter = OrgFilter.createOrg(oid, scope);
        ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);
        query.setPaging(ObjectPaging.createPaging(null, null, F_NAME, ASCENDING));
        query.setUseNewQueryInterpreter(true);

        OperationResult result = new OperationResult("checkQueryResult");
        List<PrismObject<T>> objects = repositoryService.searchObjects(type, query, null, result);
        for (PrismObject object : objects) {
            LOGGER.info("{}", object.getOid());
        }
        int realCount = objects.size();
        AssertJUnit.assertEquals("Expected count doesn't match for searchObjects " + orgFilter, count, realCount);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        realCount = repositoryService.countObjects(type, query, result);
        AssertJUnit.assertEquals("Expected count doesn't match for countObjects " + orgFilter, count, realCount);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test510QueryNameAndOrg() throws Exception {
        Session session = open();

        try {
            EqualFilter eqFilter = EqualFilter.createEqual(F_NAME, ObjectType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));

            OrgFilter orgFilter = OrgFilter.createOrg("12341234-1234-1234-1234-123412341234");

            ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(eqFilter, orgFilter));
            query.setPaging(ObjectPaging.createPaging(null, null, F_NAME, ASCENDING));

            String real = getInterpretedQuery2(session, UserType.class, query);

            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      u.name.orig = :orig and\n" +
                    "      u.name.norm = :norm\n" +
                    "    ) and\n" +
                    "    u.oid in (select ref.ownerOid from RObjectReference ref " +
                    "               where ref.referenceType = com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner.OBJECT_PARENT_ORG and " +
                    "               ref.targetOid in (select descendantOid from ROrgClosure where ancestorOid = :orgOid))\n" +
                    "  )\n" +
                    "order by u.name.orig asc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test
//    public void test520QueryEmployeeTypeAndOrgType() throws Exception {
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
    public void test530queryUserSubstringName() throws Exception {
        Session session = open();

        try {
            SubstringFilter substring = SubstringFilter.createSubstring(F_NAME, ObjectType.class,
                    prismContext, PolyStringOrigMatchingRule.NAME, "a");
            substring.setAnchorStart(true);
            ObjectQuery objectQuery = ObjectQuery.createObjectQuery(substring);
            objectQuery.setUseNewQueryInterpreter(true);

            String real = getInterpretedQuery2(session, ObjectType.class, objectQuery);
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.name.orig like :orig\n";
            assertEqualsIgnoreWhitespace(expected, real);

            OperationResult result = new OperationResult("test530queryUserSubstringName");
            int count = repositoryService.countObjects(ObjectType.class, objectQuery, result);
            AssertJUnit.assertEquals(2, count);

            substring = SubstringFilter.createSubstring(F_NAME, ObjectType.class,
                    prismContext, PolyStringOrigMatchingRule.NAME, "a");
            objectQuery = ObjectQuery.createObjectQuery(substring);
            objectQuery.setUseNewQueryInterpreter(true);
            count = repositoryService.countObjects(ObjectType.class, objectQuery, result);
            AssertJUnit.assertEquals(19, count);

        } finally {
            close(session);
        }
    }

    @Test
    public void test540queryObjectClassTypeUser() throws Exception {
        Session session = open();

        try {
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, null);
            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
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
    public void test550queryObjectClassTypeAbstractRole() throws Exception {
        Session session = open();

        try {
            TypeFilter type = TypeFilter.createType(AbstractRoleType.COMPLEX_TYPE, null);
            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  o.objectTypeClass in :objectTypeClass";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test560queryMetadataTimestamp() throws Exception {
        Session session = open();

        try {
            XMLGregorianCalendar timeXml = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

            LessFilter less = LessFilter.createLess(
                    new ItemPath(ReportOutputType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                    ReportOutputType.class, prismContext, timeXml, true);

            String real = getInterpretedQuery2(session, ReportOutputType.class, ObjectQuery.createObjectQuery(less));
            String expected = "select\n" +
                    "  r.fullObject,\n" +
                    "  r.stringsCount,\n" +
                    "  r.longsCount,\n" +
                    "  r.datesCount,\n" +
                    "  r.referencesCount,\n" +
                    "  r.polysCount,\n" +
                    "  r.booleansCount\n" +
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
    public void test570queryObjectypeByTypeUserAndLocality() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(new ItemPath(UserType.F_LOCALITY), UserType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +                       // TODO - why not RUser here? we unnecessarily join all of RObject subtypes...
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      o.localityUser.orig = :orig and\n" +
                    "      o.localityUser.norm = :norm\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
            checkQueryTypeAlias(hqlToSql(real), "m_user", "locality_orig", "locality_norm");
        } finally {
            close(session);
        }
    }

    /**
     * This checks aliases, if they were generated correctly for query. Alias for table as "table" parameter
     * must be used for columns in "properties" parameter.
     *
     * @param query
     * @param table
     * @param properties
     */
    private void checkQueryTypeAlias(String query, String table, String... properties) {
        LOGGER.info("SQL generated = {}", query);

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
                    AssertJUnit.fail("Property '" + property + "' doesn't have proper alias '"
                            + alias + "' in token '" + token + "'");
                }
            }
        }
    }

    @Test
    public void test575QueryObjectypeByTypeOrgAndLocality() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(new ItemPath(OrgType.F_LOCALITY), OrgType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            TypeFilter type = TypeFilter.createType(OrgType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      o.locality.orig = :orig and\n" +
                    "      o.locality.norm = :norm\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
            checkQueryTypeAlias(hqlToSql(real), "m_org", "locality_orig", "locality_norm");
        } finally {
            close(session);
        }
    }

    @Test
    public void test580QueryObjectypeByTypeAndExtensionAttribute() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "weapon")),
                    UserType.class, prismContext, "some weapon name");
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.strings s with ( s.ownerType = :ownerType and s.name = :name )\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    s.value = :value\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);

        } finally {
            close(session);
        }
    }

    @Test
    public void test590QueryObjectypeByTypeAndReference() throws Exception {
        Session session = open();
        try {
            RefFilter ref = RefFilter.createReferenceEqual(UserType.F_LINK_REF, UserType.class, prismContext, "123");
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, ref);

            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(type));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.linkRef l\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.objectTypeClass = :objectTypeClass and\n" +
                    "    (\n" +
                    "      l.targetOid = :targetOid and\n" +
                    "      l.relation = :relation\n" +
                    "    )\n" +
                    "  )\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test600QueryObjectypeByTypeComplex() throws Exception {
        Session session = open();
        try {
//            Criteria main = session.createCriteria(RObject.class, "o");
//            ProjectionList projections = Projections.projectionList();
//            addFullObjectProjectionList("o", projections, false);
//            main.setProjection(projections);
//
//            Conjunction c1 = Restrictions.conjunction();
//            c1.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));
//            Criterion e1 = Restrictions.and(Restrictions.eq("o.localityUser.orig", "Caribbean"),
//                    Restrictions.eq("o.localityUser.norm", "caribbean"));
//            Criterion e2 = Restrictions.and(Restrictions.eq("o.localityUser.orig", "Adriatic"),
//                    Restrictions.eq("o.localityUser.norm", "adriatic"));
//            c1.add(Restrictions.or(e1, e2));
//
//            Conjunction c2 = Restrictions.conjunction();
//            c2.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.ORG));
//            Criteria o1 = main.createCriteria("o.orgType", "o1", JoinType.LEFT_OUTER_JOIN);
//            c2.add(Restrictions.eq("o1.elements", "functional"));
//
//            Criterion c3 = Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.REPORT);
//
//            main.add(Restrictions.or(c1, c2, c3));
//            String expected = HibernateToSqlTranslator.toSql(main);


            EqualFilter eq1 = EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            EqualFilter eq2 = EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, prismContext,
                    new PolyString("Adriatic", "adriatic"));
            TypeFilter type1 = TypeFilter.createType(UserType.COMPLEX_TYPE, OrFilter.createOr(eq1, eq2));

            EqualFilter equal = EqualFilter.createEqual(OrgType.F_ORG_TYPE, OrgType.class, prismContext, "functional");
            TypeFilter type2 = TypeFilter.createType(OrgType.COMPLEX_TYPE, equal);

            TypeFilter type3 = TypeFilter.createType(ReportType.COMPLEX_TYPE, null);

            OrFilter or = OrFilter.createOr(type1, type2, type3);

            String real = getInterpretedQuery2(session, ObjectType.class, ObjectQuery.createObjectQuery(or));
            String expected = "select\n" +
                    "  o.fullObject,\n" +
                    "  o.stringsCount,\n" +
                    "  o.longsCount,\n" +
                    "  o.datesCount,\n" +
                    "  o.referencesCount,\n" +
                    "  o.polysCount,\n" +
                    "  o.booleansCount\n" +
                    "from\n" +
                    "  RObject o\n" +
                    "    left join o.orgType o2\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      o.objectTypeClass = :objectTypeClass and\n" +
                    "      (\n" +
                    "        (\n" +
                    "          o.localityUser.orig = :orig and\n" +
                    "          o.localityUser.norm = :norm\n" +
                    "        ) or\n" +
                    "        (\n" +
                    "          o.localityUser.orig = :orig2 and\n" +
                    "          o.localityUser.norm = :norm2\n" +
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

    @Test(expectedExceptions = QueryException.class)
    public void test610QueryGenericClob() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "locations")),
                    GenericObjectType.class, prismContext, null);

            getInterpretedQuery2(session, GenericObjectType.class, ObjectQuery.createObjectQuery(eq));
        } catch (QueryException ex) {
            LOGGER.info("Exception", ex);
            throw ex;
        } finally {
            close(session);
        }
    }

    @Test
    public void test620QueryGenericString() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "stringType")),
                    GenericObjectType.class, prismContext, "asdf");

            String real = getInterpretedQuery2(session, GenericObjectType.class, ObjectQuery.createObjectQuery(eq));
            String expected = "select\n" +
                    "  g.fullObject,\n" +
                    "  g.stringsCount,\n" +
                    "  g.longsCount,\n" +
                    "  g.datesCount,\n" +
                    "  g.referencesCount,\n" +
                    "  g.polysCount,\n" +
                    "  g.booleansCount\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.strings s with ( s.ownerType = :ownerType and s.name = :name )\n" +
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
//            EqualFilter eq = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_UID),
//                    new PrismPropertyDefinition(SchemaConstantsGenerated.ICF_S_UID, DOMUtil.XSD_STRING, prismContext),
//                    "8daaeeae-f0c7-41c9-b258-2a3351aa8876");
//            secondaryEquals.add(eq);
//            eq = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_NAME),
//                    new PrismPropertyDefinition(SchemaConstantsGenerated.ICF_S_NAME, DOMUtil.XSD_STRING, prismContext),
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
    public void test630QueryGenericBoolean() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, SKIP_AUTOGENERATION),
                    GenericObjectType.class, prismContext, true);

            ObjectQuery objectQuery = ObjectQuery.createObjectQuery(eq);
            objectQuery.setUseNewQueryInterpreter(true);

            String real = getInterpretedQuery2(session, GenericObjectType.class, objectQuery);
            String expected = "select\n" +
                    "  g.fullObject,\n" +
                    "  g.stringsCount,\n" +
                    "  g.longsCount,\n" +
                    "  g.datesCount,\n" +
                    "  g.referencesCount,\n" +
                    "  g.polysCount,\n" +
                    "  g.booleansCount\n" +
                    "from\n" +
                    "  RGenericObject g\n" +
                    "    left join g.booleans b with ( b.ownerType = :ownerType and b.name = :name )\n" +
                    "where\n" +
                    "  b.value = :value\n";

            assertEqualsIgnoreWhitespace(expected, real);

            OperationResult result = new OperationResult("search");
            List<PrismObject<GenericObjectType>> objects = repositoryService.searchObjects(GenericObjectType.class,
                    objectQuery, null, result);
            result.computeStatus();
            AssertJUnit.assertTrue(result.isSuccess());

            AssertJUnit.assertNotNull(objects);
            AssertJUnit.assertEquals(1, objects.size());

            PrismObject<GenericObjectType> obj = objects.get(0);
            AssertJUnit.assertTrue(obj.getCompileTimeClass().equals(GenericObjectType.class));

            result = new OperationResult("count");
            long count = repositoryService.countObjects(GenericObjectType.class, objectQuery,
                    result);
            result.computeStatus();
            AssertJUnit.assertTrue(result.isSuccess());
            AssertJUnit.assertEquals(1, count);
        } finally {
            close(session);
        }
    }

    @Test
    public void test640queryAssignmentExtensionBoolean() throws Exception {
        Session session = open();
        try {
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition userDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
            PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(F_ASSIGNMENT);
            PrismPropertyDefinition propDef = assignmentDef.createPropertyDefinition(SKIP_AUTOGENERATION, DOMUtil.XSD_BOOLEAN);

            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(F_ASSIGNMENT, AssignmentType.F_EXTENSION, SKIP_AUTOGENERATION),
                    propDef, null, true);

            ObjectQuery objectQuery = ObjectQuery.createObjectQuery(eq);
            objectQuery.setUseNewQueryInterpreter(true);

            String real = getInterpretedQuery2(session, UserType.class, objectQuery);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "    left join a.extension e\n" +
                    "    left join e.booleans b with b.name = :name\n" +
                    "where\n" +
                    "  b.value = :value";
            assertEqualsIgnoreWhitespace(expected, real);

            OperationResult result = new OperationResult("search");
            List<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class,
                    objectQuery, null, result);
            result.computeStatus();
            AssertJUnit.assertTrue(result.isSuccess());

            AssertJUnit.assertNotNull(objects);
            AssertJUnit.assertEquals(1, objects.size());

            PrismObject<UserType> obj = objects.get(0);
            AssertJUnit.assertTrue(obj.getCompileTimeClass().equals(UserType.class));

            result = new OperationResult("count");
            long count = repositoryService.countObjects(UserType.class, objectQuery, result);
            result.computeStatus();
            AssertJUnit.assertTrue(result.isSuccess());
            AssertJUnit.assertEquals(1, count);
        } finally {
            close(session);
        }
    }

    @Test
    public void test700QueryCertCaseAll() throws Exception {
        Session session = open();
        try {
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, (ObjectQuery) null, false);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test710QueryCertCaseOwner() throws Exception {
        Session session = open();
        try {
            InOidFilter filter = InOidFilter.createOwnerHasOidIn("123456");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.fullObject\n" +
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
    public void test720QueryCertCaseOwnerAndTarget() throws Exception {
        Session session = open();
        try {
            PrismContainerDefinition<AccessCertificationCaseType> caseDef =
                    prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AccessCertificationCaseType.class);
            AndFilter filter = AndFilter.createAnd(
                    InOidFilter.createOwnerHasOidIn("123456"),
                    RefFilter.createReferenceEqual(new ItemPath(AccessCertificationCaseType.F_TARGET_REF), caseDef, "1234567890"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.ownerOid in :ownerOid and\n" +
                    "    (\n" +
                    "      a.targetRef.targetOid = :targetOid and\n" +
                    "      a.targetRef.relation = :relation\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test730QueryCertCaseReviewer() throws Exception {
        Session session = open();
        try {
            PrismContainerDefinition<AccessCertificationCaseType> caseDef =
                    prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AccessCertificationCaseType.class);
            ObjectFilter filter = RefFilter.createReferenceEqual(new ItemPath(F_REVIEWER_REF), caseDef, "1234567890");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.reviewerRef r\n" +
                    "where\n" +
                    "  (\n" +
                    "    r.targetOid = :targetOid and\n" +
                    "    r.relation = :relation\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }


    @Test
    public void test735QueryCertCaseReviewerAndEnabled() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue reviewerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .item(F_REVIEWER_REF).ref(reviewerRef)
                    .and().item(F_CURRENT_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected =
                    "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.reviewerRef r\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      r.targetOid = :targetOid and\n" +
                    "      r.relation = :relation and\n" +
                    "      r.type = :type\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      a.currentStageNumber = o.stageNumber or\n" +
                    "      (\n" +
                    "        a.currentStageNumber is null and\n" +
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
    public void test745QueryCertCaseReviewerAndEnabledByDeadlineAndOidAsc() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue reviewerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();

            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .item(F_REVIEWER_REF).ref(reviewerRef)
                    .and().item(F_CURRENT_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .asc(F_REVIEW_DEADLINE).asc(T_ID)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);
            String expected =
                    "select\n" +
                            "  a.fullObject\n" +
                            "from\n" +
                            "  RAccessCertificationCase a\n" +
                            "    left join a.reviewerRef r\n" +
                            "    left join a.owner o\n" +
                            "where\n" +
                            "  (\n" +
                            "    (\n" +
                            "      r.targetOid = :targetOid and\n" +
                            "      r.relation = :relation and\n" +
                            "      r.type = :type\n" +
                            "    ) and\n" +
                            "    (\n" +
                            "      a.currentStageNumber = o.stageNumber or\n" +
                            "      (\n" +
                            "        a.currentStageNumber is null and\n" +
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
    public void test747QueryCertCaseReviewerAndEnabledByRequestedDesc() throws Exception {
        Session session = open();
        try {
            PrismReferenceValue reviewerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
            ItemPath statePath = new ItemPath(T_PARENT, F_STATE);
            PrismPropertyDefinition stateDef =
                    prismContext.getSchemaRegistry()
                            .findComplexTypeDefinitionByCompileTimeClass(AccessCertificationCampaignType.class)
                            .findPropertyDefinition(F_STATE);
            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .item(F_REVIEWER_REF).ref(reviewerRef)
                    .and().item(F_CURRENT_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                    .and().item(statePath, stateDef).eq(IN_REVIEW_STAGE)
                    .desc(F_REVIEW_REQUESTED_TIMESTAMP)
                    .build();
            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query, false);

            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.reviewerRef r\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      r.targetOid = :targetOid and\n" +
                    "      r.relation = :relation and\n" +
                    "      r.type = :type\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      a.currentStageNumber = o.stageNumber or\n" +
                    "      (\n" +
                    "        a.currentStageNumber is null and\n" +
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
    public void test750DereferenceLink() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: linkRef/@/name contains 'test.com'
             */

            ObjectFilter filter = SubstringFilter.createSubstring(
                    new ItemPath(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, F_NAME),
                    UserType.class, prismContext, "test.com");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.linkRef l\n" +
                    "    left join l.target t\n" +
                    "where\n" +
                    "  (\n" +
                    "    t.name.orig like :orig and\n" +
                    "    t.name.norm like :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test752DereferenceLinkedResourceName() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: linkRef/@/resourceRef/@/name contains 'CSV' (norm)
             */

            ObjectFilter filter = SubstringFilter.createSubstring(
                    new ItemPath(UserType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE,
                            ShadowType.F_RESOURCE_REF, PrismConstants.T_OBJECT_REFERENCE,
                            F_NAME),
                    UserType.class, prismContext, PolyStringNormMatchingRule.NAME, "CSV");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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

    @Test(expectedExceptions = IllegalStateException.class)             // at this time
    public void test760DereferenceAssignedRoleType() throws Exception {
        Session session = open();

        try {
            /*
             * This fails, as prism nor query interpreter expect that targetRef is RoleType/RRole.
             * Prism should implement something like "searching for proper root" when dereferencing "@".
             * QI should implement the proper root search not only at the query root, but always after a "@".
             *
             * ### UserType: assignment/targetRef/@/roleType
             */

            ObjectFilter filter = EqualFilter.createEqual(
                    new ItemPath(F_ASSIGNMENT, AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, RoleType.F_ROLE_TYPE),
                    UserType.class, prismContext, "type1");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String real = getInterpretedQuery2(session, UserType.class, query);

        } finally {
            close(session);
        }
    }

    @Test
    public void test770CaseParentFilter() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: Equal(../name, 'Campaign 1')
             */

            ObjectFilter filter = EqualFilter.createEqual(
                    new ItemPath(T_PARENT, F_NAME),
                    AccessCertificationCaseType.class, prismContext, "Campaign 1");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    o.name.orig = :orig and\n" +
                    "    o.name.norm = :norm\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }


    @Test
    public void test800OrderBySingleton() throws Exception {
        Session session = open();

        try {
            /*
             * ### UserType: order by activation/administrativeStatus
             */

            ObjectQuery query = ObjectQuery.createObjectQuery(
                    null,
                    ObjectPaging.createPaging(
                            new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                            ASCENDING));

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "order by u.activation.administrativeStatus asc";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test810OrderByParentCampaignName() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by ../name desc
             */

            ObjectQuery query = ObjectQuery.createObjectQuery(
                    ObjectPaging.createPaging(new ItemPath(T_PARENT, F_NAME), DESCENDING));

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.owner o\n" +
                    "order by o.name.orig desc\n";

            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test820OrderByTargetName() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccessCertificationCaseType: (all), order by targetRef/@/name
             */

            ObjectQuery query = ObjectQuery.createObjectQuery(
                    ObjectPaging.createPaging(new ItemPath(
                            AccessCertificationCaseType.F_TARGET_REF,
                            PrismConstants.T_OBJECT_REFERENCE, F_NAME), ASCENDING));

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.targetRef.target t\n" +
                    "order by t.name.orig asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)          // should fail, as Equals supports single-value right side only
                                                                        // TODO this should be perhaps checked in EqualFilter
    public void test900EqualsMultivalue() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: preferredLanguage = 'SK', 'HU'
             */
            PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
            PrismPropertyDefinition<String> prefLangDef = userDef.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);

            PrismPropertyDefinition<String> multivalDef = new PrismPropertyDefinition<String>(UserType.F_PREFERRED_LANGUAGE,
                    DOMUtil.XSD_STRING, prismContext);
            multivalDef.setMaxOccurs(-1);
            PrismProperty<String> multivalProperty = multivalDef.instantiate();
            multivalProperty.addRealValue("SK");
            multivalProperty.addRealValue("HU");

            ObjectQuery query = ObjectQuery.createObjectQuery(
                    EqualFilter.createEqual(new ItemPath(UserType.F_PREFERRED_LANGUAGE), multivalProperty));

            String real = getInterpretedQuery2(session, UserType.class, query);
//            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test910PreferredLanguageEqualsCostCenter() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: preferredLanguage = costCenter
             */
            ObjectQuery query = ObjectQuery.createObjectQuery(
                    EqualFilter.createEqual(
                            new ItemPath(UserType.F_PREFERRED_LANGUAGE),
                            UserType.class,
                            prismContext,
                            null,
                            new ItemPath(UserType.F_COST_CENTER)));

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject,\n" +
                    "  u.stringsCount,\n" +
                    "  u.longsCount,\n" +
                    "  u.datesCount,\n" +
                    "  u.referencesCount,\n" +
                    "  u.polysCount,\n" +
                    "  u.booleansCount\n" +
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

    @Test(enabled = false)          // different types of properties that are to be compared (polystring vs string in this case) are not supported yet
    public void test915OrganizationEqualsCostCenter() throws Exception {
        Session session = open();

        try {
            /*
             * ### User: organization = costCenter
             */
            ObjectQuery query = ObjectQuery.createObjectQuery(
                    EqualFilter.createEqual(
                            new ItemPath(UserType.F_ORGANIZATION),
                            UserType.class,
                            prismContext,
                            null,
                            new ItemPath(UserType.F_COST_CENTER)));

            String real = getInterpretedQuery2(session, UserType.class, query);
//            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test920DecisionsNotAnswered() throws Exception {
        Session session = open();

        try {
            /*
             * ### AccCertCase: Exists (decision: reviewerRef = XYZ and stage = ../stage and response is null or response = NO_RESPONSE)
             */
            ObjectQuery query0 = ObjectQuery.createObjectQuery(
                    ExistsFilter.createExists(
                            new ItemPath(F_DECISION),
                            AccessCertificationCaseType.class,
                            prismContext,
                            AndFilter.createAnd(
                                    RefFilter.createReferenceEqual(
                                            AccessCertificationDecisionType.F_REVIEWER_REF,
                                            AccessCertificationCaseType.class,
                                            prismContext,
                                            "123456"),
                                    EqualFilter.createEqual(
                                            new ItemPath(F_STAGE_NUMBER),
                                            AccessCertificationDecisionType.class,
                                            prismContext,
                                            null,
                                            new ItemPath(T_PARENT, F_CURRENT_STAGE_NUMBER)
                                    ),
                                    OrFilter.createOr(
                                            EqualFilter.createEqual(
                                                    AccessCertificationDecisionType.F_RESPONSE,
                                                    AccessCertificationDecisionType.class,
                                                    prismContext, null),
                                            EqualFilter.createEqual(
                                                    AccessCertificationDecisionType.F_RESPONSE,
                                                    AccessCertificationDecisionType.class,
                                                    prismContext, NO_RESPONSE)
                                    )
                            )
                    )
            );
            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .exists(F_DECISION)
                    .block()
                        .item(AccessCertificationDecisionType.F_REVIEWER_REF).ref("123456")
                        .and().item(F_STAGE_NUMBER).eq().item(T_PARENT, F_CURRENT_STAGE_NUMBER)
                        .and().block()
                            .item(F_RESPONSE).isNull()
                            .or().item(F_RESPONSE).eq(NO_RESPONSE)
                        .endBlock()
                    .endBlock()
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.decision d\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      d.reviewerRef.targetOid = :targetOid and\n" +
                    "      d.reviewerRef.relation = :relation\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      d.stageNumber = a.currentStageNumber or\n" +
                    "      (\n" +
                    "        d.stageNumber is null and\n" +
                    "        a.currentStageNumber is null\n" +
                    "      )\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      d.response is null or\n" +
                    "      d.response = :response\n" +
                    "    )\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test925DecisionsNotAnsweredOrderBy() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .exists(F_DECISION)
                    .block()
                        .item(AccessCertificationDecisionType.F_REVIEWER_REF).ref("123456")
                        .and().item(F_STAGE_NUMBER).eq().item(T_PARENT, F_CURRENT_STAGE_NUMBER)
                        .and().block()
                            .item(F_RESPONSE).isNull()
                            .or().item(F_RESPONSE).eq(NO_RESPONSE)
                        .endBlock()
                    .endBlock()
                    .asc(T_PARENT, F_NAME)
                    .asc(T_ID)
                    .asc(T_PARENT, T_ID)
                    .build();

            String real = getInterpretedQuery2(session, AccessCertificationCaseType.class, query);
            String expected = "select\n" +
                    "  a.fullObject\n" +
                    "from\n" +
                    "  RAccessCertificationCase a\n" +
                    "    left join a.decision d\n" +
                    "    left join a.owner o\n" +
                    "where\n" +
                    "  (\n" +
                    "    (\n" +
                    "      d.reviewerRef.targetOid = :targetOid and\n" +
                    "      d.reviewerRef.relation = :relation\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      d.stageNumber = a.currentStageNumber or\n" +
                    "      (\n" +
                    "        d.stageNumber is null and\n" +
                    "        a.currentStageNumber is null\n" +
                    "      )\n" +
                    "    ) and\n" +
                    "    (\n" +
                    "      d.response is null or\n" +
                    "      d.response = :response\n" +
                    "    )\n" +
                    "  )\n" +
                    "order by o.name.orig asc, a.id asc, a.ownerOid asc\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test930ResourceRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_ASSIGNMENT, F_CONSTRUCTION, F_RESOURCE_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.assignments a with a.assignmentOwner = :assignmentOwner\n" +
                    "where\n" +
                    "  (\n" +
                    "    a.resourceRef.targetOid = :targetOid and\n" +
                    "    a.resourceRef.relation = :relation\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test932CreatorRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_METADATA, F_CREATOR_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "where\n" +
                    "  (\n" +
                    "    u.creatorRef.targetOid = :targetOid and\n" +
                    "    u.creatorRef.relation = :relation\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test934CreateApproverRef() throws Exception {
        Session session = open();

        try {
            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(F_METADATA, F_CREATE_APPROVER_REF).ref("1234567")
                    .build();

            String real = getInterpretedQuery2(session, UserType.class, query);
            String expected = "select\n" +
                    "  u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount\n" +
                    "from\n" +
                    "  RUser u\n" +
                    "    left join u.createApproverRef c\n" +
                    "where\n" +
                    "  (\n" +
                    "    c.targetOid = :targetOid and\n" +
                    "    c.relation = :relation\n" +
                    "  )\n";
            assertEqualsIgnoreWhitespace(expected, real);
        } finally {
            close(session);
        }
    }

//    @Test
//    public void test930OrganizationEqualsCostCenter() throws Exception {
//        Session session = open();
//
//        try {
//            /*
//             * ### User: organization = costCenter
//             */
//            ObjectQuery query = ObjectQuery.createObjectQuery(
//                    EqualFilter.createEqual(
//                            new ItemPath(UserType.F_ORGANIZATION),
//                            UserType.class,
//                            prismContext,
//                            null,
//                            new ItemPath(UserType.F_COST_CENTER)));
//
//            String real = getInterpretedQuery2(session, UserType.class, query);
////            assertEqualsIgnoreWhitespace(expected, real);
//        } finally {
//            close(session);
//        }
//    }


    // TODO negative tests - order by entity, reference, any, collection
    // TODO implement checks for "order by" for non-singletons

    protected <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, File file) throws
            Exception {
        return getInterpretedQuery2(session, type, file, false);
    }

    protected <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, File file,
                                                                    boolean interpretCount) throws Exception {

        QueryType queryType = PrismTestUtil.parseAtomicValue(file, QueryType.COMPLEX_TYPE);

        LOGGER.info("QUERY TYPE TO CONVERT : {}", ObjectQueryUtil.dump(queryType));

        ObjectQuery query = null;
        try {
            query = QueryJaxbConvertor.createObjectQuery((Class) type, queryType, prismContext);        // TODO
        } catch (Exception ex) {
            LOGGER.info("error while converting query: " + ex.getMessage(), ex);
        }

        return getInterpretedQuery2(session, type, query, interpretCount);
    }

    protected <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, ObjectQuery query) throws Exception {
        return getInterpretedQuery2(session, type, query, false);
    }


    protected <T extends Containerable> String getInterpretedQuery2(Session session, Class<T> type, ObjectQuery query,
                                                                   boolean interpretCount) throws Exception {

        if (query != null) {
            LOGGER.info("QUERY TYPE TO CONVERT :\n{}", (query.getFilter() != null ? query.getFilter().debugDump(3) : null));
        }

        QueryEngine2 engine = new QueryEngine2(repositoryService.getConfiguration(), prismContext);
        RQuery rQuery = engine.interpret(query, type, null, interpretCount, session);
        //just test if DB will handle it or throws some exception
        if (interpretCount) {
            rQuery.uniqueResult();
        } else {
            rQuery.list();
        }

        return ((RQueryImpl) rQuery).getQuery().getQueryString();
    }

    private void assertEqualsIgnoreWhitespace(String expected, String real) {
        LOGGER.info("exp. query>\n{}\nreal query>\n{}", expected, real);
        String expNorm = StringUtils.normalizeSpace(expected);
        String realNorm = StringUtils.normalizeSpace(real);
        if (!expNorm.equals(realNorm)) {
            String m = "Generated query is not correct. Expected:\n" + expected + "\nActual:\n" + real + "\n\nNormalized versions:\n\n" +
                    "Expected: " + expNorm + "\nActual:   " + realNorm + "\n";
            LOGGER.error("{}", m);
            throw new AssertionError(m);
        }
    }

}
