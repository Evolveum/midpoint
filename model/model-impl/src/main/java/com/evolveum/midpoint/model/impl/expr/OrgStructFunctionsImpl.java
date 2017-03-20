/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
@Component
public class OrgStructFunctionsImpl implements OrgStructFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(OrgStructFunctionsImpl.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private PrismContext prismContext;

    /**
     * Returns a list of user's managers. Formally, for each Org O which this user has (any) relation to,
     * all managers of O are added to the result.
     *
     * Some customizations are probably necessary here, e.g. filter out project managers (keep only line managers),
     * or defining who is a manager of a user who is itself a manager in its org.unit. (A parent org unit manager,
     * perhaps.)
     *
     * @param user
     * @return list of oids of the respective managers
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    @Override
    public Collection<String> getManagersOids(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user, preAuthorized)) {
            retval.add(u.getOid());
        }
        return retval;
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        Set<String> retval = new HashSet<>();
        for (UserType u : getManagers(user, preAuthorized)) {
            if (!u.getOid().equals(user.getOid())) {
                retval.add(u.getOid());
            }
        }
        return retval;
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList, boolean preAuthorized)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        Set<String> rv = new HashSet<>();
		for (ObjectReferenceType ref : userRefList) {
			UserType user = getObject(UserType.class, ref.getOid(), preAuthorized);
			rv.addAll(getManagersOidsExceptUser(user, preAuthorized));
		}
		return rv;
    }

    @Override
    public Collection<UserType> getManagers(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return getManagers(user, null, false, preAuthorized);
    }

    @Override
    public Collection<UserType> getManagersByOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return getManagers(user, orgType, false, preAuthorized);
    }

    @Override
    public Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        Set<UserType> retval = new HashSet<UserType>();
        if (user == null) {
        	return retval;
        }
        Collection<String> orgOids = getOrgUnits(user, null, preAuthorized);
        while (!orgOids.isEmpty()) {
            LOGGER.trace("orgOids: {}", orgOids);
            Collection<OrgType> thisLevelOrgs = new ArrayList<OrgType>();
            for (String orgOid : orgOids) {
                if (orgType != null) {
                    OrgType org = getOrgByOid(orgOid, preAuthorized);
                    if (org == null || org.getOrgType() == null) {
                    	continue;
                    }
                    if (!org.getOrgType().contains(orgType)) {
                        continue;
                    } else {
                        thisLevelOrgs.add(org);
                    }
                }
                Collection<UserType> managersOfOrg = getManagersOfOrg(orgOid, preAuthorized);
                for (UserType managerOfOrg: managersOfOrg) {
                    if (allowSelf || !managerOfOrg.getOid().equals(user.getOid())) {
                        retval.add(managerOfOrg);
                    }
                }
            }
            LOGGER.trace("retval: {}", retval);
            if (!retval.isEmpty()) {
                return retval;
            }
            Collection<String> nextLevelOids = new ArrayList<String>();
            if (orgType == null) {
                for (String orgOid : orgOids) {
                    OrgType org = getOrgByOid(orgOid, preAuthorized);
                    if (org != null) {
                        thisLevelOrgs.add(org);
                    }
                }
            }
            for (OrgType org: thisLevelOrgs) {
                for (ObjectReferenceType parentOrgRef: org.getParentOrgRef()) {
                    if (!nextLevelOids.contains(parentOrgRef.getOid())) {
                        nextLevelOids.add(parentOrgRef.getOid());
                    }
                }
            }
            LOGGER.trace("nextLevelOids: {}",nextLevelOids);
            orgOids = nextLevelOids;
        }
        return retval;
    }

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    @Override
    public Collection<String> getOrgUnits(UserType user, boolean preAuthorized) {
        Set<String> retval = new HashSet<String>();
        if (user == null){
            return retval;
        }
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            retval.add(orgRef.getOid());
        }
        return retval;
    }

    @Override
    public Collection<String> getOrgUnits(UserType user, QName relation, boolean preAuthorized) {
        Set<String> retval = new HashSet<>();
        if (user == null) {
            return retval;
        }
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            if (ObjectTypeUtil.relationMatches(relation, orgRef.getRelation())) {
                retval.add(orgRef.getOid());
            }
        }
        return retval;
    }

    @Override
    public OrgType getOrgByOid(String oid, boolean preAuthorized) throws SchemaException {
        try {
            return getObject(OrgType.class, oid, preAuthorized);
        } catch (ObjectNotFoundException|SecurityViolationException e) {
            return null;
        } catch (CommunicationException|ConfigurationException e) {
            throw new SystemException("Couldn't get org: " + e.getMessage(), e);        // really shouldn't occur
        }
    }

    @Override
    public OrgType getOrgByName(String name, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        PolyString polyName = new PolyString(name);
        ObjectQuery q = ObjectQueryUtil.createNameQuery(polyName, prismContext);
        List<PrismObject<OrgType>> result = searchObjects(OrgType.class, q, getCurrentResult(), preAuthorized);
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            throw new IllegalStateException("More than one organizational unit with the name '" + name + "' (there are " + result.size() + " of them)");
        }
        return result.get(0).asObjectable();
    }

    @Override
    public OrgType getParentOrgByOrgType(ObjectType object, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        Collection<OrgType> parentOrgs = getParentOrgs(object, PrismConstants.Q_ANY, orgType, preAuthorized);
        if (parentOrgs.isEmpty()) {
            return null;
        }
        if (parentOrgs.size() > 1) {
            throw new IllegalArgumentException("Expected that there will be just one parent org of type "+orgType+" for "+object+", but there were "+parentOrgs.size());
        }
        return parentOrgs.iterator().next();
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, relation, null, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, relation, null, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, PrismConstants.Q_ANY, null, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, new QName(null, relation), orgType, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        List<ObjectReferenceType> parentOrgRefs = object.getParentOrgRef();
        List<OrgType> parentOrgs = new ArrayList<>(parentOrgRefs.size());
        for (ObjectReferenceType parentOrgRef: parentOrgRefs) {
            if (!ObjectTypeUtil.relationMatches(relation, parentOrgRef.getRelation())) {
            	continue;
			}
            OrgType parentOrg;
            try {
                parentOrg = getObject(OrgType.class, parentOrgRef.getOid(), preAuthorized);
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Org "+parentOrgRef.getOid()+" specified in parentOrgRef in "+object+" was not found: "+e.getMessage(), e);
                // but do not rethrow, just skip this
                continue;
            } catch (CommunicationException | ConfigurationException e) {
                // This should not happen.
                throw new SystemException(e.getMessage(), e);
            }
            if (orgType == null || parentOrg.getOrgType().contains(orgType)) {
                parentOrgs.add(parentOrg);
            }
        }
        return parentOrgs;
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        Set<UserType> retval = new HashSet<UserType>();
        OperationResult result = new OperationResult("getManagerOfOrg");

        PrismReferenceValue parentOrgRefVal = new PrismReferenceValue(orgOid, OrgType.COMPLEX_TYPE);
        parentOrgRefVal.setRelation(SchemaConstants.ORG_MANAGER);
        ObjectQuery objectQuery = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .item(ObjectType.F_PARENT_ORG_REF).ref(parentOrgRefVal)
                .build();

        List<PrismObject<ObjectType>> members = searchObjects(ObjectType.class, objectQuery, result, preAuthorized);
        for (PrismObject<ObjectType> member : members) {
            if (member.asObjectable() instanceof UserType) {
                UserType user = (UserType) member.asObjectable();
                retval.add(user);
            }
        }
        return retval;
    }

    @Override
    public boolean isManagerOf(UserType user, String orgOid, boolean preAuthorized) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid()) && ObjectTypeUtil.isManagerRelation(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isManager(UserType user) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (ObjectTypeUtil.isManagerRelation(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isManagerOfOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (ObjectTypeUtil.isManagerRelation(objectReferenceType.getRelation())) {
                OrgType org = getOrgByOid(objectReferenceType.getOid(), preAuthorized);
                if (org.getOrgType().contains(orgType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public <T extends ObjectType> T getObject(Class<T> type, String oid, boolean preAuthorized) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<T> prismObject;
        if (preAuthorized) {
            prismObject = repositoryService.getObject(type, oid, null, getCurrentResult());
        } else {
            prismObject = modelService.getObject(type, oid, null, getCurrentTask(), getCurrentResult());
        }
        return prismObject.asObjectable();
    }

    private <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> clazz, ObjectQuery query, OperationResult result, boolean preAuthorized)
            throws SchemaException, SecurityViolationException {
        if (preAuthorized) {
            return repositoryService.searchObjects(clazz, query, null, result);
        } else {
            try {
                return modelService.searchObjects(clazz, query, null, getCurrentTask(), result);
            } catch (ObjectNotFoundException|CommunicationException|ConfigurationException e) {
                throw new SystemException("Couldn't search objects: " + e.getMessage(), e);
            }
        }
    }

    private Task getCurrentTask() {
        return ModelExpressionThreadLocalHolder.getCurrentTask();
    }

    private OperationResult getCurrentResult() {
        return ModelExpressionThreadLocalHolder.getCurrentResult();
    }
}
