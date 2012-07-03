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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../application-context-sql-no-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class AddGetObjectTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_DIR = new File("src/test/resources/");
	
    private static final Trace LOGGER = TraceManager.getTrace(AddGetObjectTest.class);
	
    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;
    @Autowired
    SessionFactory factory;

    @Test(enabled = false)
    public <T extends ObjectType> void perfTest() throws Exception {
        Statistics stats = factory.getStatistics();
        stats.setStatisticsEnabled(true);

        final File OBJECTS_FILE = new File("./src/test/resources/10k-users.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(OBJECTS_FILE);

        long previousCycle = 0;
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            if (i % 500 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{
                        (System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            PrismObject<T> object = (PrismObject<T>) elements.get(i);
            repositoryService.addObject(object, new OperationResult("add performance test"));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(), (System.currentTimeMillis() - time)});

        stats.logSummary();
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void addSameName() throws Exception {
        final File user = new File("./src/test/resources/objects-user.xml");
        addGetCompare(user);
        addGetCompare(user);
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void addGetDSEESyncDoubleTest() throws Exception {
        final File OBJECTS_FILE = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!OBJECTS_FILE.exists()) {
            LOGGER.warn("skipping addGetDSEESyncDoubleTest, file {} not found.",
                    new Object[]{OBJECTS_FILE.getPath()});
            throw new ObjectAlreadyExistsException();
        }
        addGetCompare(OBJECTS_FILE);
        addGetCompare(OBJECTS_FILE);
    }

    @Test
    public void simpleAddGetTest() throws Exception {
        final File OBJECTS_FILE = new File("./src/test/resources/objects.xml");
        addGetCompare(OBJECTS_FILE);
    }

    private void addGetCompare(File file) throws Exception {
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{(i + 1), object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, result));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(), (System.currentTimeMillis() - time),});

        int count = 0;
        elements = prismContext.getPrismDomProcessor().parseObjects(file);
        for (int i = 0; i < elements.size(); i++) {
            try {
                PrismObject object = elements.get(i);
                object.asObjectable().setOid(oids.get(i));

                Class<? extends ObjectType> clazz = object.getCompileTimeClass();
                PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), result);
                ObjectDelta delta = object.diff(newObject);
                if (delta == null) {
                    continue;
                }

                count += delta.getModifications().size();
                if (delta.getModifications().size() > 0) {
                    LOGGER.error(">>> {} Found {} changes for {}\n{}", new Object[]{(i + 1), delta.getModifications().size(),
                            newObject.toString(), delta.debugDump(3)});
                    LOGGER.error("{}", prismContext.getPrismDomProcessor().serializeObjectToString(newObject));
                }
            } catch (Exception ex) {
                LOGGER.error("Exception occured", ex);
            }
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }

    @Test
    public void addUserWithAssignmentExtension() throws Exception {
        LOGGER.info("===[ addUserWithAssignmentExtension ]===");
        File file = new File("./src/test/resources/user-assignment-extension.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject((PrismObject) elements.get(0), result);

        PrismObject<UserType> fileUser = (PrismObject<UserType>)
                prismContext.getPrismDomProcessor().parseObjects(file).get(0);
        int id = 1;
        for (AssignmentType assignment: fileUser.asObjectable().getAssignment()) {
            assignment.setId(Integer.toString(id));
            id++;
        }

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, oid, result);

        ObjectDelta<UserType> delta = fileUser.diff(repoUser);
        AssertJUnit.assertNotNull(delta);
        LOGGER.info("delta\n{}", new Object[]{delta.debugDump(3)});
        AssertJUnit.assertTrue(delta.isEmpty());
    }
    
    /**
     * Attempt to store full account in the repo and then get it out again.
     * The potential problem is that there are attributes that do not have a fixed (static) definition.
     */
    @Test
    public void addGetFullAccount() throws Exception {
        LOGGER.info("===[ addGetFullAccount ]===");
        File file = new File("./src/test/resources/account-full.xml");
        PrismObject<AccountShadowType> fileAccount = prismContext.parseObject(new File(TEST_DIR, "account-full.xml"));
        
        // apply appropriate schema
        PrismObject<ResourceType> resource = prismContext.parseObject(new File(TEST_DIR, "resource-opendj.xml"));
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        ResourceObjectShadowUtil.applyResourceSchema(fileAccount, resourceSchema);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject(fileAccount, result);

        PrismObject<AccountShadowType> repoAccount = repositoryService.getObject(AccountShadowType.class, oid, result);

        ObjectDelta<AccountShadowType> delta = fileAccount.diff(repoAccount);
        AssertJUnit.assertNotNull(delta);
        LOGGER.info("delta\n{}", new Object[]{delta.debugDump(3)});
        AssertJUnit.assertTrue(delta.isEmpty());
    }
    
    @Test
    public void addOrgStriuctObjects() throws Exception{
    	
    	String orgF0001oid = "00000000-8888-6666-0000-100000000001";
    	String pRootOid = "00000000-8888-6666-0000-200000000000";
    	String userElaineName = "elaine";
    	
    	LOGGER.info("===[ addOrgStruct ]===");
    	File file = new File("./../../samples/objects/org-monkey-island.xml");
    	List<PrismObject<? extends Objectable>> orgStruct = prismContext.getPrismDomProcessor().parseObjects(file);
    	
    	OperationResult opResult = new OperationResult("ADD (org struct)");
    	
    	for (PrismObject o : orgStruct){
    		repositoryService.addObject(o, opResult);
    	}
    	
    	List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, null, null, opResult);
    	AssertJUnit.assertNotNull(orgTypes);
    	AssertJUnit.assertEquals(9, orgTypes.size());
    	
    		
    	OrgType orgF001 = repositoryService.getObject(OrgType.class, orgF0001oid, opResult).asObjectable();
    	AssertJUnit.assertNotNull(orgF001);
    	AssertJUnit.assertEquals("F0001", orgF001.getName());
    	AssertJUnit.assertEquals("The office of the most respectful Governor.", orgF001.getDescription());
   
    	PrismAsserts.assertEqualsPolyString("Governor Office", "Governor Office", orgF001.getDisplayName());
