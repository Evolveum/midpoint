/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;

import com.evolveum.midpoint.model.common.stringpolicy.StringPolicyUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class StringLimitationPanel extends BasePanel<StringLimitationResult> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_LIMITATION = "limitation";

    private boolean addTabIndex = false;

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
            if (getModelObject().isSuccess()) {
                cssClass = " fa-check";
            } else {
                cssClass = " fa-times";
            }
            return cssClass;
        }));
        icon.setOutputMarkupId(true);
        icon.add(AttributeModifier.append(
                "title",
                (IModel<String>) () -> LocalizationUtil.translate("StringLimitationPanel.decision." + getModelObject().isSuccess())));
        icon.add(AttributeModifier.append(
                "aria-label",
                (IModel<String>) () -> LocalizationUtil.translate("StringLimitationPanel.decision." + getModelObject().isSuccess())));
        icon.add(AttributeModifier.append("tabindex", () -> addTabIndex ? "0" : null));
        add(icon);
     LabelWithHelpPanel label = new LabelWithHelpPanel(ID_LIMITATION, getLimitationLabelModel()){
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getHelpModel() {
                String help = LocalizationUtil.translatePolyString(StringLimitationPanel.this.getModelObject().getHelp());
                if (StringUtils.isEmpty(help)) {
                    return Model.of();
                }
                if (StringLimitationPanel.this.getModelObject().isCharactersSequence()) {
                    return Model.of(getCharactersWithClarifiedSpecialSymbolsWrappedToSpan(help));
                }
                return Model.of(LocalizationUtil.translatePolyString(StringLimitationPanel.this.getModelObject().getHelp()));
            }
        };
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.append("tabindex", () -> addTabIndex ? "-1" : null));
        add(label);
    }

    private IModel<String> getLimitationLabelModel() {
        return () -> StringUtils.joinWith(
                " ", LocalizationUtil.translatePolyString(getModelObject().getName()), getRulesLabel());
    }

    private String getRulesLabel() {
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
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    public void enableTabIndex() {
        addTabIndex = true;
    }


    private String getCharactersWithClarifiedSpecialSymbolsWrappedToSpan(String help) {
        StringBuilder sb = new StringBuilder();

        char[] charArray = help.toCharArray();
        for (char ch : charArray) {
            sb.append("<span aria-label=\"");
            var translationKeyForCharacter = StringPolicyUtils.getTranslationKeyForCharacter(ch);
            if (translationKeyForCharacter != null) {
                sb.append(getString(translationKeyForCharacter));
            } else {
                sb.append(ch);
            }
            sb.append("\"><span aria-hidden=\"true\">");
            sb.append(ch);
            sb.append("</span></span>");
        }
        return sb.toString();
    }
}
