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

package com.evolveum.midpoint.common.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class JAXBUtilTest {

    public JAXBUtilTest() {
    }

    static Document doc;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doc = DOMUtil.getDocument();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void jaxbToDomTest1() throws JAXBException {
        UserType o = new UserType();
        o.setOid("1234");
        o.setName("foobar");

        Element el = JAXBUtil.jaxbToDom(o, new QName("http://foo/","bar","x"), doc);

        assertNotNull(el);
        AssertJUnit.assertEquals("bar",el.getLocalName());

//        System.out.println("EL: "+el);
//        System.out.println(DOMUtil.serializeDOMToString(el));
        
    }

        @Test
    public void jaxbToDomTest2() throws JAXBException {
        ObjectReferenceType o = new ObjectReferenceType();
        o.setOid("1234");

        Element el = JAXBUtil.jaxbToDom(o, new QName("http://foo/","accountRef","x"), doc);

        assertNotNull(el);
        AssertJUnit.assertEquals("accountRef",el.getLocalName());

        System.out.println("EL2: "+el);
        System.out.println(DOMUtil.serializeDOMToString(el));

    }


}