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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;

import java.util.*;

/**
 * @author semancik
 *
 */
@Component
public class MidpointFunctionsImpl implements MidpointFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointFunctionsImpl.class);

    @Autowired(required=true)
    private PrismContext prismContext;

    @Autowired(required=true)
    private ModelService modelService;

    @Autowired(required=true)
    private RepositoryService repositoryService;

    @Autowired(required = true)
    private transient Protector protector;

    public String hello(String name) {
        return "Hello "+name;
    }

    @Override
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
    @Override
    public Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            retval.add(u.getOid());
        }
        return retval;
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<String> retval = new HashSet<String>();
        for (UserType u : getManagers(user)) {
            if (!u.getOid().equals(user.getOid())) {
                retval.add(u.getOid());
            }
        }
        return retval;
    }

    @Override
    public Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException {
        Set<UserType> retval = new HashSet<UserType>();
        Collection<String> orgOids = getOrgUnits(user);
        for (String orgOid : orgOids) {
            retval.addAll(getManagersOfOrg(orgOid));
        }
        return retval;
    }

    @Override
    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, new OperationResult("getUserByOid")).asObjectable();
    }

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    @Override
    public Collection<String> getOrgUnits(UserType user) {
        Set<String> retval = new HashSet<String>();
        for (ObjectReferenceType orgRef : user.getParentOrgRef()) {
            retval.add(orgRef.getOid());
        }
        return retval;
    }

    @Override
    public OrgType getOrgByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(OrgType.class, oid, new OperationResult("getOrgByOid")).asObjectable();
    }

    @Override
    public OrgType getOrgByName(String name) throws ObjectNotFoundException, SchemaException {
        PolyString polyName = new PolyString(name);
        polyName.recompute(prismContext.getDefaultPolyStringNormalizer());
        ObjectQuery q = QueryUtil.createNameQuery(polyName, prismContext);
        List<PrismObject<OrgType>> result = repositoryService.searchObjects(OrgType.class, q, new OperationResult("getOrgByName"));
        if (result.isEmpty()) {
            throw new ObjectNotFoundException("No organizational unit with the name '" + name + "'", name);
        }
        if (result.size() > 1) {
            throw new IllegalStateException("More than one organizational unit with the name '" + name + "' (there are " + result.size() + " of them)");
        }
        return result.get(0).asObjectable();
    }

    @Override
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

    @Override
    public boolean isManagerOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid()) && SchemaConstants.ORG_MANAGER.equals(objectReferenceType.getRelation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMemberOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPlaintextUserPassword(UserType user) throws EncryptionException {
        if (user == null || user.getCredentials() == null || user.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = user.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPassword(ShadowType account) throws EncryptionException {
        if (account == null || account.getCredentials() == null || account.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = account.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException {

        if (delta.isAdd()) {
            ShadowType newShadow = delta.getObjectToAdd().asObjectable();
            return getPlaintextAccountPassword(newShadow);
        }
        if (!delta.isModify()) {
            return null;
        }

        List<ProtectedStringType> passwords = new ArrayList<ProtectedStringType>();
        for (ItemDelta itemDelta : delta.getModifications()) {
            takePasswordsFromItemDelta(passwords, itemDelta);
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size()-1));
        } else {
            return null;
        }
    }

    private void takePasswordsFromItemDelta(List<ProtectedStringType> passwords, ItemDelta itemDelta) {
        if (itemDelta.isDelete()) {
            return;
        }

        if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE))) {
            LOGGER.trace("Found password value add/modify delta");
            Collection<PrismPropertyValue<ProtectedStringType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismPropertyValue<ProtectedStringType> value : values) {
                passwords.add(value.getValue());
            }
        } else if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD))) {
            LOGGER.trace("Found password add/modify delta");
            Collection<PrismContainerValue<PasswordType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismContainerValue<PasswordType> value : values) {
                if (value.asContainerable().getValue() != null) {
                    passwords.add(value.asContainerable().getValue());
                }
            }
        } else if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_CREDENTIALS))) {
            LOGGER.trace("Found credentials add/modify delta");
            Collection<PrismContainerValue<CredentialsType>> values = itemDelta.isAdd() ? itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
            for (PrismContainerValue<CredentialsType> value : values) {
                if (value.asContainerable().getPassword() != null && value.asContainerable().getPassword().getValue() != null) {
                    passwords.add(value.asContainerable().getPassword().getValue());
                }
            }
        }
    }

    @Override
    public String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<UserType>> objectDeltas) throws EncryptionException {

        List<ProtectedStringType> passwords = new ArrayList<ProtectedStringType>();

        for (ObjectDelta<UserType> delta : objectDeltas) {

            if (delta.isAdd()) {
                UserType newUser = delta.getObjectToAdd().asObjectable();
                return getPlaintextUserPassword(newUser);       // for simplicity we do not look for other values
            }

            if (!delta.isModify()) {
                continue;
            }

            for (ItemDelta itemDelta : delta.getModifications()) {
                takePasswordsFromItemDelta(passwords, itemDelta);
            }
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size()-1));
        } else {
            return null;
        }
    }

}
