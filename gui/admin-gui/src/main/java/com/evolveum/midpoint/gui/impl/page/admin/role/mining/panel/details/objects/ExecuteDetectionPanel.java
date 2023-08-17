/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.objects;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

public class ExecuteDetectionPanel extends BasePanel<String> implements Popupable {

    private static final String ID_FREQUENCY_FORM = "thresholds_form";
    private static final String ID_FREQUENCY_SUBMIT = "ajax_submit_link";

    IModel<PrismContainerWrapper<RoleAnalysisDetectionOptionType>> roleAnalysisProcessModeType;
    DetectionOption detectionOption;

    public ExecuteDetectionPanel(String id, IModel<String> messageModel,
            IModel<PrismContainerWrapper<RoleAnalysisDetectionOptionType>> roleAnalysisProcessModeType,
            DetectionOption defaultDetectionOptions) {
        super(id, messageModel);
        this.roleAnalysisProcessModeType = roleAnalysisProcessModeType;
        this.detectionOption = defaultDetectionOptions;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ItemPanelSettings itemPanelSettings = new ItemPanelSettingsBuilder().visibilityHandler((ItemVisibilityHandler) wrapper -> {
            if (wrapper.getItemName().equivalent(RoleAnalysisDetectionOptionType.F_DETECTION_PROCESS_MODE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        }).build();

        VerticalFormPrismContainerPanel<RoleAnalysisDetectionOptionType> detectionOptionForm
                = new VerticalFormPrismContainerPanel<>("panel", roleAnalysisProcessModeType, itemPanelSettings);

        add(detectionOptionForm);
        Form<?> components = frequencyForm();
        components.setOutputMarkupId(true);
        add(components);

    }

    public Form<?> frequencyForm() {

        Form<?> form = new Form<Void>(ID_FREQUENCY_FORM);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_FREQUENCY_SUBMIT, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                PrismContainerWrapper<RoleAnalysisDetectionOptionType> object = roleAnalysisProcessModeType.getObject();

                RoleAnalysisDetectionOptionType realValue = null;
                try {
                    realValue = object.getValue().getRealValue();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                detectionOption.setMinFrequencyThreshold(realValue.getFrequencyRange().getMin());
                detectionOption.setMaxFrequencyThreshold(realValue.getFrequencyRange().getMax());
                detectionOption.setMinRoles(realValue.getMinRolesOccupancy());
                detectionOption.setMinUsers(realValue.getMinUserOccupancy());

                performAction(target, detectionOption);

                getPageBase().hideMainPopup(target);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
    }

    public void performAction(AjaxRequestTarget target, DetectionOption detectionOption) {

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel(""
                + "RoleMining.members.execute.search.panel.title");
    }

}
