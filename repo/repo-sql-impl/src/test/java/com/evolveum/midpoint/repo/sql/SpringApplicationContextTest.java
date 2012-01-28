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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.util.List;

/**
 * @author lazyman
 */
public class SpringApplicationContextTest {

    @Test
    public void initialize() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context-repo-sql.xml");
        SqlRepositoryServiceImpl service = (SqlRepositoryServiceImpl) ctx.getBean("sqlRepositoryServiceImpl");

        Objects objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/1k-users.xml"));
        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();

        String userWithResult = null;

        long time = System.currentTimeMillis();
        String oid = null;
        for (int i = 0; i < elements.size(); i++) {
            JAXBElement<? extends ObjectType> element = elements.get(i);
            ObjectType object = element.getValue();

            if (i == elements.size() - 5) {
                oid = service.add(object);
            } else if (i == 0) {
                userWithResult = service.add(object);
            } else {
                service.add(object);
            }
        }
        System.out.println("XXX Time: " + (System.currentTimeMillis() - time) + ", oid: " + oid);

        time = System.currentTimeMillis();
        System.out.println(JAXBUtil.marshalWrap(service.getObject(UserType.class, oid, null, new OperationResult("a"))));
        System.out.println("XXX Time: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        System.out.println(JAXBUtil.marshalWrap(service.getObject(ObjectType.class, oid, null, new OperationResult("a"))));
        System.out.println("XXX Time: " + (System.currentTimeMillis() - time));

        System.out.println("user with result\n" +
                JAXBUtil.marshalWrap(service.getObject(UserType.class, userWithResult, null, new OperationResult("a"))));
        System.out.println("user with existing oid\n" +
                JAXBUtil.marshalWrap(service.getObject(UserType.class, "11111111-1111-1111-1111-111111111111", null,
                        new OperationResult("a"))));
    }
}
