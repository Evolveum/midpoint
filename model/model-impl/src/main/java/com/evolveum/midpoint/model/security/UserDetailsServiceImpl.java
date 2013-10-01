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

package com.evolveum.midpoint.model.security;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.UserComputer;
import com.evolveum.midpoint.model.lens.Assignment;
import com.evolveum.midpoint.model.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Service(value = "userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(UserDetailsServiceImpl.class);
    
    @Autowired(required = true)
    private transient RepositoryService repositoryService;
    
    @Autowired(required = true)
    private ObjectResolver objectResolver;
    
    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private UserComputer userComputer;
    
    @Autowired(required = true)
    private PrismContext prismContext;

    @Override
    public MidPointPrincipal getUser(String principal) {
    	MidPointPrincipal user = null;
        try {
            user = findByUsername(principal);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{principal, ex.getMessage()});
        }

        return user;
    }

    @Override
    public void updateUser(MidPointPrincipal user) {
        try {
            save(user);
        } catch (RepositoryException ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{user.getFullName(), user.getOid(), ex.getMessage()});
        }
    }

    private MidPointPrincipal findByUsername(String username) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        usernamePoly.recompute(prismContext.getDefaultPolyStringNormalizer());

        ObjectQuery query = ObjectQuery.createObjectQuery(
                EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, usernamePoly));
        LOGGER.trace("Looking for user, query:\n" + query.dump());

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query, null, 
                new OperationResult("Find by username"));
        if (list == null) {
            return null;
        }
        LOGGER.trace("Users found: {}.", new Object[]{list.size()});
        if (list.size() == 0 || list.size() > 1) {
            return null;
        }
        
        PrismObject<UserType> user = list.get(0);
        userComputer.recompute(user);

        MidPointPrincipal principal = new MidPointPrincipal(user.asObjectable());
        addAuthorizations(principal);
        return principal;
    }

	private void addAuthorizations(MidPointPrincipal principal) {
		UserType userType = principal.getUser();

		Collection<Authorization> authorizations = principal.getAuthorities();
        CredentialsType credentials = userType.getCredentials();
        if (credentials != null && credentials.isAllowedIdmAdminGuiAccess() != null
                && credentials.isAllowedIdmAdminGuiAccess()) {
            AuthorizationType authorization = new AuthorizationType();
            authorization.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);

            authorizations.add(new Authorization(authorization));
        }

        if (userType.getAssignment().isEmpty()) {
            return;
        }
		
		AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setUserOdo(new ObjectDeltaObject<UserType>(userType.asPrismObject(), null, userType.asPrismObject()));
        assignmentEvaluator.setChannel(null);
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);
        // We do need only authorizations. Therefore we not need to evaluate constructions,
        // so switching it off is faster. It also avoids nasty problems with resources being down,
        // resource schema not available, etc.
        assignmentEvaluator.setEvaluateConstructions(false);
		
        OperationResult result = new OperationResult(UserDetailsServiceImpl.class.getName() + ".addAuthorizations");
        for(AssignmentType assignmentType: userType.getAssignment()) {
        	try {
				Assignment assignment = assignmentEvaluator.evaluate(assignmentType, userType, userType.toString(), result);
				authorizations.addAll(assignment.getAuthorizations());
			} catch (SchemaException e) {
				LOGGER.error("Schema violation while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Object not found while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ExpressionEvaluationException e) {
				LOGGER.error("Evaluation error while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			}
        }
	}

	private MidPointPrincipal save(MidPointPrincipal person) throws RepositoryException {
        try {
            UserType oldUserType = getUserByOid(person.getOid());
            PrismObject<UserType> oldUser = oldUserType.asPrismObject();

            PrismObject<UserType> newUser = person.getUser().asPrismObject();

            ObjectDelta<UserType> delta = oldUser.diff(newUser);
            repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(),
                    new OperationResult(OPERATION_UPDATE_USER));
        } catch (Exception ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        }

        return person;
    }

    private UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid,
        		null, new OperationResult(OPERATION_GET_USER)).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }
}
