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

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.UserComputer;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AdminGuiConfigTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 * @author semancik
 */
@Service(value = "userDetailsService")
public class UserProfileServiceImpl implements UserProfileService, UserDetailsService, UserDetailsContextMapper {

    private static final Trace LOGGER = TraceManager.getTrace(UserProfileServiceImpl.class);
    
    @Autowired(required = true)
    private transient RepositoryService repositoryService;
    
    @Autowired(required = true)
    private ObjectResolver objectResolver;
    
    @Autowired(required = true)
	private SystemObjectCache systemObjectCache;
    
    @Autowired(required = true)
    private MappingFactory mappingFactory;

    @Autowired(required = true)
    private MappingEvaluator mappingEvaluator;
    
    @Autowired(required = true)
    private SecurityHelper securityHelper;

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
    public MidPointPrincipal getPrincipal(String username) throws ObjectNotFoundException, SchemaException {
    	OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
    	PrismObject<UserType> user = null;
        try {
            user = findByUsername(username, result);
        } catch (ObjectNotFoundException ex) {
        	LOGGER.trace("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
        	throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw new SystemException(ex.getMessage(), ex);
        }

        return createPrincipal(user, result);
    }

    @Override
    public MidPointPrincipal getPrincipal(PrismObject<UserType> user) throws SchemaException {
    	OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
    	return createPrincipal(user, result);
    }
    
    private MidPointPrincipal createPrincipal(PrismObject<UserType> user, OperationResult result) throws SchemaException {
        if (user == null) {
            return null;
        }
        
        PrismObject<SystemConfigurationType> systemConfiguration = null;
        try {
        	systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), 
					null, result);
		} catch (ObjectNotFoundException | SchemaException e) {
			LOGGER.warn("No system configuration: {}", e.getMessage(), e);
		}

