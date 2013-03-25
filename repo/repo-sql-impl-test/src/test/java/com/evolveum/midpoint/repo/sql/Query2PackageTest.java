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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class Query2PackageTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(Query2PackageTest.class);
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
    public void testQueryRegistry() throws QueryException {
        QueryInterpreter interpreter = new QueryInterpreter();


        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        String dump = registry.dump();

        LOGGER.info(dump);
    }

    @Test
    public void queryUserByFullName() throws Exception {
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

    private <T extends ObjectType> String getInterpretedQuery(Session session, Class<T> type, File file) throws
            QueryException, SchemaException, FileNotFoundException, JAXBException {

        QueryInterpreter interpreter = new QueryInterpreter();

        Document document = DOMUtil.parseFile(file);
        QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(file, QueryType.class);
        Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);

        LOGGER.info("QUERY TYPE TO CONVERT : {}", QueryUtil.dump(queryType));

        ObjectQuery query = null;
        try {
            query = QueryConvertor.createObjectQuery(type, queryType, prismContext);
        } catch (Exception ex) {
            LOGGER.info("error while converting query: " + ex.getMessage(), ex);
        }
        Criteria criteria = interpreter.interpret(query, type, prismContext, session);
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
