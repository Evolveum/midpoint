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
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author skublik
 */

public class PasswordPolicyValidationPanel extends BasePanel<List<StringLimitationResult>> {

    private static final String ID_RESULT_ICON = "resultIcon";
    private static final String ID_INFO_ICON = "infoIcon";
    private static final String ID_POLICY_VALIDATION_POPOVER = "policyValidationPopover";

    public static final String JAVA_SCRIPT_CODE = "$(document).ready(function (){\n"
            + "            $(\".info-icon\").passwordValidatorPopover(\".info-icon\", \".password-validator-popover\");\n"
            + "        });\n";
    private Model<Boolean> isAfterInitialization = Model.of(false);

    public PasswordPolicyValidationPanel(String id, LoadableDetachableModel<List<StringLimitationResult>> model) {
        super(id, model);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(JAVA_SCRIPT_CODE));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<DisplayType> displayModel = (IModel) () -> {
            if (!Boolean.TRUE.equals(isAfterInitialization.getObject())){
                return null;
            }
            String status = "fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED + " fa-lg";
            String titleKey = "PasswordPolicyValidationPanel.valid";
            for (StringLimitationResult limitation : getModelObject()) {
                if (!limitation.isSuccess()){
                    status = "fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED + " fa-lg";
                    titleKey = "PasswordPolicyValidationPanel.invalid";
                }
            }
            return GuiDisplayTypeUtil.createDisplayType(status, "", getPageBase().createStringResource(titleKey).getString());
        };
        ImagePanel resultIcon = new ImagePanel(ID_RESULT_ICON, displayModel);
        resultIcon.setOutputMarkupId(true);
        add(resultIcon);


        ImagePanel infoPanel = new ImagePanel(ID_INFO_ICON, Model.of("fa fa-info-circle"), Model.of(getString("ChangePasswordPanel.passwordValidation")));
        infoPanel.setOutputMarkupId(true);
        infoPanel.setIconRole(IconColumn.IconRole.BUTTON);
        add(infoPanel);

        PasswordLimitationsPanel validationPanel = new PasswordLimitationsPanel(ID_POLICY_VALIDATION_POPOVER,
                (LoadableDetachableModel<List<StringLimitationResult>>) getModel());
        validationPanel.setOutputMarkupId(true);
        add(validationPanel);
    }
}
