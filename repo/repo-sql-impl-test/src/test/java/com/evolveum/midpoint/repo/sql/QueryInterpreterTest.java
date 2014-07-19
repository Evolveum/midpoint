/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RTaskExecutionStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryInterpreterTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreterTest.class);
    private static final File TEST_DIR = new File("./src/test/resources/query");

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
    public void queryOrganizationNorm() throws Exception {
        Session session = open();

        try {
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    PolyStringNormMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);


            Criteria o = main.createCriteria("organization", "o", JoinType.LEFT_OUTER_JOIN);

            o.add(Restrictions.eq("o.norm", "asdf"));

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryOrganizationOrig() throws Exception {
        Session session = open();
        try {
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    PolyStringOrigMatchingRule.NAME, new PolyString("asdf", "asdf"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            Criteria o = main.createCriteria("organization", "o", JoinType.LEFT_OUTER_JOIN);

            o.add(Restrictions.eq("o.orig", "asdf"));

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryOrganizationStrict() throws Exception {
        Session session = open();
        try {
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext,
                    null, new PolyString("asdf", "asdf"));
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            Criteria o = main.createCriteria("organization", "o", JoinType.LEFT_OUTER_JOIN);

            o.add(Restrictions.conjunction()
                    .add(Restrictions.eq("o.orig", "asdf"))
                    .add(Restrictions.eq("o.norm", "asdf")));

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryDependent() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RTask.class, "t");
            Criteria d = main.createCriteria("dependent", "d", JoinType.LEFT_OUTER_JOIN);
            d.add(Restrictions.eq("d.elements", "123456"));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("t", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            ObjectFilter filter = EqualFilter.createEqual(TaskType.F_DEPENDENT, TaskType.class, prismContext, null, "123456");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, TaskType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void queryClob() throws Exception {
        Session session = open();

        try {
            ObjectFilter filter = EqualFilter.createEqual(UserType.F_DESCRIPTION, UserType.class, prismContext,
                    null, "aaa");
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            //should throw exception, because description is lob and can't be queried
            getInterpretedQuery(session, UserType.class, query);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryEnum() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RTask.class, "t");
            main.add(Restrictions.eq("executionStatus", RTaskExecutionStatus.WAITING));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("t", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            ObjectFilter filter = EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS, TaskType.class, prismContext,
                    null, TaskExecutionStatusType.WAITING);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, TaskType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryEnabled() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            main.add(Restrictions.eq("activation.administrativeStatus", RActivationStatus.ENABLED));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-enabled.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryGenericLong() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RGenericObject.class, "g");

            Criteria stringExt = main.createCriteria("longs", "l", JoinType.LEFT_OUTER_JOIN);

            //and
            Criterion c1 = Restrictions.eq("name.norm", "generic object");
            //and
            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("l.ownerType", RObjectExtensionType.EXTENSION));
            c2.add(Restrictions.eq("l.name", new QName("http://example.com/p", "intType")));
            c2.add(Restrictions.eq("l.value", 123L));

            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(c1);
            conjunction.add(c2);
            main.add(conjunction);
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("g", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, GenericObjectType.class,
                    new File(TEST_DIR, "query-and-generic.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryOrComposite() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RShadow.class, "r");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("r", projections, false);
            main.setProjection(projections);

            Criteria stringExt = main.createCriteria("strings", "s1", JoinType.LEFT_OUTER_JOIN);

            //or
            Criterion c1 = Restrictions.eq("intent", "some account type");
            //or
            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("s1.ownerType", RObjectExtensionType.ATTRIBUTES));
            c2.add(Restrictions.eq("s1.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
            c2.add(Restrictions.eq("s1.value", "foo value"));
            //or
            Conjunction c3 = Restrictions.conjunction();
            c3.add(Restrictions.eq("s1.ownerType", RObjectExtensionType.EXTENSION));
            c3.add(Restrictions.eq("s1.name", new QName("http://example.com/p", "stringType")));
            c3.add(Restrictions.eq("s1.value", "uid=test,dc=example,dc=com"));
            //or
            Conjunction c4 = Restrictions.conjunction();
            c4.add(Restrictions.eq("r.resourceRef.targetOid", "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e"));
            c4.add(Restrictions.eq("r.resourceRef.type", QNameUtil.qNameToUri(ResourceType.COMPLEX_TYPE)));

            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add(c1);
            disjunction.add(c2);
            disjunction.add(c3);
            disjunction.add(c4);
            main.add(disjunction);

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, ShadowType.class,
                    new File(TEST_DIR, "query-or-composite.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryObjectByName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            main.add(Restrictions.and(
                    Restrictions.eq("name.orig", "cpt. Jack Sparrow"),
                    Restrictions.eq("name.norm", "cpt jack sparrow")));
            main.addOrder(Order.asc("name.orig"));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);
            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter filter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));

            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

            String real = getInterpretedQuery(session, ObjectType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryUserByFullName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            main.add(Restrictions.eq("fullName.norm", "cpt jack sparrow"));
            String expected = HibernateToSqlTranslator.toSql(main);

            String real = getInterpretedQuery(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-fullName.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryUserSubstringFullName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            main.add(Restrictions.like("fullName.norm", "%cpt jack sparrow%").ignoreCase());
            String expected = HibernateToSqlTranslator.toSql(main);

            String real = getInterpretedQuery(session, UserType.class,
                    new File(TEST_DIR, "query-user-substring-fullName.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryUserByName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            main.setProjection(projections);
            addFullObjectProjectionList("u", projections, false);

            main.add(Restrictions.eq("name.norm", "some name identificator"));
            String expected = HibernateToSqlTranslator.toSql(main);

            String real = getInterpretedQuery(session, UserType.class,
                    new File(TEST_DIR, "query-user-by-name.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryConnectorByType() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RConnector.class, "c");
            Criterion connectorType = Restrictions.conjunction().add(
                    Restrictions.eq("connectorType", "org.identityconnectors.ldap.LdapConnector"));
            main.add(connectorType);
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("c", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            String real = getInterpretedQuery(session, ConnectorType.class,
                    new File(TEST_DIR, "query-connector-by-type.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryAccountByAttributesAndResourceRef() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RShadow.class, "r");

            Criteria stringAttr = main.createCriteria("strings", "s1x", JoinType.LEFT_OUTER_JOIN);

            //and
            Conjunction c1 = Restrictions.conjunction();
            c1.add(Restrictions.eq("r.resourceRef.targetOid", "aae7be60-df56-11df-8608-0002a5d5c51b"));
            c1.add(Restrictions.eq("r.resourceRef.type", QNameUtil.qNameToUri(ResourceType.COMPLEX_TYPE)));
            //and
            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("s1x.ownerType", RObjectExtensionType.ATTRIBUTES));
            c2.add(Restrictions.eq("s1x.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
            c2.add(Restrictions.eq("s1x.value", "uid=jbond,ou=People,dc=example,dc=com"));

            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(c1);
            conjunction.add(c2);
            main.add(conjunction);
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("r", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);
            String real = getInterpretedQuery(session, ShadowType.class,
                    new File(TEST_DIR, "query-account-by-attributes-and-resource-ref.xml"));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryUserAccountRef() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            Criteria refs = main.createCriteria("linkRef", "l", JoinType.LEFT_OUTER_JOIN);
            refs.add(Restrictions.conjunction().add(Restrictions.eq("l.targetOid", "123")));

            String expected = HibernateToSqlTranslator.toSql(main);

            RefFilter filter = RefFilter.createReferenceEqual(UserType.F_LINK_REF, UserType.class, prismContext, "123");
            String real = getInterpretedQuery(session, UserType.class, ObjectQuery.createObjectQuery(filter));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryTrigger() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Criteria d = main.createCriteria("trigger", "t", JoinType.LEFT_OUTER_JOIN);
            d.add(Restrictions.le("t.timestamp", new Timestamp(NOW.getTime())));

            String expected = HibernateToSqlTranslator.toSql(main);

            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
            ItemPath triggerPath = new ItemPath(ObjectType.F_TRIGGER, TriggerType.F_TIMESTAMP);
            //        PrismContainerDefinition triggerContainerDef = objectDef.findContainerDefinition(triggerPath);
            ObjectFilter filter = LessFilter.createLess(triggerPath, objectDef, thisScanTimestamp, true);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, ObjectType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            Criteria a = main.createCriteria("assignments", "a", JoinType.LEFT_OUTER_JOIN);
            a.add(Restrictions.and(
                    Restrictions.eq("a.assignmentOwner", RAssignmentOwner.FOCUS),
                    Restrictions.eq("a.activation.administrativeStatus", RActivationStatus.ENABLED)
            ));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
            ItemPath activationPath = new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

            //        PrismContainerDefinition activationDef = objectDef.findContainerDefinition(activationPath);

            ObjectFilter filter = EqualFilter.createEqual(activationPath, objectDef, ActivationStatusType.ENABLED);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryInducementActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RRole.class, "r");
            Criteria a = main.createCriteria("assignments", "a", JoinType.LEFT_OUTER_JOIN);
            a.add(Restrictions.and(
                    Restrictions.eq("a.assignmentOwner", RAssignmentOwner.ABSTRACT_ROLE),
                    Restrictions.eq("a.activation.administrativeStatus", RActivationStatus.ENABLED)
            ));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("r", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(RoleType.class);
            ItemPath activationPath = new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

            //        PrismContainerDefinition activationDef = objectDef.findContainerDefinition(activationPath);

            ObjectFilter filter = EqualFilter.createEqual(activationPath, objectDef, ActivationStatusType.ENABLED);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, RoleType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryInducementAndAssignmentActivationAdministrativeStatus() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RRole.class, "r");
            Criteria a = main.createCriteria("assignments", "a", JoinType.LEFT_OUTER_JOIN);
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("r", projections, false);
            main.setProjection(projections);

            Criterion and1 = Restrictions.and(
                    Restrictions.eq("a.assignmentOwner", RAssignmentOwner.FOCUS),
                    Restrictions.eq("a.activation.administrativeStatus", RActivationStatus.ENABLED)
            );

            Criterion and2 = Restrictions.and(
                    Restrictions.eq("a.assignmentOwner", RAssignmentOwner.ABSTRACT_ROLE),
                    Restrictions.eq("a.activation.administrativeStatus", RActivationStatus.ENABLED)
            );

            a.add(Restrictions.or(and1, and2));

            String expected = HibernateToSqlTranslator.toSql(main);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(RoleType.class);

            //filter1
            ItemPath activationPath1 = new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            //        PrismContainerDefinition activationDef1 = objectDef.findContainerDefinition(activationPath1);
            ObjectFilter filter1 = EqualFilter.createEqual(activationPath1, objectDef, ActivationStatusType.ENABLED);

            //filter2
            ItemPath activationPath2 = new ItemPath(RoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
            //        PrismContainerDefinition activationDef2 = objectDef.findContainerDefinition(activationPath2);
            ObjectFilter filter2 = EqualFilter.createEqual(activationPath2, objectDef, ActivationStatusType.ENABLED);

            ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(filter1, filter2));
            String real = getInterpretedQuery(session, RoleType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryUserByActivationDouble() throws Exception {
        Date NOW = new Date();

        Session session = open();
        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("u", projections, false);
            main.setProjection(projections);

            main.add(Restrictions.and(
                    Restrictions.eq("u.activation.administrativeStatus", RActivationStatus.ENABLED),
                    Restrictions.eq("u.activation.validFrom", XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime()))));

            String expected = HibernateToSqlTranslator.toSql(main);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
            //        ItemPath triggerPath = new ItemPath(AssignmentType.F_ACTIVATION);

            //        PrismContainerDefinition triggerContainerDef = objectDef.findContainerDefinition(triggerPath);

            ObjectFilter filter1 = EqualFilter.createEqual(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), objectDef,
                    ActivationStatusType.ENABLED);

            ObjectFilter filter2 = EqualFilter.createEqual(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM), objectDef,
                    XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime()));

            ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filter1, filter2));
            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryTriggerTimestampDouble() throws Exception {
        final Date NOW = new Date();

        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Criteria d = main.createCriteria("trigger", "t", JoinType.LEFT_OUTER_JOIN);
            d.add(Restrictions.and(
                    Restrictions.gt("t.timestamp", new Timestamp(NOW.getTime())),
                    Restrictions.lt("t.timestamp", new Timestamp(NOW.getTime()))
            ));

            String expected = HibernateToSqlTranslator.toSql(main);

            XMLGregorianCalendar thisScanTimestamp = XmlTypeConverter.createXMLGregorianCalendar(NOW.getTime());

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
            ItemPath triggerPath = new ItemPath(ObjectType.F_TRIGGER, TriggerType.F_TIMESTAMP);
            //        PrismContainerDefinition triggerContainerDef = objectDef.findContainerDefinition(triggerPath);
            ObjectFilter greater = GreaterFilter.createGreater(triggerPath, objectDef, thisScanTimestamp, false);
            ObjectFilter lesser = LessFilter.createLess(triggerPath, objectDef, thisScanTimestamp, false);
            AndFilter and = AndFilter.createAnd(greater, lesser);
            LOGGER.info(and.debugDump());

            ObjectQuery query = ObjectQuery.createObjectQuery(and);
            String real = getInterpretedQuery(session, ObjectType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    private void addFullObjectProjectionList(String prefix, ProjectionList list, boolean group) {
        if (prefix == null) {
            prefix = "";
        } else {
            prefix = prefix + ".";
        }

        if (group) {
            list.add(Projections.groupProperty(prefix + "fullObject"));
            list.add(Projections.groupProperty(prefix + "stringsCount"));
            list.add(Projections.groupProperty(prefix + "longsCount"));
            list.add(Projections.groupProperty(prefix + "datesCount"));
            list.add(Projections.groupProperty(prefix + "referencesCount"));
            list.add(Projections.groupProperty(prefix + "polysCount"));
        } else {
            list.add(Projections.property(prefix + "fullObject"));
            list.add(Projections.property(prefix + "stringsCount"));
            list.add(Projections.property(prefix + "longsCount"));
            list.add(Projections.property(prefix + "datesCount"));
            list.add(Projections.property(prefix + "referencesCount"));
            list.add(Projections.property(prefix + "polysCount"));
        }
    }

    @Test
    public void countObjectOrderByName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            main.add(Restrictions.and(
                    Restrictions.eq("u.name.orig", "cpt. Jack Sparrow"),
                    Restrictions.eq("u.name.norm", "cpt jack sparrow")));
            main.setProjection(Projections.rowCount());
            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter filter = EqualFilter.createEqual(UserType.F_NAME, UserType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));

            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

            String real = getInterpretedQuery(session, UserType.class, query, true);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void countObjectOrderByNameWithoutFilter() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            main.setProjection(Projections.rowCount());
            String expected = HibernateToSqlTranslator.toSql(main);

            ObjectPaging paging = ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING);
            ObjectQuery query = ObjectQuery.createObjectQuery(null, paging);

            String real = getInterpretedQuery(session, ObjectType.class, query, true);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
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
    public void countTaskOrderByName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RTask.class, "t");
            main.add(Restrictions.isNull("t.parent"));

            main.setProjection(Projections.rowCount());
            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter filter = EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, prismContext, null);

            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            query.setPaging(ObjectPaging.createPaging(null, null, TaskType.F_NAME, OrderDirection.ASCENDING));

            String real = getInterpretedQuery(session, TaskType.class, query, true);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void inOidTest() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            main.add(Restrictions.in("oid", Arrays.asList("1", "2")));
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            InOidFilter filter = InOidFilter.createInOid(Arrays.asList("1", "2"));

            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, ObjectType.class, query, false);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void queryOrgTreeFindOrgs() throws Exception {
        Session session = open();

        try {

            Criteria main = session.createCriteria(ROrg.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            DetachedCriteria detached = DetachedCriteria.forClass(RParentOrgRef.class, "p");
            detached.setProjection(Projections.distinct(Projections.property("p.ownerOid")));
            detached.add(Restrictions.eq("p.targetOid", "some oid"));

            main.add(Subqueries.propertyIn("o.oid", detached));
            main.addOrder(Order.asc("o.name.orig"));

            String expected = HibernateToSqlTranslator.toSql(main);

            OrgFilter orgFilter = OrgFilter.createOrg("some oid", OrgFilter.Scope.ONE_LEVEL);
            ObjectQuery objectQuery = ObjectQuery.createObjectQuery(orgFilter);
            objectQuery.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

            String real = getInterpretedQuery(session, OrgType.class, objectQuery);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});

            OperationResult result = new OperationResult("query org structure");
            repositoryService.searchObjects(OrgType.class, objectQuery, null, result);

            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void asdf() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RUser.class, "u");
            Criteria a = main.createCriteria("assignments", "a");
            a.add(Restrictions.eq("a.assignmentOwner", RAssignmentOwner.FOCUS));
            Criteria e = a.createCriteria("a.extension");

            Criteria s = e.createCriteria("strings", "s");

            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("s.extensionType", RAssignmentExtensionType.EXTENSION));
            c2.add(Restrictions.eq("s.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
            c2.add(Restrictions.eq("s.value", "uid=jbond,ou=People,dc=example,dc=com"));

            Conjunction c1 = Restrictions.conjunction();
            c1.add(Restrictions.eq("a.targetRef.targetOid", "1234"));
            c1.add(Restrictions.eq("a.targetRef.type", RObjectType.ORG));

            main.add(Restrictions.and(c1, c2));

            main.setProjection(Projections.property("u.fullObject"));

            String expected = HibernateToSqlTranslator.toSql(main);
            LOGGER.info(">>> >>> {}", expected);
        } finally {
            close(session);
        }
    }


    @Test
    public void test100ActivationQuery() throws Exception {
        PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        XMLGregorianCalendar thisScanTimestamp = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

        OrFilter filter = OrFilter.createOr(
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef,
                        thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef,
                        thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                        focusObjectDef, thisScanTimestamp, true),
                LessFilter.createLess(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                        focusObjectDef, thisScanTimestamp, true)
        );

        Session session = open();
        try {
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, UserType.class, query, false);

            String expected = null;
            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        } finally {
            close(session);
        }
    }

    @Test
    public void test200ActivationQuery() throws Exception {
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
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                focusObjectDef, lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                                focusObjectDef, thisScanTimestamp, true)
                ),
                AndFilter.createAnd(
                        GreaterFilter.createGreater(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                focusObjectDef, lastScanTimestamp, false),
                        LessFilter.createLess(new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALID_TO),
                                focusObjectDef, thisScanTimestamp, true)
                )
        );

        Session session = open();
        try {
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            String real = getInterpretedQuery(session, UserType.class, query, false);

            String expected = null;
            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        } finally {
            close(session);
        }
    }

    @Test
    public void test300OrgQuery() throws Exception {
        File objects = new File("src/test/resources/orgstruct/org-monkey-island.xml");
        OperationResult opResult = new OperationResult("test300OrgQuery");
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
        query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

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
    public void test310QueryNameAndOrg() throws Exception {
        Session session = open();

        try {
            DetachedCriteria detached = DetachedCriteria.forClass(ROrgClosure.class, "cl");
            detached.setProjection(Projections.distinct(Projections.property("cl.descendantOid")));
            detached.add(Restrictions.eq("cl.ancestorOid", "1234"));
            detached.add(Restrictions.ne("cl.descendantOid", "1234"));

            Criteria main = session.createCriteria(RUser.class, "u");
            String mainAlias = "u";

            ProjectionList projections = Projections.projectionList();
            projections.add(Projections.property("fullObject"));

            projections.add(Projections.property("stringsCount"));
            projections.add(Projections.property("longsCount"));
            projections.add(Projections.property("datesCount"));
            projections.add(Projections.property("referencesCount"));
            projections.add(Projections.property("polysCount"));

            main.setProjection(projections);

            Conjunction c = Restrictions.conjunction();
            c.add(Restrictions.and(
                    Restrictions.eq("u.name.orig", "cpt. Jack Sparrow"),
                    Restrictions.eq("u.name.norm", "cpt jack sparrow")));
            c.add(Subqueries.propertyIn(mainAlias + ".oid", detached));
            main.add(c);


            main.addOrder(Order.asc("u.name.orig"));

            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter eqFilter = EqualFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext,
                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));

            OrgFilter orgFilter = OrgFilter.createOrg("12341234-1234-1234-1234-123412341234");

            ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(eqFilter, orgFilter));
            query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

            String real = getInterpretedQuery(session, UserType.class, query);

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(enabled = false)
    public void test320QueryEmployeeTypeAndOrgType() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            main.add(Restrictions.or(
                    Restrictions.and(
                            Restrictions.eq("o.name.orig", "some name"),
                            Restrictions.eq("o.employeeNumber", "123")
                    ),
                    Restrictions.eq("o.identifier", "1234")
            ));
            ProjectionList list = Projections.projectionList();
            addFullObjectProjectionList("o", list, false);
            main.setProjection(list);

            List l = main.list();
            l.size();
            String expected = HibernateToSqlTranslator.toSql(main);
            LOGGER.info("expected query>\n{}", new Object[]{expected});