//    	AssertJUnit.assertEquals("Governor Office", orgF001.getDisplayName());
    	AssertJUnit.assertEquals("0001", orgF001.getIdentifier());
    	AssertJUnit.assertEquals(1, orgF001.getOrgType().size());
    	AssertJUnit.assertEquals("functional", orgF001.getOrgType().get(0));
    	AssertJUnit.assertEquals("CC0", orgF001.getCostCenter());
    	PrismAsserts.assertEqualsPolyString("The Governor's Mansion", "The Governor's Mansion", orgF001.getLocality());
    	
		
    	OrgType pRoot = repositoryService.getObject(OrgType.class, pRootOid, opResult).asObjectable();
    	AssertJUnit.assertEquals("PRoot", pRoot.getName());
    	AssertJUnit.assertEquals("Project organizational structure root", pRoot.getDescription());
    	PrismAsserts.assertEqualsPolyString("Projects", "Projects", pRoot.getDisplayName());
    	AssertJUnit.assertEquals(1, pRoot.getOrgType().size());
    	AssertJUnit.assertEquals("project", pRoot.getOrgType().get(0));
	
    	QueryType query = QueryUtil.createNameQuery(userElaineName);
    	
    	List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);
    	
    	AssertJUnit.assertNotNull(users);
    	AssertJUnit.assertEquals(1, users.size());
    	UserType elaine = users.get(0).asObjectable();
    	LOGGER.info("elaine");
    	LOGGER.info(prismContext.silentMarshalObject(elaine));
    	AssertJUnit.assertEquals("elaine", elaine.getName());
    	AssertJUnit.assertEquals(1, elaine.getOrgRef().size());
    	AssertJUnit.assertEquals("00000000-8888-6666-0000-100000000001", elaine.getOrgRef().get(0).getOid());
    	AssertJUnit.assertEquals("manager", elaine.getOrgRef().get(0).getRelation().getLocalPart());
    	PrismAsserts.assertEqualsPolyString("Elaine Marley", "Elaine Marley", elaine.getFullName());
    	PrismAsserts.assertEqualsPolyString("Marley", "Marley", elaine.getFamilyName());
    	PrismAsserts.assertEqualsPolyString("Elaine", "Elaine", elaine.getGivenName());
    	PrismAsserts.assertEqualsPolyString("Governor", "Governor", elaine.getTitle());
    	
    	
    }
    

}
