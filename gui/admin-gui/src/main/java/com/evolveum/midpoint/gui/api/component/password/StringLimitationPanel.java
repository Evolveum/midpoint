/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class StringLimitationPanel extends BasePanel<StringLimitationResult> {

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "name";
    private static final String ID_RULES = "rules";

    public StringLimitationPanel(String id, IModel<StringLimitationResult> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        Label icon = new Label(ID_ICON);
        icon.add(AttributeModifier.append("class", (IModel<String>) () -> {
            String cssClass;
            if (Boolean.TRUE.equals(getModelObject().isSuccess())) {
                cssClass = " fa-check";
            } else {
                cssClass = " fa-times";
            }
            return cssClass;
        }));
        icon.setOutputMarkupId(true);
        icon.add(AttributeModifier.append(
                "title",
                (IModel<String>) () -> LocalizationUtil.translate("StringLimitationPanel.decision." + Boolean.TRUE.equals(getModelObject().isSuccess()))));
        add(icon);

        LabelWithHelpPanel label = new LabelWithHelpPanel(ID_NAME, Model.of(WebComponentUtil.getTranslatedPolyString(getModelObject().getName()))){
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(StringLimitationPanel.this.getModelObject().getHelp()));
            }
        };
        label.setOutputMarkupId(true);
        add(label);

        IModel<String> rulesModel = getRulesModel();
        Label rules = new Label(ID_RULES, rulesModel);
        rules.setOutputMarkupId(true);
        rules.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(rulesModel.getObject())));
        add(rules);
    }

    private IModel<String> getRulesModel() {
        List<String> rules = new ArrayList<>();
        if (getModelObject().getMinOccurs() != null) {
            rules.add(PageBase.createStringResourceStatic("StringLimitationPanel.min", getModelObject().getMinOccurs()).getString());
        }
        if (getModelObject().getMaxOccurs() != null) {
            rules.add(PageBase.createStringResourceStatic("StringLimitationPanel.max", getModelObject().getMaxOccurs()).getString());
        }
        if (Boolean.TRUE.equals(getModelObject().isMustBeFirst())) {
            rules.add(PageBase.createStringResourceStatic("StringLimitationPanel.mustBeFirst").getString());
        }
        StringBuilder sb = new StringBuilder("");
        if (!rules.isEmpty()) {
            for(int i = 0; i < rules.size(); i++){
                if (i != 0) {
                    if (i != (rules.size() - 1)) {
                        sb.append(",");
                    } else {
                        sb.append(" ").append(PageBase.createStringResourceStatic("StringLimitationPanel.rules.and").getString());
                    }
                }
                sb.append(" ").append(rules.get(i));
            }

        }
        return new IModel<String>() {
            @Override
            public String getObject() {
                return sb.toString();
            }
        };
    }
}
