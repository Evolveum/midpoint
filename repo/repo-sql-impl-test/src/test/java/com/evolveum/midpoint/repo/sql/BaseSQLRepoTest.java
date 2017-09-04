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

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.HibernateToSqlTranslator;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
public class BaseSQLRepoTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(BaseSQLRepoTest.class);

    public static final File FOLDER_BASE = new File("./src/test/resources");

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");

    @Autowired
    protected LocalSessionFactoryBean sessionFactoryBean;
    @Autowired
    protected RepositoryService repositoryService;
	@Autowired
	protected BaseHelper baseHelper;
    @Autowired
    protected AuditService auditService;
    @Autowired
    protected PrismContext prismContext;
    @Autowired
    protected SessionFactory factory;

    protected static Set<Class> initializedClasses = new HashSet<>();

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    public SessionFactory getFactory() {
        return factory;
    }

    public void setFactory(SessionFactory factory) {
        RUtil.fixCompositeIDHandling(factory);

        this.factory = factory;
    }

    protected boolean isSystemInitialized() {
        return initializedClasses.contains(this.getClass());
    }

    private void setSystemInitialized() {
        initializedClasses.add(this.getClass());
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> START " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> START {} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @AfterClass
    public void afterClass() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> FINISH " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> FINISH {} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> START TEST" + getClass().getName() + "." + method.getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> START {}.{} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});

        if (!isSystemInitialized()) {
            initSystem();
            setSystemInitialized();
        }
    }

    @AfterMethod
    public void afterMethod(Method method) {
        try {
            Session session = factory.getCurrentSession();
            if (session != null) {
                session.close();
                AssertJUnit.fail("Session is still open, check test code or bug in sql service.");
            }
        } catch (Exception ex) {
            //it's ok
            LOGGER.debug("after test method, checking for potential open session, exception occurred: " + ex.getMessage());
        }

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> END TEST" + getClass().getName() + "." + method.getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> END {}.{} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});
    }

    protected boolean isH2used() {
        String dialect = sessionFactoryBean.getHibernateProperties().getProperty("hibernate.dialect");

        return H2Dialect.class.getName().equals(dialect);
    }

    public void initSystem() throws Exception {

    }

    protected Session open() {
        Session session = getFactory().openSession();
        session.beginTransaction();
        return session;
    }

    protected void close(Session session) {
        session.getTransaction().commit();
        session.close();
    }

    protected String hqlToSql(String hql) {
        return HibernateToSqlTranslator.toSql(factory, hql);
    }

}
