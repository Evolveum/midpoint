/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.impl;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.SearchResultList;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipalManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@Component
public class MidPointPrincipalManagerMock implements MidPointPrincipalManager, UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointPrincipalManagerMock.class);

    @Autowired
    private transient RepositoryService repositoryService;

    @Autowired
    private ActivationComputer activationComputer;

    @Autowired
    private PrismContext prismContext;

    @Override
    public MidPointPrincipal getPrincipal(String username, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        PrismObject<? extends FocusType> focus;
        try {
            focus = findByUsername(username, clazz, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.",
                    new Object[]{username, ex.getMessage(), ex});
            throw new SystemException(ex.getMessage(), ex);
        }

        return getPrincipal(focus, null, result);
    }

    @Override
    public MidPointPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        FocusType focus = getUserByOid(oid, clazz, result);
        return getPrincipal(focus.asPrismObject());
    }

    @Override
    public MidPointPrincipal getPrincipal(PrismObject<? extends FocusType> focus) throws SchemaException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        return getPrincipal(focus, null, result);
    }



    @Override
    public MidPointPrincipal getPrincipal(PrismObject<? extends FocusType> focus,
            AuthorizationTransformer authorizationLimiter, OperationResult result) throws SchemaException {
        if (focus == null) {
            return null;
        }

        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);

        MidPointPrincipal principal = new MidPointPrincipal(focus.asObjectable());
        initializePrincipalFromAssignments(principal, systemConfiguration);
        return principal;
    }

    private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) {
        PrismObject<SystemConfigurationType> systemConfiguration = null;
        try {
            // TODO: use SystemObjectCache instead?
            systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                    null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.warn("No system configuration: {}", e.getMessage(), e);
        }
        return systemConfiguration;
    }

    @Override
    public void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        OperationResult result = new OperationResult(OPERATION_UPDATE_USER);
        try {
            save(principal, result);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{principal.getUsername(), principal.getOid(), ex.getMessage()});
        }
    }

    private PrismObject<? extends FocusType> findByUsername(String username, Class<? extends FocusType> focusType,  OperationResult result) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        ObjectQuery query = ObjectQueryUtil.createNormNameQuery(usernamePoly, prismContext);
        LOGGER.trace("Looking for user, query:\n" + query.debugDump());

        List<? extends PrismObject<? extends FocusType>> list = repositoryService.searchObjects(focusType, query, null,
                result);
        LOGGER.trace("Users found: {}.", (list != null ? list.size() : 0));
        if (list == null || list.size() != 1) {
            return null;
        }

        return list.get(0);
    }

    private void initializePrincipalFromAssignments(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration) {

        OperationResult result = new OperationResult(MidPointPrincipalManagerMock.class.getName() + ".addAuthorizations");

        principal.setApplicableSecurityPolicy(locateSecurityPolicy(principal, systemConfiguration, result));

//        if (systemConfiguration != null) {
//            principal.setAdminGuiConfiguration(systemConfiguration.asObjectable().getAdminGuiConfiguration());
//        }

        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.getAction().add("FAKE");
        principal.getAuthorities().add(new Authorization(authorizationType));

        ActivationType activation = principal.getFocus().getActivation();
        if (activation != null) {
            activationComputer.computeEffective(principal.getFocus().getLifecycleState(), activation, null);
        }
    }

    private SecurityPolicyType locateSecurityPolicy(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, OperationResult result) {
        if (systemConfiguration == null) {
            return null;
        }
        ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.asObjectable().getGlobalSecurityPolicyRef();
        if (globalSecurityPolicyRef == null) {
            return null;
        }
        try {
            PrismObject<SecurityPolicyType> policy = repositoryService.getObject(SecurityPolicyType.class, globalSecurityPolicyRef.getOid(), null, result);
            return policy.asObjectable();
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private MidPointPrincipal save(MidPointPrincipal person, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        PrismObject<? extends FocusType> newUser = person.getFocus().asPrismObject();

        FocusType oldUserType = getUserByOid(person.getOid(), newUser.asObjectable().getClass(), result);
        PrismObject<? extends FocusType> oldUser = oldUserType.asPrismObject();



        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>)oldUser).diff((PrismObject<FocusType>)newUser);
        repositoryService.modifyObject(newUser.asObjectable().getClass(), delta.getOid(), delta.getModifications(),
                new OperationResult(OPERATION_UPDATE_USER));

        return person;
    }

    private FocusType getUserByOid(String oid, Class<? extends FocusType> clazz, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(clazz, oid,
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
            owner = repositoryService.searchShadowOwner(object.getOid(), null, new OperationResult(MidPointPrincipalManagerMock.class+".resolveOwner"));
        }
        if (owner == null) {
            return null;
        }
        return owner;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//         TODO Auto-generated method stub
        try {
            return getPrincipal(username, FocusType.class);
        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        } catch (SchemaException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }


}