    	userComputer.recompute(user);
        MidPointPrincipal principal = new MidPointPrincipal(user.asObjectable());
        initializePrincipalFromAssignments(principal, systemConfiguration);
        return principal;
    }

    @Override
    public void updateUser(MidPointPrincipal principal) {
    	OperationResult result = new OperationResult(OPERATION_UPDATE_USER);
        try {
            save(principal, result);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{principal.getFullName(), principal.getOid(), ex.getMessage(), ex});
        }
    }

    private PrismObject<UserType> findByUsername(String username, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        ObjectQuery query = ObjectQueryUtil.createNormNameQuery(usernamePoly, prismContext);
        LOGGER.trace("Looking for user, query:\n" + query.debugDump());

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query, null, 
                result);
        LOGGER.trace("Users found: {}.", (list != null ? list.size() : 0));
        if (list == null || list.size() != 1) {
            return null;
        }
        
        return list.get(0);
    }
        
	private void initializePrincipalFromAssignments(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration) throws SchemaException {
		UserType userType = principal.getUser();

		Collection<Authorization> authorizations = principal.getAuthorities();
		List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();

		Task task = taskManager.createTaskInstance(UserProfileServiceImpl.class.getName() + ".addAuthorizations");
        OperationResult result = task.getResult();

        principal.setApplicableSecurityPolicy(securityHelper.locateSecurityPolicy(userType.asPrismObject(), systemConfiguration, task, result));

		if (!userType.getAssignment().isEmpty()) {
			LensContext<UserType> lensContext = new LensContextPlaceholder<>(userType.asPrismObject(), prismContext);
			AssignmentEvaluator.Builder<UserType> builder =
					new AssignmentEvaluator.Builder<UserType>()
							.repository(repositoryService)
							.focusOdo(new ObjectDeltaObject<>(userType.asPrismObject(), null, userType.asPrismObject()))
							.channel(null)
							.objectResolver(objectResolver)
							.systemObjectCache(systemObjectCache)
							.prismContext(prismContext)
							.mappingFactory(mappingFactory)
							.mappingEvaluator(mappingEvaluator)
							.activationComputer(activationComputer)
							.now(clock.currentTimeXMLGregorianCalendar())
							// We do need only authorizations + gui config. Therefore we not need to evaluate
							// constructions and the like, so switching it off makes the evaluation run faster.
							// It also avoids nasty problems with resources being down,
							// resource schema not available, etc.
							.loginMode(true)
							// We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
							// will need something to push on the stack. So give them context placeholder.
							.lensContext(lensContext);

			AssignmentEvaluator<UserType> assignmentEvaluator = builder.build();

			for (AssignmentType assignmentType: userType.getAssignment()) {
				try {
					ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
					assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
					assignmentIdi.recompute();
					EvaluatedAssignment<UserType> assignment = assignmentEvaluator.evaluate(assignmentIdi, false, userType, userType.toString(), task, result);
					if (assignment.isValid()) {
						authorizations.addAll(assignment.getAuthorizations());
						adminGuiConfigurations.addAll(assignment.getAdminGuiConfigurations());
					}
					for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) {
						if (target.getTarget() != null && target.getTarget().asObjectable() instanceof UserType
							&& DeputyUtils.isDelegationPath(target.getAssignmentPath())) {
							List<OtherPrivilegesLimitationType> limitations = DeputyUtils.extractLimitations(target.getAssignmentPath());
							principal.addDelegatorWithOtherPrivilegesLimitations(new DelegatorWithOtherPrivilegesLimitations(
									(UserType) target.getTarget().asObjectable(), limitations));
						}
					}
				} catch (SchemaException e) {
					LOGGER.error("Schema violation while processing assignment of {}: {}; assignment: {}",
							userType, e.getMessage(), assignmentType, e);
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Object not found while processing assignment of {}: {}; assignment: {}",
							userType, e.getMessage(), assignmentType, e);
				} catch (ExpressionEvaluationException e) {
					LOGGER.error("Evaluation error while processing assignment of {}: {}; assignment: {}",
							userType, e.getMessage(), assignmentType, e);
				} catch (PolicyViolationException e) {
					LOGGER.error("Policy violation while processing assignment of {}: {}; assignment: {}",
							userType, e.getMessage(), assignmentType, e);
				}
			}
		}
		if (userType.getAdminGuiConfiguration() != null) {
			// config from the user object should go last (to be applied as the last one)
			adminGuiConfigurations.add(userType.getAdminGuiConfiguration());
		}
        principal.setAdminGuiConfiguration(AdminGuiConfigTypeUtil.compileAdminGuiConfiguration(adminGuiConfigurations, systemConfiguration));
	}

	private MidPointPrincipal save(MidPointPrincipal person, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        UserType oldUserType = getUserByOid(person.getOid(), result);
        PrismObject<UserType> oldUser = oldUserType.asPrismObject();

        PrismObject<UserType> newUser = person.getUser().asPrismObject();

        ObjectDelta<UserType> delta = oldUser.diff(newUser);
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Updating user {} with delta:\n{}", newUser, delta.debugDump());
        }
        repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(),
                new OperationResult(OPERATION_UPDATE_USER));

        return person;
    }

    private UserType getUserByOid(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid,
        		null, result).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }

	@Override
	public <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object) {
		if (object == null || object.getOid() == null) {
			return null;
		}
		PrismObject<F> owner = null;
		if (object.canRepresent(ShadowType.class)) {
			owner = repositoryService.searchShadowOwner(object.getOid(), null, new OperationResult(UserProfileServiceImpl.class+".resolveOwner"));
		} else if (object.canRepresent(AbstractRoleType.class)) {
			ObjectReferenceType ownerRef = ((AbstractRoleType)(object.asObjectable())).getOwnerRef();
			if (ownerRef != null && ownerRef.getOid() != null && ownerRef.getType() != null) {
				OperationResult result = new OperationResult(UserProfileService.class.getName() + ".resolveOwner");
				try {
					owner = (PrismObject<F>) repositoryService.getObject(ObjectTypes.getObjectTypeFromTypeQName(ownerRef.getType()).getClassDefinition(),
							ownerRef.getOid(), null, result);
				} catch (ObjectNotFoundException | SchemaException e) {
					LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
				}
			}
		}
		if (owner == null) {
			return null;
		}
		if (owner.canRepresent(UserType.class)) {
			userComputer.recompute((PrismObject<UserType>)owner);
		}
		return owner;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			return getPrincipal(username);
		} catch (ObjectNotFoundException e) {
			throw new UsernameNotFoundException(e.getMessage(), e);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
			Collection<? extends GrantedAuthority> authorities) {
		try {
			return getPrincipal(username);
		} catch (ObjectNotFoundException e) {
			throw new UsernameNotFoundException(e.getMessage(), e);
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		// TODO Auto-generated method stub
		
	}
	
	
}
