/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author skublik
 */

public class StringLimitationPanel extends BasePanel<StringLimitationResult> {

    private static final String ID_NAME = "name";
    private static final String ID_HELP = "help";
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
        Label name = new Label(ID_NAME, WebComponentUtil.getTranslatedPolyString(getModelObject().getName()));
        name.setOutputMarkupId(true);
        add(name);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = new IModel<String>() {
            @Override
            public String getObject() {
                return WebComponentUtil.getTranslatedPolyString(getModelObject().getHelp());
            }
        };
        help.add(AttributeModifier.replace("title",createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.setOutputMarkupId(true);
        add(help);

        IModel<String> rulesModel = getRulesModel();
        Label rules = new Label(ID_RULES, rulesModel);
        rules.setOutputMarkupId(true);
        rules.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(rulesModel.getObject())));
        add(rules);
    }

    private IModel<String> getRulesModel() {
        List<String> rules = new ArrayList<>();
        if (getModelObject().getMinOccurs() != null) {
            rules.add(PageBase.createStringResourceStatic(null, "StringLimitationPanel.min", getModelObject().getMinOccurs()).getString());
        }
        if (getModelObject().getMaxOccurs() != null) {
            rules.add(PageBase.createStringResourceStatic(null, "StringLimitationPanel.max", getModelObject().getMaxOccurs()).getString());
        }
        if (Boolean.TRUE.equals(getModelObject().isMustBeFirst())) {
            rules.add(PageBase.createStringResourceStatic(null, "StringLimitationPanel.mustBeFirst").getString());
        }
        StringBuilder sb = new StringBuilder("");
        if (!rules.isEmpty()) {
            for(int i = 0; i < rules.size(); i++){
                if (i != 0) {
                    if (i != (rules.size() - 1)) {
                        sb.append(",");
                    } else {
                        sb.append(" ").append(PageBase.createStringResourceStatic(null, "StringLimitationPanel.rules.and").getString());
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
