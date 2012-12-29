/**
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
package com.evolveum.midpoint.model.expr;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 *
 */
@Component
public class MidpointFunctions {

	@Autowired(required=true)
	private PrismContext prismContext;
	
	@Autowired(required=true)
	private ModelService modelService;

    @Autowired(required=true)
    private RepositoryService repositoryService;

	public String hello(String name) {
		return "Hello "+name;
	}

    public List<String> toList(String... s) {
        return Arrays.asList(s);
    }

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
    public Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            retval.add(u.getOid());
        }
        return retval;
    }

    public Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            if (!u.getOid().equals(user.getOid())) {
                retval.add(u.getOid());
            }
        }
        return retval;
    }

    public Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<UserType> retval = new HashSet<UserType>();
        Collection<String> orgOids = getOrgUnits(user);
        for (String orgOid : orgOids) {
            retval.addAll(getManagersOfOrg(orgOid));
        }
        return retval;
    }

    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, new OperationResult("getUserByOid")).asObjectable();
    }

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    public Collection<String> getOrgUnits(UserType user) {
        Set<String> retval = new HashSet<String>();
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            retval.add(orgRef.getOid());
        }
        return retval;
    }

    public OrgType getOrgByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(OrgType.class, oid, new OperationResult("getOrgByOid")).asObjectable();
    }

    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException {
        Set<UserType> retval = new HashSet<UserType>();
        OperationResult result = new OperationResult("getManagerOfOrg");
        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(orgOid, null, 1));
        List<PrismObject<ObjectType>> members = repositoryService.searchObjects(ObjectType.class, objectQuery, result);
        for (PrismObject<ObjectType> member : members) {
            if (member.asObjectable() instanceof UserType) {
                UserType user = (UserType) member.asObjectable();
                if (isManagerOf(user, orgOid)) {
                    retval.add(user);
                }
            }
        }
        return retval;
    }

    public boolean isManagerOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid()) && SchemaConstants.ORG_MANAGER.equals(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }
}
