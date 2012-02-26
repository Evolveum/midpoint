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

import com.evolveum.midpoint.repo.api.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.*;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml",
        "application-context-configuration-sql-test.xml"})
public class SpringApplicationContextTest extends AbstractTestNGSpringContextTests {

    @Autowired(required=true)
    private RepositoryService repositoryService;

    @Test
    public void initApplicationContext() {
        assertNotNull(repositoryService);
    }

//    public void initialize() throws Exception {
//        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context-repository.xml");
//        SqlRepositoryServiceImpl service = (SqlRepositoryServiceImpl) ctx.getBean("sqlRepositoryServiceImpl");
//
//        Objects objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/objects.xml"));
//        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();
//        List<String> oids = new ArrayList<String>();
//
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < elements.size(); i++) {
//            JAXBElement<? extends ObjectType> element = elements.get(i);
//            ObjectType object = element.getValue();
//            oids.add(service.addObject(object, new OperationResult("a")));
//        }
//        System.out.println("XXX Time: " + (System.currentTimeMillis() - time));
//
//        int count = 0;
//
//        objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/objects.xml"));
//        for (int i = 0; i < elements.size(); i++) {
//            JAXBElement<? extends ObjectType> element = elements.get(i);
//            ObjectType object = element.getValue();
//            object.setTarget(oids.get(i));
//
//            ObjectType type = service.getObject(ObjectType.class, oids.get(i), null, new OperationResult("R"));
//            ObjectModificationType changes = CalculateXmlDiff.calculateChanges(type, object);
//            if (changes.getPropertyModification().isEmpty()) {
//                continue;
//            }
//            count += changes.getPropertyModification().size();
//            System.out.println("Changes: " + (i + 1) + "\n" + type.getClass()
//                    + "\n" + JAXBUtil.marshalWrap(changes) + "\n\n");
//        }
//
//        ResultList<UserType> list = service.listObjects(UserType.class, PagingTypeFactory.createPaging(1, 2,
//                OrderDirectionType.ASCENDING, "name"), new OperationResult("a"));
//        System.out.println(list.getTotalResultCount() + "\n" + list);
//
//        UserType user = service.listAccountShadowOwner("1234", new OperationResult("a"));
//        System.out.println(JAXBUtil.marshalWrap(user));
//
//        System.out.println("*******************");
//        System.out.println(JAXBUtil.marshalWrap(service.getObject(TaskType.class, "555", null, new OperationResult("a"))));
//        service.claimTask("555", new OperationResult("r"));
//        System.out.println(JAXBUtil.marshalWrap(service.getObject(TaskType.class, "555", null, new OperationResult("a"))));
//        service.releaseTask("555", new OperationResult("r"));
//        System.out.println(JAXBUtil.marshalWrap(service.getObject(TaskType.class, "555", null, new OperationResult("a"))));
//        System.out.println("*******************");
//
//       ResultList<? extends ResourceObjectShadowType> shadows = service.listResourceObjectShadows(
//               "ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", AccountShadowType.class, new OperationResult("a"));
//        System.out.println(shadows.getTotalResultCount() + "\n" + shadows);
//
//
//        System.out.println(JAXBUtil.marshalWrap(service.getObject(GenericObjectType.class, "9999", null, new OperationResult("a"))));
//
//        System.out.println("Expected changes must be 0, but was: " + count);
////        AssertJUnit.assertEquals("Expected changes must be 0. ", 0, count);
//    }
//
//    //    @Test
//    public void perfTest() throws Exception {
//        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context-repo-sql.xml");
//        SqlRepositoryServiceImpl service = (SqlRepositoryServiceImpl) ctx.getBean("sqlRepositoryServiceImpl");
//
//        Objects objects = (Objects) JAXBUtil.unmarshal(new File("./src/test/resources/50k-users.xml"));
//        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();
//
//        long time = System.currentTimeMillis();
//        for (JAXBElement<? extends ObjectType> element : elements) {
//            service.addObject(element.getValue(), new OperationResult("a"));
//        }
//        time = (System.currentTimeMillis() - time);
//        System.out.println("XXX Time: " + (time / 1000) + "s, that's " + (50000000 / time) + "/s");
//    }
}
