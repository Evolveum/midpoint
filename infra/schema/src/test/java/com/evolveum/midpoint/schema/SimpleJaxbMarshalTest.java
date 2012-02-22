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

import static org.testng.AssertJUnit.assertEquals;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.testng.AssertJUnit;
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
        UserType user1 = element.getValue();
        
        element = JaxbTestUtil.unmarshalElement(JaxbTestUtil.marshalToString(user1));
        UserType user2 = element.getValue();

        assertEquals("Users not same (UserType)", user1, user2);
        assertEquals("Users not same (prism)", user1.asPrismObject(), user2.asPrismObject());
    }

    @Test
    public void marshalUser() throws Exception {
        UserType user = new UserType();
        user.setOid("1234");
        user.setGivenName("vilko");
        user.setName("lazyman");
        user.setFamilyName("family");
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("5678");
        ref.setType(AccountShadowType.COMPLEX_TYPE);
        user.getAccountRef().add(ref);

        JAXBElement<UserType> element = JaxbTestUtil.unmarshalElement(JaxbTestUtil.marshalToString(user));
        UserType user1 = element.getValue();
        assertEquals(user.getOid(), user1.getOid());
        assertEquals(user.getGivenName(), user1.getGivenName());
        assertEquals(user.getName(), user1.getName());
        assertEquals(user.getFamilyName(), user1.getFamilyName());
    }
}
