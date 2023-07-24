/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@PageDescriptor(urls = {
@Url(mountUrl = "/correlation", matchUrlForSecurity = "/correlation")},
        permitAll = true,  loginPage = true, authModule = AuthenticationModuleNameConstants.CORRELATION)   //todo remove permit all later : [KV] why?
public class PageCorrelationFocusIdentification extends PageAbstractAttributeVerification {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCorrelationFocusIdentification.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageCorrelationFocusIdentification.class);
    private static final String OPERATION_LOAD_ARCHETYPE = DOT_CLASS + "loadArchetype";
    private static final String OPERATION_LOAD_OBJECT_TEMPLATE = DOT_CLASS + "loadObjectTemplate";
    private static final String OPERATION_LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String OPERATION_CORRELATE = DOT_CLASS + "correlate";
    private static final String OPERATION_DETERMINE_CORRELATOR_SETTINGS = DOT_CLASS + "determineCorrelatorSettings";

    private static final String ID_CORRELATE = "correlate";

    private LoadableDetachableModel<ObjectTemplateType> objectTemplateModel;
//    private LoadableDetachableModel<List<ItemsSubCorrelatorType>> correlatorsModel;
    private String archetypeOid;

    public PageCorrelationFocusIdentification(PageParameters parameters) {
        var archetypeOidParamValue = parameters.get(ARCHETYPE_OID_PARAMETER);
        archetypeOid = archetypeOidParamValue != null ? archetypeOidParamValue.toString() : null;
    }

    @Override
    protected String getUrlProcessingLogin() {
        //todo
        return "";
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
//        return moduleAttributes.stream()
//                .map(attr -> new VerificationAttributeDto(attr))
//                .collect(Collectors.toList());

        var index = 1; //todo this should be the index of the currently processing correlator

        Task task = createAnonymousTask(OPERATION_DETERMINE_CORRELATOR_SETTINGS);
        PathSet pathList;
        try {
            pathList = getCorrelationService().determineCorrelatorConfiguration(new CorrelatorDiscriminator(correlatorName, CorrelationUseType.USERNAME_RECOVERY), archetypeOid, task, task.getResult());
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine correlator configuration", e);
            pathList = new PathSet();
        }

//        new ArrayList<>();
//        if (correlatorsModel == null || correlatorsModel.getObject() == null) {
//            return pathList;
//        }
//        if (CollectionUtils.isNotEmpty(correlatorsModel.getObject())) {
//            var correlator = correlatorsModel.getObject().get(index);
//            correlator.getItem().forEach(item -> {
//                ItemPathType pathBean = item.getRef();
//                if (pathBean != null) {
//                    pathList.add(pathBean.getItemPath());
//                }
//            });
//        }
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
        objectTemplateModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected ObjectTemplateType load() {
                var archetype = loadArchetype();
                return loadObjectTemplateForArchetype(archetype);
            }
        };
//        correlatorsModel = new LoadableDetachableModel<>() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected List<ItemsSubCorrelatorType> load() {
//                var objectTemplate = objectTemplateModel.getObject();
//                if (objectTemplate == null) {
//                    //todo show warning?
//                    return Collections.emptyList();
//                }
//                return getCorrelators(objectTemplate);
//            }
//        };
    }

    @Override
    protected void initCustomLayout() {
        super.initCustomLayout();

        var correlateButton = new AjaxButton(ID_CORRELATE) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                correlate(target);
            }
        };
        getForm().add(correlateButton);
    }

    private ArchetypeType loadArchetype() {
        return runPrivileged((Producer<ArchetypeType>) () -> {
            if (archetypeOid == null) {
                return null;
            }
            var task = createAnonymousTask(OPERATION_LOAD_ARCHETYPE);
            var result = new OperationResult(OPERATION_LOAD_ARCHETYPE);
            PrismObject<ArchetypeType> archetype = WebModelServiceUtils.loadObject(ArchetypeType.class,
                    archetypeOid, PageCorrelationFocusIdentification.this, task, result);
            return archetype == null ? null : archetype.asObjectable();
        });
    }

    private ObjectTemplateType loadObjectTemplateForArchetype(ArchetypeType archetype) {
        return runPrivileged((Producer<ObjectTemplateType>) () -> {
            var archetypePolicy = archetype.getArchetypePolicy();
            if (archetypePolicy == null) {
                return null;
            }
            var objectTemplateRef = archetypePolicy.getObjectTemplateRef();
            var loadObjectTemplateTask = createAnonymousTask(OPERATION_LOAD_OBJECT_TEMPLATE);
            var result = new OperationResult(OPERATION_LOAD_OBJECT_TEMPLATE);
            PrismObject<ObjectTemplateType> objectTemplate = WebModelServiceUtils.resolveReferenceNoFetch(objectTemplateRef,
                    PageCorrelationFocusIdentification.this, loadObjectTemplateTask, result);
            return objectTemplate == null ? null : objectTemplate.asObjectable();
        });
    }

//    private List<ItemsSubCorrelatorType> getCorrelators(ObjectTemplateType objectTemplate) {
//        var correlatorConfiguration = determineCorrelatorConfiguration(objectTemplate);
//        if (correlatorConfiguration == null) {
//            return Collections.emptyList();
//        }
//
//        return ((CompositeCorrelatorType) correlatorConfiguration.getConfigurationBean())
//                .getItems()
//                .stream()
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }

//    //todo this method will be replaced so that correlation service will return the list of correlation items
//    private CorrelatorConfiguration determineCorrelatorConfiguration(ObjectTemplateType objectTemplate) {
//        OperationResult result = new OperationResult(OPERATION_LOAD_SYSTEM_CONFIGURATION);
//        try {
//            var systemConfiguration = getModelInteractionService().getSystemConfiguration(result);
//            return getCorrelationService().determineCorrelatorConfiguration(objectTemplate, systemConfiguration);
//        } catch (SchemaException | ObjectNotFoundException e) {
//            LoggingUtils.logException(LOGGER, "Couldn't determine correlation configuration.", e);
//        }
//        return null;
//    }

    //actually, we don't need to search for user via query
    @Override
    protected ObjectQuery createStaticFormQuery() {
        String username = "";
        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();
    }

    //todo correlation will be called from auth module, this is just an attempt to play with correlation
    private void correlate(AjaxRequestTarget target) {
        var task = createAnonymousTask(OPERATION_CORRELATE);
        var result = new OperationResult(OPERATION_CORRELATE);
        var user = createUser();
        var objectTemplate = objectTemplateModel.getObject();
        try {
            getCorrelationService().correlate(user, objectTemplate, task, result);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine correlation configuration.", e);
            result.recordFatalError(e);
            showResult(result);
            target.add(getFeedbackPanel());
        }


    }

    private UserType createUser() {
        UserType user = new UserType();
        return user;
    }

}
