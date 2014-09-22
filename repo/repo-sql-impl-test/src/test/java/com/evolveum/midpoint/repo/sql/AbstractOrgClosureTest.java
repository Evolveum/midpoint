/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author lazyman
 */
public class AbstractOrgClosureTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractOrgClosureTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    protected int count = 0;

    protected List<String> rootOids = new ArrayList<>();

    protected List<OrgType> allOrgCreated = new ArrayList<>();

    protected List<List<String>> orgsByLevels = new ArrayList<>();

    protected List<List<String>> usersByLevels = new ArrayList<>();

    protected SimpleDirectedGraph<String, DefaultEdge> orgGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    protected Session session;            // used exclusively for read-only operations

    protected void openSessionIfNeeded() {
        if (session == null || !session.isConnected()) {
            session = repositoryService.getSessionFactory().openSession();
        }
    }

    protected void checkClosure(Set<String> oidsToCheck) {
        openSessionIfNeeded();
        SimpleDirectedGraph<String,DefaultEdge> tc = (SimpleDirectedGraph) orgGraph.clone();
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(tc);
        for (String subroot : oidsToCheck) {
            LOGGER.info("Checking descendants of {}", subroot);
            Set<String> expectedChildren = new HashSet<>();
            for (DefaultEdge edge : tc.incomingEdgesOf(subroot)) {
                expectedChildren.add(tc.getEdgeSource(edge));
            }
            expectedChildren.add(subroot);
            LOGGER.trace("Expected children: {}", expectedChildren);
            Set<String> actualChildren = getActualChildrenOf(subroot);
            LOGGER.trace("Actual children: {}", actualChildren);

            Set<String> expectedMinusActual = new HashSet<>(expectedChildren);
            expectedMinusActual.removeAll(actualChildren);
            if (!expectedMinusActual.isEmpty()) {
                System.out.println("Expected-Actual = " + expectedMinusActual);
            }
            Set<String> actualMinusExpected = new HashSet<>(actualChildren);
            actualMinusExpected.removeAll(expectedChildren);
            if (!actualMinusExpected.isEmpty()) {
                System.out.println("Actual-Expected = " + actualMinusExpected);
            }
            assertEquals("Incorrect children for " + subroot, expectedChildren, actualChildren);
        }
    }

    protected Set<String> getActualChildrenOf(String ancestor) {
        List<ROrgClosure> descendantRecords = getOrgClosureByAncestor(ancestor);
        Set<String> rv = new HashSet<String>();
        for (ROrgClosure c : descendantRecords) {
            rv.add(c.getDescendantOid());
        }
        return rv;
    }

    private List<ROrgClosure> getOrgClosureByDescendant(String descendantOid) {
        Query query = session.createQuery("from ROrgClosure where descendantOid=:oid");
        query.setString("oid", descendantOid);
        return query.list();
    }

    private List<ROrgClosure> getOrgClosureByAncestor(String ancestorOid) {
        Query query = session.createQuery("from ROrgClosure where ancestorOid=:oid");
        query.setString("oid", ancestorOid);
        return query.list();
    }

    protected void removeOrgParent(OrgType org, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta removeParent = ReferenceDelta.createModificationDelete(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(removeParent);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.removeEdge(org.getOid(), existingValue.getOid());
    }

    protected void addOrgParent(OrgType org, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta readdParent = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(readdParent);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.addEdge(org.getOid(), existingValue.getOid());
    }

    protected void removeOrg(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(OrgType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    protected void removeUser(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(UserType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    protected void reAddOrg(OrgType org, OperationResult opResult) throws Exception {
        repositoryService.addObject(org.asPrismObject(), null, opResult);
        registerObject(org, true);
    }

    protected void reAddUser(UserType user, OperationResult opResult) throws Exception {
        repositoryService.addObject(user.asPrismObject(), null, opResult);
        registerObject(user, false);
    }

    // parentsInLevel may be null (in that case, a simple tree is generated)
    protected void loadOrgStructure(int level, String parentOid, int[] orgChildrenInLevel, int[] userChildrenInLevel, int[] parentsInLevel, String oidPrefix,
                                  OperationResult result) throws Exception {
        if (level == orgChildrenInLevel.length) {
            return;
        }

        List<String> orgsAtThisLevel;
        if (orgsByLevels.size() == level) {
            orgsAtThisLevel = new ArrayList<>();
            orgsByLevels.add(orgsAtThisLevel);
        } else {
            orgsAtThisLevel = orgsByLevels.get(level);
        }

        for (int i = 0; i < orgChildrenInLevel[level]; i++) {
            String newOidPrefix = getOidCharFor(i) + oidPrefix;
            int numberOfParents = parentsInLevel==null ? (parentOid != null ? 1 : 0) : parentsInLevel[level];
            PrismObject<OrgType> org = createOrg(generateParentsForLevel(parentOid, level, numberOfParents), newOidPrefix);
            LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{org, count, getParentsOids(org)});
            String oid = repositoryService.addObject(org, null, result);
            org.setOid(oid);
            if (parentOid == null) {
                rootOids.add(oid);
            }
            allOrgCreated.add(org.asObjectable());
            registerObject(org.asObjectable(), false);
            orgsAtThisLevel.add(oid);
            count++;

            loadOrgStructure(level+1, oid, orgChildrenInLevel, userChildrenInLevel, parentsInLevel, newOidPrefix, result);
        }

        if (parentOid != null) {

            while (usersByLevels.size() <= level) {
                usersByLevels.add(new ArrayList<String>());
            }
            List<String> usersAtThisLevel = usersByLevels.get(level);

            for (int u = 0; u < userChildrenInLevel[level]; u++) {
                int numberOfParents = parentsInLevel==null ? 1 : parentsInLevel[level];
                PrismObject<UserType> user = createUser(generateParentsForLevel(parentOid, level, numberOfParents), getOidCharFor(u) + ":" + oidPrefix);
                LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{user, count, getParentsOids(user)});
                String uoid = repositoryService.addObject(user, null, result);
                user.setOid(uoid);
                registerObject(user.asObjectable(), false);
                usersAtThisLevel.add(uoid);
                count++;
            }
        }

    }

    private static String SPECIAL="!@#$%^&*()";
    private char getOidCharFor(int i) {
        if (i < 10) {
            return (char) ('0'+i);
        } else if (i < 36) {
            return (char) ('A'+i-10);
        } else if (i < 46) {
            return SPECIAL.charAt(i-36);
        } else {
            throw new IllegalArgumentException("Too many items in a level: " + i);
        }
    }

    protected Collection<String> getParentsOids(PrismObject<? extends ObjectType> object) {
        List<String> retval = new ArrayList<String>();
        for(ObjectReferenceType objectReferenceType : object.asObjectable().getParentOrgRef()) {
            retval.add(objectReferenceType.getOid());
        }
        return retval;
    }

    private List<String> generateParentsForLevel(String explicitParentOid, int level, int totalParents) {
        List<String> rv = new ArrayList<>();
        if (totalParents == 0) {
            return rv;
        }
        List<String> potentialParents = level > 0 ? new ArrayList<String>(orgsByLevels.get(level-1)) : new ArrayList<String>();
        if (explicitParentOid != null) {
            rv.add(explicitParentOid);
            potentialParents.remove(explicitParentOid);
            totalParents--;
        }
        while (totalParents > 0 && !potentialParents.isEmpty()) {
            int i = (int) Math.floor(Math.random()*potentialParents.size());
            rv.add(potentialParents.get(i));
            potentialParents.remove(i);
            totalParents--;
        }
        return rv;
    }

    protected void registerObject(ObjectType objectType, boolean registerChildrenLinks) {
        String oid = objectType.getOid();
        orgGraph.addVertex(oid);
        for (ObjectReferenceType ort : objectType.getParentOrgRef()) {
            orgGraph.addEdge(oid, ort.getOid());
        }

        if (registerChildrenLinks) {
            // let's check for existing children
            List<String> children = getChildren(oid);
            for (String child : children) {
                orgGraph.addEdge(child, oid);
            }
        }
    }

    protected List<String> getChildren(String oid) {
        Query childrenQuery = session.createQuery("select distinct ownerOid from RParentOrgRef where targetOid=:oid");
        childrenQuery.setString("oid", oid);
        return childrenQuery.list();
    }


    protected void removeOrgStructure(OperationResult result) throws Exception {
        for (String rootOid : rootOids) {
            removeOrgStructure(rootOid, result);
        }
    }

    protected void removeOrgStructure(String nodeOid, OperationResult result) throws Exception {
        removeUsersFromOrg(nodeOid, result);
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
        List<PrismObject<OrgType>> subOrgs = repositoryService.searchObjects(OrgType.class, query, null, result);
        for (PrismObject<OrgType> subOrg : subOrgs) {
            removeOrgStructure(subOrg.getOid(), result);
        }
        repositoryService.deleteObject(OrgType.class, nodeOid, result);
        LOGGER.trace("Org " + nodeOid + " was removed");
    }

    protected void removeUsersFromOrg(String nodeOid, OperationResult result) throws Exception {
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        for (PrismObject<UserType> user : users) {
            repositoryService.deleteObject(UserType.class, user.getOid(), result);
            LOGGER.trace("User " + user.getOid() + " was removed");
        }
    }

    protected PrismObject<UserType> createUser(List<String> parentOids, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("u" + createOid(oidPrefix));
        user.setName(createPolyString("u" + oidPrefix));
        user.setFullName(createPolyString("fu" + oidPrefix));
        user.setFamilyName(createPolyString("fa" + oidPrefix));
        user.setGivenName(createPolyString("gi" + oidPrefix));
        if (parentOids != null) {
            for (String parentOid : parentOids) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(parentOid);
                ref.setType(OrgType.COMPLEX_TYPE);
                user.getParentOrgRef().add(ref);
            }
        }

        PrismObject<UserType> object = user.asPrismObject();
        prismContext.adopt(user);

        addExtensionProperty(object, "shipName", "Ship " + oidPrefix);
        addExtensionProperty(object, "weapon", "weapon " + oidPrefix);
        //addExtensionProperty(object, "loot", oidPrefix);
        addExtensionProperty(object, "funeralDate", XMLGregorianCalendarType.asXMLGregorianCalendar(new Date()));

        return object;
    }

    protected void addExtensionProperty(PrismObject object, String name, Object value) throws SchemaException {
        String NS = "http://example.com/p";
        PrismProperty p = object.findOrCreateProperty(new ItemPath(UserType.F_EXTENSION, new QName(NS, name)));
        p.setRealValue(value);
    }

    protected PrismObject<OrgType> createOrg(List<String> parentOids, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("o" + createOid(oidPrefix));
        org.setDisplayName(createPolyString("o" + oidPrefix));
        org.setName(createPolyString("o" + oidPrefix));
        if (parentOids != null) {
            for (String parentOid : parentOids) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(parentOid);
                ref.setType(OrgType.COMPLEX_TYPE);
                org.getParentOrgRef().add(ref);
            }
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    protected String createOid(String oidPrefix) {
        String oid = StringUtils.rightPad(oidPrefix, 31, '.');

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

    protected PolyStringType createPolyString(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }
}
