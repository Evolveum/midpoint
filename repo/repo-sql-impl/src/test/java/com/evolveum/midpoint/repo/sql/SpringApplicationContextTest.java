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

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SpringApplicationContextTest {

    @Test
    public void initialize() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context-repo-sql.xml");
        SqlRepositoryServiceImpl service = (SqlRepositoryServiceImpl) ctx.getBean("sqlRepositoryServiceImpl");

        Objects objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/objects.xml"));
        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();
        List<String> oids = new ArrayList<String>();

        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            JAXBElement<? extends ObjectType> element = elements.get(i);
            ObjectType object = element.getValue();
            oids.add(service.add(object));
        }
        System.out.println("XXX Time: " + (System.currentTimeMillis() - time));

        int count = 0;

        objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/objects.xml"));
        for (int i = 0; i < elements.size(); i++) {
            JAXBElement<? extends ObjectType> element = elements.get(i);
            ObjectType object = element.getValue();
            object.setOid(oids.get(i));

            ObjectType type = service.getObject(ObjectType.class, oids.get(i), null, new OperationResult("R"));
            ObjectModificationType changes = CalculateXmlDiff.calculateChanges(type, object);
            if (changes.getPropertyModification().isEmpty()) {
                continue;
            }
            count += changes.getPropertyModification().size();
            System.out.println("Changes: " + (i + 1) + "\n" + type.getClass()
                    + "\n" + JAXBUtil.marshalWrap(changes) + "\n\n");
        }

        AssertJUnit.assertEquals("Expected changes must be 0. ", 0, count);
    }

    //    @Test
    public void perfTest() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context-repo-sql.xml");
        SqlRepositoryServiceImpl service = (SqlRepositoryServiceImpl) ctx.getBean("sqlRepositoryServiceImpl");

        Objects objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/50k-users.xml"));
        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();

        long time = System.currentTimeMillis();
        for (JAXBElement<? extends ObjectType> element : elements) {
            service.add(element.getValue());
        }
        time = (System.currentTimeMillis() - time);
        System.out.println("XXX Time: " + (time / 1000) + "s, that's " + (50000000 / time) + "/s");
    }
}
