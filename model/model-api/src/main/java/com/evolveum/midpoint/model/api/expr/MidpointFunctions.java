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
