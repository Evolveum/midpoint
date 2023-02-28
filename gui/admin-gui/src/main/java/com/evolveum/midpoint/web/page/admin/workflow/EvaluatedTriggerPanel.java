/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerDto;

public class EvaluatedTriggerPanel extends BasePanel<EvaluatedTriggerDto> {

    private static final String ID_MESSAGE = "message";
    private static final String ID_CHILDREN = "children";

    public EvaluatedTriggerPanel(String id, IModel<EvaluatedTriggerDto> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        add(AttributeAppender.append("class", "m-2 p-2 border rounded"));
        add(AttributeAppender.append("class", () -> getModelObject().isHighlighted() ? "bg-warning" : null));

        add(new Label(ID_MESSAGE, () -> LocalizationUtil.translateMessage(getModelObject().getMessage())));

        EvaluatedTriggerGroupPanel children = new EvaluatedTriggerGroupPanel(ID_CHILDREN, () -> getModelObject().getChildren());
        children.add(new VisibleBehaviour(() -> !getModelObject().getChildren().getTriggers().isEmpty()));
        add(children);
    }

}
