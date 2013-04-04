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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryInterpreterTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreterTest.class);
    private static final File TEST_DIR = new File("./src/test/resources/query");

    @Test(enabled = false)//todo implement and enable
    public void queryEnabled() throws  Exception {
        Session session = open();
        Criteria main = session.createCriteria(RUser.class, "u");
        main.add(Restrictions.eq("activation.enabled", true));


        String expected = HibernateToSqlTranslator.toSql(main);
        String real = getInterpretedQuery(session, UserType.class,
                new File(TEST_DIR, "query-user-by-enabled.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }


    @Test
    public void queryGenericLong() throws Exception {
        Session session = open();
        Criteria main = session.createCriteria(RGenericObject.class, "g");

        Criteria extension = main.createCriteria("extension", "e");
        Criteria stringExt = extension.createCriteria("longs", "l");

        //and
        Criterion c1 = Restrictions.eq("name.norm", "generic object");
        //and
        Conjunction c2 = Restrictions.conjunction();
        c2.add(Restrictions.eq("l.value", 123L));
        c2.add(Restrictions.eq("l.name", new QName("http://example.com/p", "intType")));
        c2.add(Restrictions.eq("l.type", new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer")));

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(c1);
        conjunction.add(c2);
        main.add(conjunction);

        String expected = HibernateToSqlTranslator.toSql(main);
        String real = getInterpretedQuery(session, GenericObjectType.class,
                new File(TEST_DIR, "query-and-generic.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryOrComposite() throws Exception {
        Session session = open();
        Criteria main = session.createCriteria(RResourceObjectShadow.class, "r");

        Criteria attributes = main.createCriteria("attributes", "a");
        Criteria stringAttr = attributes.createCriteria("strings", "s1x");

        Criteria extension = main.createCriteria("extension", "e");
        Criteria stringExt = extension.createCriteria("strings", "s2x");

        //or
        Criterion c1 = Restrictions.eq("intent", "some account type");
        //or
        Conjunction c2 = Restrictions.conjunction();
        c2.add(Restrictions.eq("s1x.value", "foo value"));
        c2.add(Restrictions.eq("s1x.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
        c2.add(Restrictions.eq("s1x.type", new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string")));
        //or
        Conjunction c3 = Restrictions.conjunction();
        c3.add(Restrictions.eq("s2x.value", "uid=test,dc=example,dc=com"));
        c3.add(Restrictions.eq("s2x.name", new QName("http://example.com/p", "stringType")));
        c3.add(Restrictions.eq("s2x.type", new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string")));
        //or
        Criterion c4 = Restrictions.eq("r.resourceRef.targetOid", "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e");

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

        close(session);
    }

    @Test
    public void queryUserByFullName() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryUserByFullName"});
        Session session = open();

        Criteria main = session.createCriteria(RUser.class, "u");
        main.add(Restrictions.eq("fullName.norm", "cpt jack sparrow"));
        String expected = HibernateToSqlTranslator.toSql(main);

        String real = getInterpretedQuery(session, UserType.class,
                new File(TEST_DIR, "query-user-by-fullName.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryUserSubstringFullName() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryUserSubstringFullName"});
        Session session = open();

        Criteria main = session.createCriteria(RUser.class, "u");
        main.add(Restrictions.like("fullName.norm", "%cpt jack sparrow%").ignoreCase());
        String expected = HibernateToSqlTranslator.toSql(main);

        String real = getInterpretedQuery(session, UserType.class,
                new File(TEST_DIR, "query-user-substring-fullName.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryUserByName() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryUserByName"});
        Session session = open();

        Criteria main = session.createCriteria(RUser.class, "u");
        main.add(Restrictions.eq("name.norm", "some name identificator"));
        String expected = HibernateToSqlTranslator.toSql(main);

        String real = getInterpretedQuery(session, UserType.class,
                new File(TEST_DIR, "query-user-by-name.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryConnectorByType() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryConnectorByType"});
        Session session = open();

        Criteria main = session.createCriteria(RConnector.class, "c");
        main.add(Restrictions.eq("connectorType", "org.identityconnectors.ldap.LdapConnector"));
        String expected = HibernateToSqlTranslator.toSql(main);

        String real = getInterpretedQuery(session, ConnectorType.class,
                new File(TEST_DIR, "query-connector-by-type.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryAccountByAttributesAndResourceRef() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryAccountByAttributesAndResourceRef"});
        Session session = open();
        Criteria main = session.createCriteria(RResourceObjectShadow.class, "r");

        Criteria attributes = main.createCriteria("attributes", "a");
        Criteria stringAttr = attributes.createCriteria("strings", "s1x");

        //and
        Criterion c1 = Restrictions.eq("r.resourceRef.targetOid", "aae7be60-df56-11df-8608-0002a5d5c51b");
        //and
        Conjunction c2 = Restrictions.conjunction();
        c2.add(Restrictions.eq("s1x.value", "uid=jbond,ou=People,dc=example,dc=com"));
        c2.add(Restrictions.eq("s1x.name", new QName("http://midpoint.evolveum.com/blabla", "foo")));
        c2.add(Restrictions.eq("s1x.type", new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string")));

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(c1);
        conjunction.add(c2);
        main.add(conjunction);

        String expected = HibernateToSqlTranslator.toSql(main);
        String real = getInterpretedQuery(session, ShadowType.class,
                new File(TEST_DIR, "query-account-by-attributes-and-resource-ref.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryUserAccountRef() throws Exception {
        LOGGER.info("===[{}]===", new Object[]{"queryUserAccountRef"});
        Session session = open();
        Criteria main = session.createCriteria(RUser.class, "u");
        Criteria refs = main.createCriteria("accountRefs", "a");
        refs.add(Restrictions.eq("a.targetOid", "123"));

        String expected = HibernateToSqlTranslator.toSql(main);

        RefFilter filter = RefFilter.createReferenceEqual(UserType.class, UserType.F_ACCOUNT_REF, prismContext, "123");
        String real = getInterpretedQuery(session, UserType.class, filter);

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    private <T extends ObjectType> String getInterpretedQuery(Session session, Class<T> type, ObjectFilter filter) throws
            QueryException, SchemaException, FileNotFoundException, JAXBException {

        QueryInterpreter interpreter = new QueryInterpreter(session, type, prismContext);
        LOGGER.info("QUERY TYPE TO CONVERT : {}", filter.debugDump(3));

        Criteria criteria = interpreter.interpret(filter);

        return HibernateToSqlTranslator.toSql(criteria);
    }

    private <T extends ObjectType> String getInterpretedQuery(Session session, Class<T> type, File file) throws
            QueryException, SchemaException, FileNotFoundException, JAXBException {

        QueryInterpreter interpreter = new QueryInterpreter(session, type, prismContext);

        Document document = DOMUtil.parseFile(file);
        QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(file, QueryType.class);
        Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);

        LOGGER.info("QUERY TYPE TO CONVERT : {}", QueryUtil.dump(queryType));

        ObjectQuery  query = QueryConvertor.createObjectQuery(type, queryType, prismContext);

        Criteria criteria = interpreter.interpret(query.getFilter());
        return HibernateToSqlTranslator.toSql(criteria);
    }

    private Session open() {
        Session session = getFactory().openSession();
        session.beginTransaction();
        return session;
    }

    private void close(Session session) {
        session.getTransaction().commit();
        session.close();
    }
}
