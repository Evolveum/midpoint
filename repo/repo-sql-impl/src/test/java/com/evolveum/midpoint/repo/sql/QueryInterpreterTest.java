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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RAccountShadow;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreterException;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class QueryInterpreterTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(AddGetObjectTest.class);
    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;
    @Autowired(required = true)
    SessionFactory factory;

    @Test
    public void queryOrComposite() throws Exception {
        Session session = open();
        Criteria main = session.createCriteria(RAccountShadow.class, "a");

        Criteria resourceRef = main.createCriteria("resourceRef", "r");

        Criteria extension = main.createCriteria("extension", "e");
        Criteria stringAttr = extension.createCriteria("strings", "s");

        //first
        Conjunction and = Restrictions.conjunction();
        and.add(Restrictions.eq("s.value", "uid=test,dc=example,dc=com"));
        and.add(Restrictions.eq("s.name", new QName("http://example.com/p", "stringType")));
        and.add(Restrictions.eq("s.type", new QName("http://www.w3.org/2001/XMLSchema", "string")));
        //or second
        main.add(Restrictions.or(and, Restrictions.eq("r.targetOid", "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e")));

        String expected = HibernateToSqlTranslator.toSql(main);


        String real = getInterpretedQuery(session, AccountShadowType.class,
                new File("./src/test/resources/query/query-or-composite.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    @Test
    public void queryUserFullName() throws Exception {
        Session session = open();

        Criteria main = session.createCriteria(RUser.class, "u");
        main.add(Restrictions.eq("fullName", "Cpt. Jack Sparrow"));
        String expected = HibernateToSqlTranslator.toSql(main);

        String real = getInterpretedQuery(session, UserType.class,
                new File("./src/test/resources/query/query-user-by-fullName.xml"));

        LOGGER.info("exp. query>\n{}\nreal query>\n{}", new Object[]{expected, real});
        AssertJUnit.assertEquals(expected, real);

        close(session);
    }

    private <T extends ObjectType> String getInterpretedQuery(Session session, Class<T> type, File file) throws
            QueryInterpreterException {

        QueryInterpreter interpreter = new QueryInterpreter(session, type, prismContext);

        Document document = DOMUtil.parseFile(file);
        Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);
        Criteria criteria = interpreter.interpret(filter);
        return HibernateToSqlTranslator.toSql(criteria);
    }

    private Session open() {
        Session session = factory.openSession();
        session.beginTransaction();
        return session;
    }

    private void close(Session session) {
        session.getTransaction().commit();
        session.close();
    }
}
