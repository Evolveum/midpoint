package com.evolveum.midpoint.model.api.expr;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface MidpointFunctions {

    List<String> toList(String... s);

    Collection<String> getManagersOids(UserType user) throws SchemaException, ObjectNotFoundException;

    Collection<String> getManagersOidsExceptUser(UserType user) throws SchemaException, ObjectNotFoundException;

    Collection<UserType> getManagers(UserType user) throws SchemaException, ObjectNotFoundException;

    UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException;

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    Collection<String> getOrgUnits(UserType user);

    OrgType getOrgByOid(String oid) throws ObjectNotFoundException, SchemaException;

    OrgType getOrgByName(String name) throws ObjectNotFoundException, SchemaException;

    Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException;

    boolean isManagerOf(UserType user, String orgOid);

    boolean isMemberOf(UserType user, String orgOid);

    String getPlaintextUserPassword(UserType user) throws EncryptionException;

    String getPlaintextAccountPassword(ShadowType account) throws EncryptionException;

    String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException;

    String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<UserType>> deltas) throws EncryptionException;
}
