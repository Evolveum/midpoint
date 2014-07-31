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

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.UserComputer;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 * @author semancik
 */
@Service(value = "userDetailsService")
public class UserProfileServiceImpl implements UserProfileService {

    private static final Trace LOGGER = TraceManager.getTrace(UserProfileServiceImpl.class);
    
    @Autowired(required = true)
    private transient RepositoryService repositoryService;
    
    @Autowired(required = true)
    private ObjectResolver objectResolver;
    
    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private UserComputer userComputer;
    
    @Autowired(required = true)
	private ActivationComputer activationComputer;
    
    @Autowired(required = true)
    private Clock clock;
    
    @Autowired(required = true)
    private PrismContext prismContext;
    
    @Autowired(required = true)
    private TaskManager taskManager;

    @Override
    public MidPointPrincipal getPrincipal(String username) throws ObjectNotFoundException {
    	PrismObject<UserType> user = null;
        try {
            user = findByUsername(username);
        } catch (ObjectNotFoundException ex) {
        	LOGGER.trace("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
        	throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw new SystemException(ex.getMessage(), ex);
        }

        if (user == null) {
            return null;
        }

        return getPrincipal(user);
    }

    @Override
    public MidPointPrincipal getPrincipal(PrismObject<UserType> user) {
        if (user == null) {
            return null;
        }

    	userComputer.recompute(user);
        MidPointPrincipal principal = new MidPointPrincipal(user.asObjectable());
        addAuthorizations(principal);
        return principal;
    }

    @Override
    public void updateUser(MidPointPrincipal principal) {
        try {
            save(principal);
        } catch (RepositoryException ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{principal.getFullName(), principal.getOid(), ex.getMessage()});
        }
    }

    private PrismObject<UserType> findByUsername(String username) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        ObjectQuery query = ObjectQueryUtil.createNameQuery(usernamePoly, prismContext);
//        ObjectQuery query = ObjectQuery.createObjectQuery(
//                EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, usernamePoly));
        LOGGER.trace("Looking for user, query:\n" + query.debugDump());

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
        return user;
    }
        
	private void addAuthorizations(MidPointPrincipal principal) {
		UserType userType = principal.getUser();

		Collection<Authorization> authorizations = principal.getAuthorities();
        CredentialsType credentials = userType.getCredentials();

        if (userType.getAssignment().isEmpty()) {
            return;
        }
		
		AssignmentEvaluator<UserType> assignmentEvaluator = new AssignmentEvaluator<>();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setFocusOdo(new ObjectDeltaObject<UserType>(userType.asPrismObject(), null, userType.asPrismObject()));
        assignmentEvaluator.setChannel(null);
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setMappingFactory(valueConstructionFactory);
        assignmentEvaluator.setActivationComputer(activationComputer);
        assignmentEvaluator.setNow(clock.currentTimeXMLGregorianCalendar());
        
        // We do need only authorizations. Therefore we not need to evaluate constructions,
        // so switching it off is faster. It also avoids nasty problems with resources being down,
        // resource schema not available, etc.
        assignmentEvaluator.setEvaluateConstructions(false);
        
        // We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
        // will need something to push on the stack. So give them context placeholder.
        LensContext<UserType> lensContext = new LensContextPlaceholder<>(prismContext);
		assignmentEvaluator.setLensContext(lensContext);
		
		Task task = taskManager.createTaskInstance(UserProfileServiceImpl.class.getName() + ".addAuthorizations");
        OperationResult result = task.getResult();
        for(AssignmentType assignmentType: userType.getAssignment()) {
        	try {
        		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        		assignmentIdi.recompute();
				EvaluatedAssignment<UserType> assignment = assignmentEvaluator.evaluate(assignmentIdi, false, userType, userType.toString(), task, result);
				if (assignment.isValid()) {
					authorizations.addAll(assignment.getAuthorizations());
				}
			} catch (SchemaException e) {
				LOGGER.error("Schema violation while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Object not found while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (ExpressionEvaluationException e) {
				LOGGER.error("Evaluation error while processing assignment of {}: {}; assignment: {}", 
						new Object[]{userType, e.getMessage(), assignmentType, e});
			} catch (PolicyViolationException e) {
				LOGGER.error("Policy violation while processing assignment of {}: {}; assignment: {}", 
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

	@Override
	public <F extends FocusType> PrismObject<F> resolveOwner(PrismObject<ShadowType> shadow) {
		if (shadow == null || shadow.getOid() == null) {
			return null;
		}
		PrismObject<F> owner;
		try {
			owner = repositoryService.searchShadowOwner(shadow.getOid(), null, new OperationResult(UserProfileServiceImpl.class+".resolveOwner"));
		} catch (ObjectNotFoundException e) {
			throw new SystemException(e.getMessage(), e);
		}
		if (owner == null) {
			return null;
		}
		if (owner.canRepresent(UserType.class)) {
			userComputer.recompute((PrismObject<UserType>)owner);
		}
		return owner;
	}
}
