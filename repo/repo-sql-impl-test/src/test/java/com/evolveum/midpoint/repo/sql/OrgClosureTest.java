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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.util.Date;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    //50531 OU, 810155 U
//    private static final int[] TREE_LEVELS = {1, 5, 5, 20, 20, 4};
//    private static final int[] TREE_LEVELS_USERS = {5, 10, 4, 20, 20, 15};

    //1191 OU, 10943 U  =>  428585 queries ~ 6min, h2
//    private static final int[] TREE_LEVELS = {1, 5, 3, 3, 5, 4};
//    private static final int[] TREE_LEVELS_USERS = {3, 4, 5, 6, 7, 10};

    //9 OU, 23 U        =>  773 queries ~ 50s, h2
    private static final int[] TREE_LEVELS = {1, 2, 3};
    private static final int[] TREE_LEVELS_USERS = {1, 2, 3};

    private int count = 0;

    @Test(enabled = false)
    public void loadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        LOGGER.info("Start.");
        loadOrgStructure(null, TREE_LEVELS, TREE_LEVELS_USERS, "", opResult);
        LOGGER.info("Finish.");
    }

    private void loadOrgStructure(String parentOid, int[] TREE_SIZE, int[] USER_SIZE, String oidPrefix,
                                  OperationResult result) throws Exception {
        if (TREE_SIZE.length == 0) {
            return;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String newOidPrefix = (TREE_SIZE[0] - i) + "a" + oidPrefix;
            PrismObject<OrgType> org = createOrg(parentOid, i, newOidPrefix);
            LOGGER.info("Creating {}, total {}", org, count);
            String oid = repositoryService.addObject(org, null, result);
            count++;

            for (int u = 0; u < USER_SIZE[0]; u++) {
                PrismObject<UserType> user = createUser(oid, i, u, newOidPrefix);
                repositoryService.addObject(user, null, result);
                count++;
            }

            loadOrgStructure(oid, ArrayUtils.remove(TREE_SIZE, 0), ArrayUtils.remove(USER_SIZE, 0),
                    newOidPrefix + i, result);
        }
    }

    private PrismObject<UserType> createUser(String parentOid, int i, int u, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("1" + createOid(u, oidPrefix + i));
        user.setName(createPolyString("u" + oidPrefix + i + u));
        user.setFullName(createPolyString("fu" + oidPrefix + i + u));
        user.setFamilyName(createPolyString("fa" + oidPrefix + i + u));
        user.setGivenName(createPolyString("gi" + oidPrefix + i + u));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            user.getParentOrgRef().add(ref);
        }

        PrismObject<UserType> object = user.asPrismObject();
        prismContext.adopt(user);

        addExtensionProperty(object, "shipName", "Ship " + i + "-" + u);
        addExtensionProperty(object, "weapon", "weapon " + i + "-" + u);
        addExtensionProperty(object, "loot", i + u);
        addExtensionProperty(object, "funeralDate", XMLGregorianCalendarType.asXMLGregorianCalendar(new Date()));

        return object;
    }

    private void addExtensionProperty(PrismObject object, String name, Object value) throws SchemaException {
        String NS = "http://example.com/p";
        PrismProperty p = object.findOrCreateProperty(new ItemPath(UserType.F_EXTENSION, new QName(NS, name)));
        p.setRealValue(value);
    }

    private PrismObject<OrgType> createOrg(String parentOid, int i, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("2" + createOid(i, oidPrefix));
        org.setDisplayName(createPolyString("o" + oidPrefix + i));
        org.setName(createPolyString("o" + oidPrefix + i));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    private String createOid(int i, String oidPrefix) {
        String oid = StringUtils.rightPad(oidPrefix + Integer.toString(i), 31, 'a');

        StringBuilder sb = new StringBuilder();
        sb.append(oid.substring(0, 7));
        sb.append('-');
        sb.append(oid.substring(7, 11));
        sb.append('-');
        sb.append(oid.substring(11, 15));
        sb.append('-');
        sb.append(oid.substring(15, 19));
        sb.append('-');
        sb.append(oid.substring(19, 31));

        return sb.toString();
    }

    private PolyStringType createPolyString(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }
}
