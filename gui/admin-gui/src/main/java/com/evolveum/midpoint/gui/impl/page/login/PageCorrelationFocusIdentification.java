/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationUseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@PageDescriptor(urls = {
@Url(mountUrl = "/correlation", matchUrlForSecurity = "/correlation")},
        permitAll = true,  loginPage = true, authModule = AuthenticationModuleNameConstants.CORRELATION)   //todo remove permit all later : [KV] why?
public class PageCorrelationFocusIdentification extends PageAbstractAttributeVerification {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCorrelationFocusIdentification.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageCorrelationFocusIdentification.class);
    private static final String OPERATION_DETERMINE_CORRELATOR_SETTINGS = DOT_CLASS + "determineCorrelatorSettings";

    private static final String ID_CORRELATE = "correlate";

    private String archetypeOid;

    private boolean allowUndefinedArchetype;

    public PageCorrelationFocusIdentification() {
    }

    @Override
    protected String getModuleTypeName() {
        return AuthenticationModuleNameConstants.CORRELATION;
    }

    @Override
    protected DynamicFormPanel<UserType> getDynamicForm() {
        return null;
    }

    @Override
    protected List<VerificationAttributeDto> loadAttrbuteVerificationDtoList() {
        return getCurrentCorrelationItemPathList()
                .stream()
                .map(p -> new VerificationAttributeDto(new ItemPathType(p)))
                .collect(Collectors.toList());
    }

    private PathSet getCurrentCorrelationItemPathList() {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (moduleAuthentication == null
                && !AuthenticationModuleNameConstants.CORRELATION.equals(moduleAuthentication.getModuleTypeName())) {
            getSession().error(getString("No authentication module is found"));
            throw new RestartResponseException(PageError.class);
        }
        if (StringUtils.isEmpty(moduleAuthentication.getModuleIdentifier())) {
            getSession().error(getString("No module identifier is defined"));
            throw new RestartResponseException(PageError.class);
        }
        CorrelationAuthenticationModuleType module = getModuleByIdentifier(moduleAuthentication.getModuleIdentifier());
        if (module == null) {
            getSession().error(getString("No module with identifier \"" + moduleAuthentication.getModuleIdentifier() + "\" is found"));
            throw new RestartResponseException(PageError.class);
        }
        String correlatorName = module.getCorrelationRuleIdentifier();

        var index = 1; //todo this should be the index of the currently processing correlator

        Task task = createAnonymousTask(OPERATION_DETERMINE_CORRELATOR_SETTINGS);
        PathSet pathList;
        try {
            pathList = getCorrelationService().determineCorrelatorConfiguration(new CorrelatorDiscriminator(correlatorName, CorrelationUseType.USERNAME_RECOVERY), archetypeOid, task, task.getResult());
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine correlator configuration", e);
            pathList = new PathSet();
        }

        return pathList;
    }

    private CorrelationAuthenticationModuleType getModuleByIdentifier(String moduleIdentifier) {
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }

        //TODO security policy defined for archetype? e.g. not null user but empty focus with archetype. but wouldn't it be hack?
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            getSession().error(getString("Security policy not found"));
            throw new RestartResponseException(PageError.class);
        }
        return securityPolicy.getAuthentication().getModules().getCorrelation()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()) || moduleIdentifier.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void initModels() {
        super.initModels();
        allowUndefinedArchetype = loadAllowUndefinedArchetypeConfig();
        archetypeOid = loadArchetypeOidFromAuthentication();
    }

    private boolean loadAllowUndefinedArchetypeConfig() {
        var securityPolicy = resolveSecurityPolicy(null);
        var archetypeSelectionModule = ConfigurationLoadUtil.loadArchetypeSelectionModuleForLoginRecovery(
                PageCorrelationFocusIdentification.this, securityPolicy);
        return Boolean.TRUE.equals(archetypeSelectionModule.isAllowUndefinedArchetype());

    }

    private String loadArchetypeOidFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            return "./spring_security_login";
        }
        return ((MidpointAuthentication) authentication).getArchetypeOid();
    }

    //actually, we don't need to search for user via query
    @Override
    protected ObjectQuery createStaticFormQuery() {
        String username = "";
        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();
    }

    //todo correlation will be called from auth module, this is just an attempt to play with correlation

    private UserType createUser() {
        UserType user = new UserType();
        return user;
    }

}
