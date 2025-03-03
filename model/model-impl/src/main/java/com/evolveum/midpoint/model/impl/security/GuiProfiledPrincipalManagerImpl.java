/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.authentication.api.MidpointSessionRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;

import jakarta.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.FocusComputer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheInvalidationEventSpecification;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.google.common.collect.ImmutableSet;

/**
 * @author lazyman
 * @author semancik
 */
@Service(value = "guiProfiledPrincipalManager")
public class GuiProfiledPrincipalManagerImpl
        implements CacheListener, GuiProfiledPrincipalManager, UserDetailsService, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(GuiProfiledPrincipalManagerImpl.class);

    // TODO consider also roleMembershipRef
    private static final Set<ItemPath> ASSIGNMENTS_AND_ADMIN_GUI_PATHS = ImmutableSet.of(FocusType.F_ASSIGNMENT, RoleType.F_ADMIN_GUI_CONFIGURATION, FocusType.F_ACTIVATION);
    private static final Set<ChangeType> MODIFY_DELETE_CHANGES = CacheInvalidationEventSpecification.MODIFY_DELETE;

    private static final Collection<CacheInvalidationEventSpecification> CACHE_EVENT_SPECIFICATION =
            ImmutableSet.<CacheInvalidationEventSpecification>builder()
            .add(CacheInvalidationEventSpecification.of(UserType.class,ASSIGNMENTS_AND_ADMIN_GUI_PATHS,MODIFY_DELETE_CHANGES))
            .add(CacheInvalidationEventSpecification.of(ArchetypeType.class, ImmutableSet.of(ArchetypeType.F_ARCHETYPE_POLICY, FocusType.F_ASSIGNMENT, RoleType.F_ADMIN_GUI_CONFIGURATION, FocusType.F_ACTIVATION) ,MODIFY_DELETE_CHANGES))
            .add(CacheInvalidationEventSpecification.of(AbstractRoleType.class,ASSIGNMENTS_AND_ADMIN_GUI_PATHS,MODIFY_DELETE_CHANGES))
            .add(CacheInvalidationEventSpecification.of(SystemConfigurationType.class, ImmutableSet.of(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION), MODIFY_DELETE_CHANGES))
            .build();


    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private GuiProfileCompiler guiProfileCompiler;
    @Autowired
    private FocusComputer focusComputer;
    @Autowired
    private PrismContext prismContext;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private CacheDispatcher cacheDispatcher;

    // registry is not available e.g. during tests
    @Autowired(required = false)
    private MidpointSessionRegistry sessionRegistry;

    @Override
    public void setMessageSource(@NotNull MessageSource messageSource) {
    }

    @PostConstruct
    public void initialize() {
        LOGGER.info("Registering as cache listener");
        cacheDispatcher.registerCacheListener(this);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(String username, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        PrismObject<? extends FocusType> focus;
        try {
            focus = findByUsername(username, clazz, result);
            if (focus == null) {
                throw new ObjectNotFoundException("Couldn't find focus with name '" + username + "'", clazz, null);
            }
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Couldn't find user with name '{}', reason: {}.", username, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.", username, ex.getMessage(), ex);
            throw new SystemException(ex.getMessage(), ex);
        }

        return getPrincipal(focus, null, options, result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(ObjectQuery query, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        PrismObject<? extends FocusType> focus;
        try {
            focus = searchFocus(clazz, query, result);
            if (focus == null) {
                throw new ObjectNotFoundException("Couldn't find focus by query '" + query + "'", clazz, null);
            }
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Couldn't find user by defined query '{}', reason: {}.", query, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user by defined query '{}', reason: {}.", query, ex.getMessage(), ex);
            throw new SystemException(ex.getMessage(), ex);
        }

        return getPrincipal(focus, null, options, result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        return getPrincipal(
                getUserByOid(oid, clazz, result).asPrismObject(),
                options,
                result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(
            PrismObject<? extends FocusType> focus, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        return getPrincipal(focus, null, options, result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(
            PrismObject<? extends FocusType> focus,
            AuthorizationTransformer authorizationTransformer,
            ProfileCompilerOptions options,
            OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        if (focus == null) {
            return null;
        }
        securityContextManager.setTemporaryPrincipalOid(focus.getOid());
        try {
            PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
            LifecycleStateModelType lifecycleModel = getLifecycleModel(focus, systemConfiguration);

            focusComputer.recompute(focus, lifecycleModel);
            GuiProfiledPrincipal principal = new GuiProfiledPrincipal(focus.asObjectable());
            initializePrincipalFromAssignments(principal, systemConfiguration, authorizationTransformer, options);
            return principal;
        } finally {
            securityContextManager.clearTemporaryPrincipalOid();
        }
    }

    @Override
    public List<UserSessionManagementType> getLocalLoggedInPrincipals() {

        String currentNodeId = taskManager.getNodeId();

        if (sessionRegistry != null) {
            List<Object> loggedInUsers = sessionRegistry.getAllPrincipals();
            List<UserSessionManagementType> loggedPrincipals = new ArrayList<>();
            for (Object principal : loggedInUsers) {

                if (!(principal instanceof GuiProfiledPrincipal midPointPrincipal)) {
                    continue;
                }

                List<SessionInformation> sessionInfos = sessionRegistry.getLoggedInUsersSession(principal);
                if (sessionInfos == null || sessionInfos.isEmpty()) {
                    continue;
                }

                UserSessionManagementType userSessionManagementType = new UserSessionManagementType();
                userSessionManagementType.setFocus(midPointPrincipal.getFocus());
                userSessionManagementType.setActiveSessions(sessionInfos.size());
                userSessionManagementType.getNode().add(currentNodeId);
                loggedPrincipals.add(userSessionManagementType);

            }

            return loggedPrincipals;

        } else {
            return emptyList();
        }
    }

    @Override
    public void terminateLocalSessions(TerminateSessionEvent terminateSessionEvent) {
        List<String> principalOids = terminateSessionEvent.getPrincipalOids();
        if (sessionRegistry != null && CollectionUtils.isNotEmpty(principalOids)) {
            List<Object> loggedInUsers = sessionRegistry.getAllPrincipals();
            for (Object principal : loggedInUsers) {

                if (!(principal instanceof GuiProfiledPrincipal midPointPrincipal)) {
                    continue;
                }

                if (!principalOids.contains(midPointPrincipal.getOid())) {
                    continue;
                }

                List<SessionInformation> sessionInfos = sessionRegistry.getLoggedInUsersSession(principal);
                if (sessionInfos == null || sessionInfos.isEmpty()) {
                    continue;
                }

                for (SessionInformation sessionInfo : sessionInfos) {
                    sessionInfo.expireNow();
                }
            }
        }
    }

    private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) {
        PrismObject<SystemConfigurationType> systemConfiguration = null;
        try {
            // TODO: use SystemObjectCache instead?
            systemConfiguration = repositoryService.getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), createReadOnlyCollection(), result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.warn("No system configuration: {}", e.getMessage(), e);
        }
        return systemConfiguration;
    }

    private LifecycleStateModelType getLifecycleModel(PrismObject<? extends FocusType> focus, PrismObject<SystemConfigurationType> systemConfiguration) {
        if (systemConfiguration == null) {
            return null;
        }
        try {
            return ArchetypeManager.determineLifecycleModel(focus, systemConfiguration.asObjectable());
        } catch (ConfigurationException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    @Override
    public void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        OperationResult result = new OperationResult(OPERATION_UPDATE_USER);
        try {
            save(principal, itemDeltas, result);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.", principal.getUsername(), principal.getOid(), ex.getMessage(), ex);
        }
    }

    private PrismObject<? extends FocusType> findByUsername(
            String username, Class<? extends FocusType> clazz, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return searchFocus(
                clazz,
                ObjectQueryUtil.createNormNameQuery(new PolyString(username)),
                result);
    }

    private PrismObject<? extends FocusType> searchFocus(
            Class<? extends FocusType> clazz, ObjectQuery query, OperationResult result) throws SchemaException {
        LOGGER.trace("Looking for user, query:\n{}", query.debugDumpLazily(1));
        var list = repositoryService.searchObjects(clazz, query, null, result);
        LOGGER.trace("Users found: {}.", list.size());
        if (list.size() != 1) {
            return null;
        }
        return list.get(0);
    }

    private void initializePrincipalFromAssignments(
            GuiProfiledPrincipal principal,
            PrismObject<SystemConfigurationType> systemConfiguration,
            AuthorizationTransformer authorizationTransformer,
            ProfileCompilerOptions options) {
        Task task = taskManager.createTaskInstance(GuiProfiledPrincipalManagerImpl.class.getName() + ".initializePrincipalFromAssignments");
        OperationResult result = task.getResult();
        try {
            guiProfileCompiler.compileFocusProfile(principal, systemConfiguration, authorizationTransformer, options, task, result);
        } catch (Throwable e) {
            // Do not let any error stop processing here. This code is used during user login. An error here can stop login procedure. We do not
            // want that. E.g. wrong adminGuiConfig may prohibit login on administrator, therefore ruining any chance of fixing the situation.
            LOGGER.error("Error compiling user profile for {}: {}", principal, e.getMessage(), e);
            // Do NOT re-throw the exception here. Just go on.
        }
    }

    private void save(MidPointPrincipal person, Collection<? extends ItemDelta<?, ?>> itemDeltas,
            OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        LOGGER.trace("Updating user {} with deltas:\n{}", person.getFocus(), DebugUtil.debugDumpLazily(itemDeltas));
        repositoryService.modifyObject(FocusType.class, person.getFocus().getOid(), itemDeltas, result);
    }

    private FocusType getUserByOid(String oid, Class<? extends FocusType> clazz, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(clazz, oid, null, result).asObjectable();
    }

    @Override
    public <F extends FocusType, O extends ObjectType> Collection<PrismObject<F>> resolveOwner(PrismObject<O> object) {
        if (object == null || object.getOid() == null) {
            return null;
        }
        PrismObject<F> owner = null;
        OperationResult result = new OperationResult(GuiProfiledPrincipalManagerImpl.class + ".resolveOwner");

        // TODO: what about using LensOwnerResolver here?

        if (object.canRepresent(ShadowType.class)) {
            owner = repositoryService.searchShadowOwner(object.getOid(), null, result);

        } else if (object.canRepresent(UserType.class)) {
            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(FocusType.F_PERSONA_REF).ref(object.getOid()).build();
            SearchResultList<PrismObject<UserType>> owners;
            try {
                owners = repositoryService.searchObjects(UserType.class, query, null, result);
                if (owners.isEmpty()) {
                    return null;
                }
                if (owners.size() > 1) {
                    LOGGER.warn("More than one owner of {}: {}", object, owners);
                }
                owner = (PrismObject<F>) owners.get(0);
            } catch (SchemaException e) {
                LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
            }

        } else if (object.canRepresent(AbstractRoleType.class)) {
            var prismRefs = SchemaService.get().relationRegistry().getAllRelationsFor(RelationKindType.OWNER)
                    .stream().map(r -> {
                        var ret = PrismContext.get().itemFactory().createReferenceValue(object.getOid(), object.getAnyValue().getTypeName());
                        ret.setRelation(r);
                        return ret;
                    })
                    .toList();
            ObjectQuery query = PrismContext.get().queryFor(FocusType.class)
                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(prismRefs).build();
            try {
                return repositoryService.searchObjects((Class<F>)(Class) FocusType.class, query, createReadOnlyCollection(), result);
            } catch (SchemaException e) {
                LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
                return null;
            }
        } else if (object.canRepresent(TaskType.class)) {
            ObjectReferenceType ownerRef = ((TaskType) (object.asObjectable())).getOwnerRef();
            if (ownerRef != null && ownerRef.getOid() != null && ownerRef.getType() != null) {
                try {
                    ObjectTypes type = ObjectTypes.getObjectTypeFromTypeQName(ownerRef.getType());
                    owner = repositoryService.getObject(
                            type.getClassDefinition(), ownerRef.getOid(), null, result);
                } catch (ObjectNotFoundException | SchemaException e) {
                    LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
                }
            }
        }

        if (owner == null) {
            return null;
        }
        if (owner.canRepresent(UserType.class)) {
            PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
            LifecycleStateModelType lifecycleModel = getLifecycleModel(owner, systemConfiguration);
            focusComputer.recompute(owner, lifecycleModel);
        }
        return Collections.singletonList(owner);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return getPrincipal(username, FocusType.class, ProfileCompilerOptions.create());
        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    @Override
    public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return CACHE_EVENT_SPECIFICATION;
    }

    @Override
    public <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide,
            CacheInvalidationContext context) {
        if (sessionRegistry == null) {
            // In tests sessionRegistry is null.
            return;
        }

        List<Object> loggedInUsers = sessionRegistry.getAllPrincipals();
        for (Object principal : loggedInUsers) {

            if (!(principal instanceof GuiProfiledPrincipal midPointPrincipal)) {
                continue;
            }

            List<SessionInformation> sessionInfos = sessionRegistry.getLoggedInUsersSession(principal);
            if (sessionInfos == null || sessionInfos.isEmpty()) {
                continue;
            }
            CompiledGuiProfile compiledProfile = midPointPrincipal.getCompiledGuiProfile();
            LOGGER.debug("Checking {} if it is derived from {}", midPointPrincipal, oid);
            LOGGER.trace("      is actually derived from {}", compiledProfile.getDependencies());

            if (oid == null || compiledProfile.derivedFrom(oid)) {
                LOGGER.debug("Markin profile invalid for {} because of change in {}:{}", midPointPrincipal, type, oid);
                compiledProfile.markInvalid();
            }

        }
    }


    @Override
    public @NotNull CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal principal) {
        return refreshCompiledProfile(
                principal,
                ProfileCompilerOptions.create()
                    .compileGuiAdminConfiguration(true)
                    .collectAuthorization(true)
                    .locateSecurityPolicy(true));
    }

    @Override
    public @NotNull CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal principal, ProfileCompilerOptions options) {
        OperationResult result = new OperationResult("refreshCompiledProfile");

        // Maybe focus was also changed, we should probably reload it
        // TODO: Should recompute / compute be synchronized on principal?

        // TODO check also if authorization list is empty (beware, this maybe should be done after principal is fully initialized)

        LOGGER.debug("Recomputing GUI profile for {}", principal);
        var focusOid = principal.getFocus().getOid();
        PrismObject<? extends FocusType> focus;
        try {
            focus = repositoryService.getObject(principal.getFocus().getClass(), focusOid, null, result);
            principal.setOrReplaceFocus(focus.asObjectable());
            if (options.terminateDisabledUserSession() && !principal.isEnabled()) {
                // User is disabled
                var terminate = new TerminateSessionEvent();
                terminate.setPrincipalOids(Collections.singletonList(principal.getOid()));
                terminateLocalSessions(terminate);
                // Do not recompute profile
                return principal.getCompiledGuiProfile();
            }

        } catch (ObjectNotFoundException e) {
            throw new SystemException("Focus was deleted");
        } catch (SchemaException e) {
            throw new SystemException("Encountered schema exception", e);
        }
        securityContextManager.setTemporaryPrincipalOid(focusOid);
        try {
            PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
            LifecycleStateModelType lifecycleModel = getLifecycleModel(focus, systemConfiguration);
            focusComputer.recompute(focus, lifecycleModel);
            principal.clearAuthorizations();
            principal.clearOtherPrivilegesLimitations();
            // For refreshing current logged-in principal, we need to support GUI config
            initializePrincipalFromAssignments(
                    principal,
                    systemConfiguration,
                    null,
                    options);
            principal.clearEffectivePrivilegesModification(); // we just recomputed them strictly from user's assignments
            return principal.getCompiledGuiProfile();
        } finally {
            securityContextManager.clearTemporaryPrincipalOid();
        }
    }
}

