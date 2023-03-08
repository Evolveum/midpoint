/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRemediationStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */

public class DefinitionBasicPanel extends BasePanel<CertDefinitionDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LAST_STARTED = "campaignLastStarted";
    private static final String ID_LAST_STARTED_HELP = "campaignLastStartedHelp";
    private static final String ID_LAST_CLOSED = "campaignLastClosed";
    private static final String ID_LAST_CLOSED_HELP = "campaignLastClosedHelp";
    private static final String ID_OWNER_REF_CHOOSER = "ownerRefChooser";
    private static final String ID_REMEDIATION = "remediation";
    private static final String ID_AUTOMATIC_ITERATION_AFTER = "automaticIterationAfter";
    private static final String ID_AUTOMATIC_ITERATION_LIMIT = "automaticIterationLimit";
    private static final String ID_OVERALL_ITERATION_LIMIT = "overallIterationLimit";
    private static final String ID_OUTCOME_STRATEGY = "outcomeStrategy";
    private static final String ID_OUTCOME_STRATEGY_HELP = "outcomeStrategyHelp";
    private static final String ID_STOP_REVIEW_ON = "stopReviewOn";

    public DefinitionBasicPanel(String id, IModel<CertDefinitionDto> model) {
        super(id, model);
        initBasicInfoLayout();
    }

    private void initBasicInfoLayout() {

        final TextField<?> nameField = new TextField<>(ID_NAME, new PropertyModel<>(getModel(), CertDefinitionDto.F_NAME));
        nameField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(nameField);

        final TextArea<?> descriptionField = new TextArea<>(ID_DESCRIPTION, new PropertyModel<>(getModel(), CertDefinitionDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(descriptionField);

        final WebMarkupContainer ownerRefChooser = createOwnerRefChooser(ID_OWNER_REF_CHOOSER);
        ownerRefChooser.setOutputMarkupId(true);
        add(ownerRefChooser);

        DropDownChoice<?> remediation = new DropDownChoice<>(ID_REMEDIATION, new Model<>() {

            @Override
            public AccessCertificationRemediationStyleType getObject() {
                return getModel().getObject().getRemediationStyle();
            }

            @Override
            public void setObject(AccessCertificationRemediationStyleType object) {
                getModel().getObject().setRemediationStyle(object);
            }
        }, WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationRemediationStyleType.class),
                new EnumChoiceRenderer<>(this));
        add(remediation);

        final TextField<String> automaticIterationAfterField = new TextField<>(ID_AUTOMATIC_ITERATION_AFTER,
                new PropertyModel<>(getModel(), CertDefinitionDto.F_AUTOMATIC_ITERATION_AFTER));
        add(automaticIterationAfterField);

        final TextField<Integer> automaticIterationLimitField = new TextField<>(ID_AUTOMATIC_ITERATION_LIMIT,
                new PropertyModel<>(getModel(), CertDefinitionDto.F_AUTOMATIC_ITERATION_LIMIT));
        automaticIterationLimitField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(automaticIterationLimitField);

        final TextField<Integer> overallIterationLimitField = new TextField<>(ID_OVERALL_ITERATION_LIMIT,
                new PropertyModel<>(getModel(), CertDefinitionDto.F_OVERALL_ITERATION_LIMIT));
        overallIterationLimitField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(overallIterationLimitField);

        DropDownChoice<?> outcomeStrategy =
                new DropDownChoice<>(ID_OUTCOME_STRATEGY,
                        new PropertyModel<>(getModel(), CertDefinitionDto.F_OUTCOME_STRATEGY),
                        WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
                        new EnumChoiceRenderer<>(this));
        add(outcomeStrategy);

        add(WebComponentUtil.createHelp(ID_OUTCOME_STRATEGY_HELP));

        Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, (IModel<String>) () -> {
            List<AccessCertificationResponseType> stopOn = getModel().getObject().getStopReviewOn();
            return CertMiscUtil.getStopReviewOnText(stopOn, getPageBase());
        });
        add(stopReviewOn);

        add(new Label(ID_LAST_STARTED, new PropertyModel<>(getModel(), CertDefinitionDto.F_LAST_STARTED)));
        add(new Label(ID_LAST_CLOSED, new PropertyModel<>(getModel(), CertDefinitionDto.F_LAST_CLOSED)));
        add(WebComponentUtil.createHelp(ID_LAST_STARTED_HELP));
        add(WebComponentUtil.createHelp(ID_LAST_CLOSED_HELP));
    }

    private WebMarkupContainer createOwnerRefChooser(String id) {
        ChooseTypePanel tenantRef = new ChooseTypePanel(id,
                new PropertyModel<ObjectViewDto>(getModel(), CertDefinitionDto.F_OWNER)) {

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return UserType.F_NAME;
            }
        };

        return tenantRef;
    }

}
