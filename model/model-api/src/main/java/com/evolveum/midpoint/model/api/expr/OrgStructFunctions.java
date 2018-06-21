/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface OrgStructFunctions {

    Collection<String> getManagersOids(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<String> getManagersOidsExceptUser(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList, boolean preAuthorized)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException;

    Collection<UserType> getManagers(UserType user, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<UserType> getManagersByOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf, boolean preAuthorized) throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    // todo here we could select "functional" org.units in order to filter out e.g. project managers from the list of managers
    // however, the syntax of orgType attribute is not standardized
    Collection<String> getOrgUnits(UserType user, boolean preAuthorized);

    Collection<String> getOrgUnits(UserType user, QName relation, boolean preAuthorized);

    OrgType getOrgByOid(String oid, boolean preAuthorized) throws SchemaException;

    OrgType getOrgByName(String name, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    OrgType getParentOrgByOrgType(ObjectType object, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<OrgType> getParentOrgs(ObjectType object, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    Collection<UserType> getManagersOfOrg(String orgOid, boolean preAuthorized) throws SchemaException, SecurityViolationException;

    boolean isManagerOf(UserType user, String orgOid, boolean preAuthorized);

    boolean isManager(UserType user);

    boolean isManagerOfOrgType(UserType user, String orgType, boolean preAuthorized) throws SchemaException;
}
