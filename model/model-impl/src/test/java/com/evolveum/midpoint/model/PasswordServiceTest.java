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

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.evolveum.midpoint.model.PasswordService;
import com.evolveum.midpoint.xml.ns._public.model.password_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordChangeRequestType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordChangeResponseType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordSynchronizeRequestType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.SelfPasswordChangeRequestType;

/**
 *
 * @author laszlohordos
 */
public class PasswordServiceTest {

    private JAXBContext ctx;
    private Unmarshaller unmarshaller;

    public PasswordServiceTest() throws JAXBException {
        ctx = JAXBContext.newInstance(ObjectFactory.class);
        unmarshaller = ctx.createUnmarshaller();

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    @Ignore
    public void testSelfChangePassword() {
        System.out.println("selfChangePassword");
        SelfPasswordChangeRequestType spcrt = null;
        PasswordService instance = new PasswordService();
        PasswordChangeResponseType expResult = null;
        PasswordChangeResponseType result = instance.selfChangePassword(spcrt);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore
    public void testChangePassword() {
        System.out.println("changePassword");
//        PasswordChangeRequestType pcrt = null;
//        PasswordService instance = new PasswordService();
//        PasswordChangeResponseType expResult = null;
//        PasswordChangeResponseType result = instance.changePassword(pcrt);
//        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore
    public void testSynchronizePassword() throws JAXBException {
        System.out.println("synchronizePassword");
        InputStream in = PasswordServiceTest.class.getResourceAsStream("/password-SynchronizeRequest.xml");
        JAXBElement<PasswordSynchronizeRequestType> input = (JAXBElement<PasswordSynchronizeRequestType>) unmarshaller.unmarshal(in);
        PasswordService instance = new PasswordService();
        instance.synchronizePassword(input.getValue());        
    }

}
