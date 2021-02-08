/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordLimitationsPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author skublik
 */

public class PasswordPolicyValidationPanel extends BasePanel<List<StringLimitationResult>> {

    private static final String ID_RESULT_ICON = "resultIcon";
    private static final String ID_INFO_ICON = "infoIcon";
    private static final String ID_POLICY_VALIDATION_POPOVER = "policyValidationPopover";

    public PasswordPolicyValidationPanel(String id, IModel<List<StringLimitationResult>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<DisplayType> displayModel = (IModel) () -> {
            OperationResultStatusPresentationProperties status = OperationResultStatusPresentationProperties.SUCCESS;
            for (StringLimitationResult limitation : getModelObject()) {
                if (!limitation.isSuccess()){
                    status = OperationResultStatusPresentationProperties.FATAL_ERROR;
                }
            }
            return WebComponentUtil.createDisplayType(status.getIcon() + " fa-lg", "",
                    getPageBase().createStringResource(status.getStatusLabelKey()).getString());
        };
        ImagePanel resultIcon = new ImagePanel(ID_RESULT_ICON, displayModel);
        resultIcon.setOutputMarkupId(true);
        add(resultIcon);


        ImagePanel infoPanel = new ImagePanel(
                ID_INFO_ICON, Model.of(WebComponentUtil.createDisplayType("fa fa-info-circle")));
        add(infoPanel);

        PasswordLimitationsPanel validationPanel = new PasswordLimitationsPanel(ID_POLICY_VALIDATION_POPOVER, getModel());
        validationPanel.setOutputMarkupId(true);
        add(validationPanel);
    }

    public void refreshValidationPopup(AjaxRequestTarget target){
        target.add(get(ID_RESULT_ICON));
        ((PasswordLimitationsPanel)get(ID_POLICY_VALIDATION_POPOVER)).refreshItems(target);
    }
}