//            EqualsFilter nameFilter = EqualsFilter.createEqual(ObjectType.F_NAME, ObjectType.class, prismContext,
//                    null, new PolyString("cpt. Jack Sparrow", "cpt jack sparrow"));
//
//            EqualsFilter numberFilter = EqualsFilter.createEqual(UserType.F_EMPLOYEE_NUMBER, UserType.class, prismContext,
//                    null, "123");
//
////            EqualsFilter orgTypeFilter = EqualsFilter.createEqual(OrgType.F_ORG_TYPE, OrgType.class, prismContext,
////                    null, "orgtypevalue");
//
//            ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(
//                    AndFilter.createAnd(nameFilter, numberFilter)//,
////                    orgTypeFilter
//            ));
//            query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));
//
//            String real = getInterpretedQuery(session, ObjectType.class, query);
//
//            LOGGER.info("real query>\n{}", new Object[]{real});
        } finally {
            close(session);
        }
    }

    @Test
    public void test330queryUserSubstringName() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            main.add(Restrictions.like("name.orig", "a%"));
            String expected = HibernateToSqlTranslator.toSql(main);

            SubstringFilter substring = SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class,
                    prismContext, PolyStringOrigMatchingRule.NAME, "a");
            substring.setAnchorStart(true);
            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(substring));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);

            OperationResult result = new OperationResult("test330queryUserSubstringName");
            int count = repositoryService.countObjects(ObjectType.class, ObjectQuery.createObjectQuery(substring), result);
            AssertJUnit.assertEquals(2, count);

            substring = SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class,
                    prismContext, PolyStringOrigMatchingRule.NAME, "a");
            count = repositoryService.countObjects(ObjectType.class, ObjectQuery.createObjectQuery(substring), result);
            AssertJUnit.assertEquals(16, count);
        } finally {
            close(session);
        }
    }

    @Test
    public void test340queryObjectClassTypeUser() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            main.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));
            String expected = HibernateToSqlTranslator.toSql(main);

            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, null);
            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test350queryObjectClassTypeAbstractRole() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            List<RObjectType> list = new ArrayList<>();
            list.add(RObjectType.ABSTRACT_ROLE);
            list.add(RObjectType.ORG);
            list.add(RObjectType.ROLE);
            main.add(Restrictions.in("o." + RObject.F_OBJECT_TYPE_CLASS, list));
            String expected = HibernateToSqlTranslator.toSql(main);

            TypeFilter type = TypeFilter.createType(AbstractRoleType.COMPLEX_TYPE, null);
            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test360queryMetadataTimestamp() throws Exception {
        Session session = open();

        try {
            Criteria main = session.createCriteria(RReportOutput.class, "r");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("r", projections, false);
            main.setProjection(projections);

            XMLGregorianCalendar timeXml = XMLGregorianCalendarType.asXMLGregorianCalendar(new Date());

            main.add(Restrictions.le("r.createTimestamp", timeXml));
            String expected = HibernateToSqlTranslator.toSql(main);

            LessFilter less = LessFilter.createLess(
                    new ItemPath(ReportOutputType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                    ReportOutputType.class, prismContext, timeXml, true);

            String real = getInterpretedQuery(session, ReportOutputType.class, ObjectQuery.createObjectQuery(less));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test370queryObjectypeByTypeUserAndLocality() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Conjunction c = Restrictions.conjunction();
            main.add(c);
            c.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));
            c.add(Restrictions.and(Restrictions.eq("o.localityUser.orig", "Caribbean"),
                    Restrictions.eq("o.localityUser.norm", "caribbean")));

            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter eq = EqualFilter.createEqual(new ItemPath(UserType.F_LOCALITY), UserType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);

            checkQueryTypeAlias(real, "m_user", "locality_orig", "locality_norm");
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
                if (token.endsWith(property + "=?") && !token.startsWith("(" + alias + ".")) {
                    AssertJUnit.fail("Property '" + property + "' doesn't have proper alias '"
                            + alias + "' in token '" + token + "'");
                }
            }
        }
    }

    @Test
    public void test375queryObjectypeByTypeOrgAndLocality() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Conjunction c = Restrictions.conjunction();
            main.add(c);
            c.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.ORG));
            c.add(Restrictions.and(Restrictions.eq("o.locality.orig", "Caribbean"),
                    Restrictions.eq("o.locality.norm", "caribbean")));

            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter eq = EqualFilter.createEqual(new ItemPath(OrgType.F_LOCALITY), OrgType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            TypeFilter type = TypeFilter.createType(OrgType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);

            checkQueryTypeAlias(real, "m_org", "locality_orig", "locality_norm");
        } finally {
            close(session);
        }
    }

    @Test
    public void test380queryObjectypeByTypeAndExtensionAttribute() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Conjunction c = Restrictions.conjunction();
            main.add(c);
            c.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));

            Conjunction c2 = Restrictions.conjunction();
            main.createCriteria("strings", "s", JoinType.LEFT_OUTER_JOIN);
            c2.add(Restrictions.eq("s.ownerType", RObjectExtensionType.EXTENSION));
            c2.add(Restrictions.eq("s.name", new QName("http://example.com/p", "weapon")));
            c2.add(Restrictions.eq("s.value", "some weapon name"));
            c.add(c2);

            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "weapon")),
                    UserType.class, prismContext, "some weapon name");
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, eq);

            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test390queryObjectypeByTypeAndReference() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Conjunction c = Restrictions.conjunction();
            main.add(c);
            c.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));

            Criteria refs = main.createCriteria("linkRef", "l", JoinType.LEFT_OUTER_JOIN);
            c.add(Restrictions.and(Restrictions.eq("l.targetOid", "123")));

            String expected = HibernateToSqlTranslator.toSql(main);

            RefFilter ref = RefFilter.createReferenceEqual(UserType.F_LINK_REF, UserType.class, prismContext, "123");
            TypeFilter type = TypeFilter.createType(UserType.COMPLEX_TYPE, ref);

            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(type));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test
    public void test400queryObjectypeByTypeComplex() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RObject.class, "o");
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("o", projections, false);
            main.setProjection(projections);

            Conjunction c1 = Restrictions.conjunction();
            c1.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.USER));
            Criterion e1 = Restrictions.and(Restrictions.eq("o.localityUser.orig", "Caribbean"),
                    Restrictions.eq("o.localityUser.norm", "caribbean"));
            Criterion e2 = Restrictions.and(Restrictions.eq("o.localityUser.orig", "Adriatic"),
                    Restrictions.eq("o.localityUser.norm", "adriatic"));
            c1.add(Restrictions.or(e1, e2));

            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.ORG));
            Criteria o1 = main.createCriteria("o.orgType", "o1", JoinType.LEFT_OUTER_JOIN);
            c2.add(Restrictions.eq("o1.elements", "functional"));

            Criterion c3 = Restrictions.eq("o." + RObject.F_OBJECT_TYPE_CLASS, RObjectType.REPORT);

            main.add(Restrictions.or(c1, c2, c3));
            String expected = HibernateToSqlTranslator.toSql(main);


            EqualFilter eq1 = EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, prismContext,
                    new PolyString("Caribbean", "caribbean"));
            EqualFilter eq2 = EqualFilter.createEqual(UserType.F_LOCALITY, UserType.class, prismContext,
                    new PolyString("Adriatic", "adriatic"));
            TypeFilter type1 = TypeFilter.createType(UserType.COMPLEX_TYPE, OrFilter.createOr(eq1, eq2));

            EqualFilter equal = EqualFilter.createEqual(OrgType.F_ORG_TYPE, OrgType.class, prismContext, "functional");
            TypeFilter type2 = TypeFilter.createType(OrgType.COMPLEX_TYPE, equal);

            TypeFilter type3 = TypeFilter.createType(ReportType.COMPLEX_TYPE, null);

            OrFilter or = OrFilter.createOr(type1, type2, type3);

            String real = getInterpretedQuery(session, ObjectType.class, ObjectQuery.createObjectQuery(or));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }

    @Test(expectedExceptions = QueryException.class)
    public void test410QueryGenericClob() throws Exception {
        Session session = open();
        try {
            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "locations")),
                    GenericObjectType.class, prismContext, null);

            getInterpretedQuery(session, GenericObjectType.class, ObjectQuery.createObjectQuery(eq));
        } catch (QueryException ex) {
            LOGGER.info("Exception", ex);
            throw ex;
        } finally {
            close(session);
        }
    }

    @Test
    public void test420QueryGenericString() throws Exception {
        Session session = open();
        try {
            Criteria main = session.createCriteria(RGenericObject.class, "g");

            Criteria stringExt = main.createCriteria("strings", "s", JoinType.LEFT_OUTER_JOIN);

            //and
            Conjunction c2 = Restrictions.conjunction();
            c2.add(Restrictions.eq("s.ownerType", RObjectExtensionType.EXTENSION));
            c2.add(Restrictions.eq("s.name", new QName("http://example.com/p", "stringType")));
            c2.add(Restrictions.eq("s.value", "asdf"));

            main.add(c2);
            ProjectionList projections = Projections.projectionList();
            addFullObjectProjectionList("g", projections, false);
            main.setProjection(projections);

            String expected = HibernateToSqlTranslator.toSql(main);

            EqualFilter eq = EqualFilter.createEqual(
                    new ItemPath(ObjectType.F_EXTENSION, new QName("http://example.com/p", "stringType")),
                    GenericObjectType.class, prismContext, "asdf");

            String real = getInterpretedQuery(session, GenericObjectType.class, ObjectQuery.createObjectQuery(eq));

            LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
            AssertJUnit.assertEquals(expected, real);
        } finally {
            close(session);
        }
    }
}
