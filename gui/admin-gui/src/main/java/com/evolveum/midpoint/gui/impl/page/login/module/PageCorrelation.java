/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.login.dto.CorrelatorConfigDto;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationUseType;

@PageDescriptor(urls = {
@Url(mountUrl = "/correlation", matchUrlForSecurity = "/correlation")},
        permitAll = true,  loginPage = true, authModule = AuthenticationModuleNameConstants.CORRELATION)
public class PageCorrelation extends PageAbstractAttributeVerification<CorrelationModuleAuthentication> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCorrelation.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageCorrelation.class);
    private static final String OPERATION_DETERMINE_CORRELATOR_SETTINGS = DOT_CLASS + "determineCorrelatorSettings";

    private LoadableModel<CorrelatorConfigDto> correlatorModel;

    public PageCorrelation() {
        super();
    }


    //TODO refactor
    @Override
    protected List<VerificationAttributeDto> loadAttrbuteVerificationDtoList() {
        return correlatorModel.getObject().getAttributeDtoList();
    }

    @Override
    protected void initModels() {
        correlatorModel = new LoadableModel<>() {
            @Override
            protected CorrelatorConfigDto load() {
                CorrelationModuleAuthentication module = getAuthenticationModuleConfiguration();
                String correlatorName = module.getCurrentCorrelatorIdentifier();

                String archetypeOid = null;
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof MidpointAuthentication mpAuthentication) {
                    archetypeOid = mpAuthentication.getArchetypeOid();
                }

                return new CorrelatorConfigDto(correlatorName, archetypeOid, getCorrelationAttributePaths(correlatorName, archetypeOid),
                        module.getCurrentCorrelatorIndex());
            }
        };
        super.initModels();
    }

    private List<VerificationAttributeDto> getCorrelationAttributePaths(String correlatorName, String archetypeOid) {
        PathSet paths;
        try {
            Task task = createAnonymousTask(OPERATION_DETERMINE_CORRELATOR_SETTINGS);
            paths = getCorrelationService().determineCorrelatorConfiguration(
                    CorrelatorDiscriminator.forIdentityRecovery(correlatorName), archetypeOid, task, task.getResult());
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine correlator configuration", e);
            paths = new PathSet();
        }

        return paths
                .stream()
                .map(itemPath -> new VerificationAttributeDto(createItemWrapper(itemPath), itemPath))
                .collect(Collectors.toList());
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        super.initModuleLayout(form);

        HiddenField<String> verified = new HiddenField<>(AuthConstants.ATTR_VERIFICATION_CORRELATOR_NAME, new PropertyModel<>(correlatorModel, CorrelatorConfigDto.CORRELATOR_IDENTIFIER));
        verified.setOutputMarkupId(true);
        form.add(verified);

        HiddenField<String> index = new HiddenField<>(AuthConstants.ATTR_VERIFICATION_CORRELATOR_INDEX, new PropertyModel<>(correlatorModel, CorrelatorConfigDto.CORRELATOR_INDEX));
        index.setOutputMarkupId(true);
        form.add(index);
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageCorrelation.title");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageCorrelation.title.description");
    }

    @Override
    protected boolean areAllItemsMandatory(ItemWrapper<?,?> itemWrapper) {
        return true;
    }
}
