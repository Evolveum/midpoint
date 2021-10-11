/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import java.util.*;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.model.api.authentication.MidpointDirContextAdapter;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.impl.FocusComputer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static java.util.Collections.emptyList;

/**
 * @author lazyman
 * @author semancik
 */
@Service(value = "guiProfiledPrincipalManager")
public class GuiProfiledPrincipalManagerImpl implements GuiProfiledPrincipalManager, UserDetailsService, UserDetailsContextMapper, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(GuiProfiledPrincipalManagerImpl.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    @Qualifier("modelObjectResolver")
    private ObjectResolver objectResolver;
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

    // registry is not available e.g. during tests
    @Autowired(required = false)
    private SessionRegistry sessionRegistry;

    //optional application.yml property for LDAP authentication, marks LDAP attribute name that correlates with midPoint UserType name
//    @Value("${auth.ldap.search.naming-attr:#{null}}")
//    private String ldapNamingAttr;

    private MessageSourceAccessor messages;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(String username, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        PrismObject<FocusType> focus;
        try {
            focus = findByUsername(username, clazz, result);

            if (focus == null) {
                throw new ObjectNotFoundException("Couldn't find focus with name '" + username + "'");
            }
        } catch (ObjectNotFoundException ex) {
            LOGGER.trace("Couldn't find user with name '{}', reason: {}.", username, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Error getting user with name '{}', reason: {}.", username, ex.getMessage(), ex);
            throw new SystemException(ex.getMessage(), ex);
        }

        return getPrincipal(focus, null, result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        return getPrincipal(getUserByOid(oid, clazz, result).asPrismObject());
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> focus) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = new OperationResult(OPERATION_GET_PRINCIPAL);
        return getPrincipal(focus, null, result);
    }

    @Override
    public GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> focus, AuthorizationTransformer authorizationTransformer, OperationResult result) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (focus == null) {
            return null;
        }
        securityContextManager.setTemporaryPrincipalOid(focus.getOid());
        try {
            PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
            LifecycleStateModelType lifecycleModel = getLifecycleModel(focus, systemConfiguration);

            focusComputer.recompute(focus, lifecycleModel);
            GuiProfiledPrincipal principal = new GuiProfiledPrincipal(focus.asObjectable());
            initializePrincipalFromAssignments(principal, systemConfiguration, authorizationTransformer);
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

                if (!(principal instanceof GuiProfiledPrincipal)) {
                    continue;
                }

                List<SessionInformation> sessionInfos = sessionRegistry.getAllSessions(principal, false);
                if (sessionInfos == null || sessionInfos.isEmpty()) {
                    continue;
                }
                GuiProfiledPrincipal midPointPrincipal = (GuiProfiledPrincipal) principal;

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

                if (!(principal instanceof GuiProfiledPrincipal)) {
                    continue;
                }

                GuiProfiledPrincipal midPointPrincipal = (GuiProfiledPrincipal) principal;
                if (!principalOids.contains(midPointPrincipal.getOid())) {
                    continue;
                }

                List<SessionInformation> sessionInfos = sessionRegistry.getAllSessions(principal, false);
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
            systemConfiguration = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                    null, result);
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

    private PrismObject<FocusType> findByUsername(String username, Class<? extends FocusType> clazz, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PolyString usernamePoly = new PolyString(username);
        ObjectQuery query = ObjectQueryUtil.createNormNameQuery(usernamePoly, prismContext);
        LOGGER.trace("Looking for user, query:\n" + query.debugDump());

        List<PrismObject<FocusType>> list = repositoryService.searchObjects((Class<FocusType>) clazz, query, null, result);
        LOGGER.trace("Users found: {}.", list.size());
        if (list.size() != 1) {
            return null;
        }
        return list.get(0);
    }

    private void initializePrincipalFromAssignments(GuiProfiledPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, AuthorizationTransformer authorizationTransformer) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(GuiProfiledPrincipalManagerImpl.class.getName() + ".initializePrincipalFromAssignments");
        OperationResult result = task.getResult();
        try {
            guiProfileCompiler.compileUserProfile(principal, systemConfiguration, authorizationTransformer, task, result);
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
    public <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object) {
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
            // TODO: get owner from roleMembershipRef;relation=owner (MID-5689)

        } else if (object.canRepresent(TaskType.class)) {
            ObjectReferenceType ownerRef = ((TaskType) (object.asObjectable())).getOwnerRef();
            if (ownerRef != null && ownerRef.getOid() != null && ownerRef.getType() != null) {
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
            PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
            LifecycleStateModelType lifecycleModel = getLifecycleModel((PrismObject<UserType>) owner, systemConfiguration);
            focusComputer.recompute((PrismObject<UserType>) owner, lifecycleModel);
        }
        return owner;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return getPrincipal(username, FocusType.class);
        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {

        String userNameEffective = username;
        Class<? extends FocusType> focusType = UserType.class;
        try {
            if (ctx instanceof MidpointDirContextAdapter && ((MidpointDirContextAdapter) ctx).getNamingAttr() != null) {
                userNameEffective = resolveLdapName(ctx, username, ((MidpointDirContextAdapter) ctx).getNamingAttr());
                focusType = ((MidpointDirContextAdapter) ctx).getFocusType();
            }
            return getPrincipal(userNameEffective, focusType);

        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException("UserProfileServiceImpl.unknownUser", e);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | NamingException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // TODO Auto-generated method stub

    }

    private String resolveLdapName(DirContextOperations ctx, String username, String ldapNamingAttr) throws NamingException, ObjectNotFoundException {
        Attribute ldapResponse = ctx.getAttributes().get(ldapNamingAttr);
        if (ldapResponse != null) {
            if (ldapResponse.size() == 1) {
                Object namingAttrValue = ldapResponse.get(0);

                if (namingAttrValue != null) {
                    return namingAttrValue.toString().toLowerCase();
                }
            } else {
                throw new ObjectNotFoundException("Bad response"); // naming attribute contains multiple values
            }
        }
        return username; // fallback to typed-in username in case ldap value is missing
    }
}

