/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import java.util.function.Predicate;

import static com.evolveum.midpoint.schema.util.FocusTypeUtil.determineSubTypes;

@Component
public class OrgStructFunctionsImpl implements OrgStructFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(OrgStructFunctionsImpl.class);
    private static final String CLASS_DOT = OrgStructFunctions.class.getName() + ".";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private MidpointFunctions midpointFunctions;

    /**
     * Returns a list of user's managers. Formally, for each Org O which this user has (any) relation to,
     * all managers of O are added to the result.
     *
     * Some customizations are probably necessary here, e.g. filter out project managers (keep only line managers),
     * or defining who is a manager of a user who is itself a manager in its org.unit. (A parent org unit manager,
     * perhaps.)
     *
     * @return list of oids of the respective managers
     */
    @Override
    public Collection<String> getManagersOids(UserType user, boolean preAuthorized) throws SchemaException,
            SecurityViolationException {
        Set<String> retval = new HashSet<>();
        for (UserType u : getManagers(user, preAuthorized)) {
            retval.add(u.getOid());
        }
        return retval;
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user, boolean preAuthorized) throws SchemaException,
            SecurityViolationException {
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
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Set<String> rv = new HashSet<>();
        for (ObjectReferenceType ref : userRefList) {
            UserType user = getObject(UserType.class, ref.getOid(), preAuthorized);
            rv.addAll(getManagersOidsExceptUser(user, preAuthorized));
        }
        return rv;
    }

    @Override
    public Collection<UserType> getManagers(UserType user, boolean preAuthorized) throws SchemaException,
            SecurityViolationException {
        return getManagers(user, null, false, preAuthorized);
    }

    @Override
    public Collection<UserType> getManagersByOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException,
            SecurityViolationException {
        return getManagers(user, orgType, false, preAuthorized);
    }

    @Override
    public Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf, boolean preAuthorized) throws SchemaException,
            SecurityViolationException {
        Set<UserType> retval = new HashSet<>();
        if (user == null) {
            return retval;
        }
        Collection<String> orgOids = getOrgUnits(user, null, preAuthorized);
        while (!orgOids.isEmpty()) {
            LOGGER.trace("orgOids: {}", orgOids);
            Collection<OrgType> thisLevelOrgs = new ArrayList<>();
            for (String orgOid : orgOids) {
                if (orgType != null) {
                    OrgType org = getOrgByOid(orgOid, preAuthorized);
                    if (org == null) {
                        continue;
                    }
                    if (!determineSubTypes(org).contains(orgType)) {
                        continue;
                    } else {
                        thisLevelOrgs.add(org);
                    }
                }
                Collection<UserType> managersOfOrg = getManagersOfOrg(orgOid, preAuthorized);
                LOGGER.trace("managersOfOrg {}: {}", orgOid, managersOfOrg);
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
            Collection<String> nextLevelOids = new ArrayList<>();
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
        Set<String> retval = new HashSet<>();
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
            if (prismContext.relationMatches(relation, orgRef.getRelation())) {
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
        } catch (CommunicationException|ConfigurationException|ExpressionEvaluationException e) {
            throw new SystemException("Couldn't get org: " + e.getMessage(), e);        // really shouldn't occur
        }
    }

    @Override
    public OrgType getOrgByName(String name, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        PolyString polyName = new PolyString(name);
        ObjectQuery q = ObjectQueryUtil.createNameQuery(polyName, prismContext);
        List<PrismObject<OrgType>> result = searchObjects(OrgType.class, q, midpointFunctions.getCurrentResult(), preAuthorized);
        if (result.isEmpty()) {
            return null;
        } else if (result.size() > 1) {
            throw new IllegalStateException("More than one organizational unit with the name '" + name + "' (there are " + result.size() + " of them)");
        } else {
            return result.get(0).asObjectable();
        }
    }

    @Override
    public OrgType getParentOrgByOrgType(ObjectType object, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        Collection<OrgType> parentOrgs = getParentOrgs(object, PrismConstants.Q_ANY, orgType, preAuthorized);
        if (parentOrgs.isEmpty()) {
            return null;
        } else if (parentOrgs.size() > 1) {
            throw new IllegalArgumentException("Expected that there will be just one parent org of type "+orgType+" for "+object+", but there were "+parentOrgs.size());
        } else {
            return parentOrgs.iterator().next();
        }
    }

    @Override
    public OrgType getParentOrgByArchetype(ObjectType object, String archetypeOid, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        Collection<OrgType> parentOrgs = getParentOrgs(
                object,
                PrismConstants.Q_ANY,
                org -> archetypeOid == null
                        || ObjectTypeUtil.hasArchetypeRef(org, archetypeOid),
                preAuthorized);
        if (parentOrgs.isEmpty()) {
            return null;
        } else if (parentOrgs.size() > 1) {
            throw new IllegalArgumentException("Expected that there will be just one parent org of archetype "+archetypeOid+" for "+object+", but there were "+parentOrgs.size());
        } else {
            return parentOrgs.iterator().next();
        }
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, relation, org -> true, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, relation, null, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, PrismConstants.Q_ANY, org -> true, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, new QName(null, relation), orgType, preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType, boolean preAuthorized)
            throws SchemaException, SecurityViolationException {
        return getParentOrgs(object, relation, org -> orgType == null || determineSubTypes(org).contains(orgType), preAuthorized);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, @NotNull Predicate<OrgType>predicate, boolean preAuthorized)
            throws SchemaException, SecurityViolationException {
        List<ObjectReferenceType> parentOrgRefs = object.getParentOrgRef();
        List<OrgType> parentOrgs = new ArrayList<>(parentOrgRefs.size());
        for (ObjectReferenceType parentOrgRef: parentOrgRefs) {
            if (!prismContext.relationMatches(relation, parentOrgRef.getRelation())) {
                continue;
            }
            OrgType parentOrg;
            try {
                parentOrg = getObject(OrgType.class, parentOrgRef.getOid(), preAuthorized);
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Org "+parentOrgRef.getOid()+" specified in parentOrgRef in "+object+" was not found: "+e.getMessage(), e);
                // but do not rethrow, just skip this
                continue;
            } catch (CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                // This should not happen.
                throw new SystemException(e.getMessage(), e);
            }
            if (predicate.test(parentOrg)) {
                parentOrgs.add(parentOrg);
            }
        }
        return parentOrgs;
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid, boolean preAuthorized) throws SchemaException, SecurityViolationException {
        Set<UserType> retval = new HashSet<>();
        OperationResult result = new OperationResult("getManagerOfOrg");

        ObjectQuery objectQuery = ObjectTypeUtil.createManagerQuery(ObjectType.class, orgOid, relationRegistry, prismContext);
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
            if (orgOid.equals(objectReferenceType.getOid()) &&
                    relationRegistry.isManager(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isManager(UserType user) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (relationRegistry.isManager(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isManagerOfOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (relationRegistry.isManager(objectReferenceType.getRelation())) {
                OrgType org = getOrgByOid(objectReferenceType.getOid(), preAuthorized);
                if (determineSubTypes(org).contains(orgType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public <T extends ObjectType> T getObject(Class<T> type, String oid, boolean preAuthorized) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<T> prismObject;
        if (preAuthorized) {
            prismObject = repositoryService.getObject(type, oid, null, midpointFunctions.getCurrentResult());
        } else {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createExecutionPhase());
            prismObject = modelService.getObject(type, oid, options, midpointFunctions.getCurrentTask(), midpointFunctions.getCurrentResult(CLASS_DOT + "getObject"));
        }
        return prismObject.asObjectable();
    }

    private <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> clazz, ObjectQuery query, OperationResult result, boolean preAuthorized)
            throws SchemaException, SecurityViolationException {
        if (preAuthorized) {
            return repositoryService.searchObjects(clazz, query, null, result);
        } else {
            try {
                Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createExecutionPhase());
                return modelService.searchObjects(clazz, query, options, midpointFunctions.getCurrentTask(), result);
            } catch (ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                throw new SystemException("Couldn't search objects: " + e.getMessage(), e);
            }
        }
    }
}
