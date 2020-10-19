/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author lazyman
 */
public class CheckFormGroup extends BasePanel<Boolean> {

    private static final String ID_CHECK = "check";
    private static final String ID_CHECK_WRAPPER = "checkWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelSize, String textSize) {
        this(id, value, label, null, false, labelSize, textSize);
    }

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label, String tooltipKey,
            boolean isTooltipInModal, String labelSize, String textSize) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModal, labelSize, textSize);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelSize, String textSize) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        add(labelContainer);
        Label l = new Label(ID_LABEL, label);
        l.setOutputMarkupId(true);

        if (StringUtils.isNotEmpty(labelSize)) {
            labelContainer.add(AttributeAppender.prepend("class", labelSize));
        }
        labelContainer.add(l);

        Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
        tooltipLabel.add(new AttributeAppender("data-original-title",
                (IModel<String>) () -> getString(tooltipKey)));
        tooltipLabel.add(new InfoTooltipBehavior(isTooltipInModal));
        tooltipLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return tooltipKey != null;
            }
        });
        tooltipLabel.setOutputMarkupId(true);
        tooltipLabel.setOutputMarkupPlaceholderTag(true);
        labelContainer.add(tooltipLabel);
        labelContainer.add(new VisibleBehaviour(() -> getLabelVisible()) {
        });

        WebMarkupContainer checkWrapper = new WebMarkupContainer(ID_CHECK_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            checkWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(checkWrapper);
        checkWrapper.setOutputMarkupId(true);

        CheckBox check = new CheckBox(ID_CHECK, getModel());
        check.setOutputMarkupId(true);
        check.setLabel(label);
        checkWrapper.add(check);
        setOutputMarkupId(true);
    }

    protected boolean getLabelVisible() {
        return true;
    }

    public CheckBox getCheck() {
        return (CheckBox) get(ID_CHECK_WRAPPER + ":" + ID_CHECK);
    }

    public Boolean getValue() {
        return getCheck().getModelObject();
    }
}
