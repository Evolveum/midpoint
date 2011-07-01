/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.test.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;

/**
 *
 * @author laszlohordos
 */
@Ignore
public class BaseXDatabaseFactoryTest {

    private ObjectFactory objectFactory = new ObjectFactory();

    private File repoLocation;

    public BaseXDatabaseFactoryTest() {
    	repoLocation = new File("src/main/resources/test-data/repository/");
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    	try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of XMLDBQuery method, of class BaseXDatabaseFactory.
     */
    @Test
    @Ignore
    public void testXMLDBQuery() throws Exception {
        BaseXDatabaseFactory.XMLServerStart(BaseXDatabaseFactoryTest.class.getResource("/").getPath(), new String[]{"-d"});
        BaseXDatabaseFactory instance = new BaseXDatabaseFactory();
        instance.XMLDBQuery();
        BaseXDatabaseFactory.XMLServerStop();
        assertTrue(true);
    }

    @Test
    //@Ignore
    public void testGetDeleteObject() throws Exception {
        RepositoryPortType port = BaseXDatabaseFactory.getRepositoryPort(repoLocation);
        assertNotNull(port);

        String oid = "ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
        try {
            ObjectContainerType out = port.getObject(oid, new PropertyReferenceListType());
            assertNotNull(out);
            assertNotNull(out.getObject());
            port.deleteObject(oid);
            try {
            	port.getObject(oid, new PropertyReferenceListType());
            	assertTrue(false);
            } catch (FaultMessage fault) {
            	// This is expected
            }
        } finally {
            BaseXDatabaseFactory.XMLServerStop();
        }

    }

    @Test
    //@Ignore
    public void testAddObject() throws Exception {
        RepositoryPortType port = BaseXDatabaseFactory.getRepositoryPort(repoLocation);
        assertNotNull(port);

        String oid = null;
        try {
            ResourceType res = objectFactory.createResourceType();

            res.setName("OpenDJ Local");
            res.setType("Type");

            AccountShadowType account = objectFactory.createAccountShadowType();
            account.setName("TAstUser");
            account.setObjectClass(new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", "__ACCOUNT__"));

            ObjectContainerType in = objectFactory.createObjectContainerType();
            in.setObject(res);
            oid = port.addObject(in);

            assertNotNull(oid);

            in.setObject(account);
            oid = port.addObject(in);

            ObjectContainerType out = port.getObject(oid, null);

            assertNotNull(out.getObject());

        } finally {
            BaseXDatabaseFactory.XMLServerStop();
        }

    }
}
