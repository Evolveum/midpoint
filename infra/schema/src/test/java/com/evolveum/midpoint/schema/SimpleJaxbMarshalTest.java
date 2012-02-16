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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import java.io.File;

/**
 * @author lazyman
 */
public class SimpleJaxbMarshalTest {

    public static final String TEST_DIR = "src/test/resources/schema";
    public static final String USER_BARBOSSA_FILENAME = TEST_DIR + "/user-barbossa.xml";
    private static final Trace LOGGER = TraceManager.getTrace(SimpleJaxbMarshalTest.class);

    @Test
    public void unmarshalMarshalUser() throws Exception {
        JAXBElement<UserType> element = JaxbTestUtil.unmarshalElement(new File(USER_BARBOSSA_FILENAME),
                UserType.class);
        System.out.println(element.getValue().toString());
        System.out.println(JaxbTestUtil.marshalWrap(element.getValue()));
    }
    
    @Test
    public void marshalUser() throws Exception {
        UserType user = new UserType();
        user.setOid("1234");
        user.setGivenName("vilko");
        user.setName("lazyman");
        user.setFamilyName("family");

        System.out.println(JaxbTestUtil.marshalWrap(user));
    }
}
