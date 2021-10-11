/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.LocalizableMessageModel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author mederly
 */
public class EvaluatedTriggerPanel extends BasePanel<EvaluatedTriggerDto> {

    private static final String ID_FRAME = "frame";
    private static final String ID_MESSAGE = "message";
    private static final String ID_CHILDREN = "children";

    public EvaluatedTriggerPanel(String id, IModel<EvaluatedTriggerDto> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        EvaluatedTriggerDto trigger = getModelObject();
        WebMarkupContainer frame = new WebMarkupContainer(ID_FRAME);
        if (trigger.isHighlighted()) {
            frame.add(new AttributeAppender("style", "background-color: #fcffd3"));     // TODO skin
        }
        add(frame);

        frame.add(new Label(ID_MESSAGE,
                new LocalizableMessageModel(Model.of(trigger.getMessage()), this)));
        EvaluatedTriggerGroupDto children = trigger.getChildren();
        EvaluatedTriggerGroupPanel childrenPanel = new EvaluatedTriggerGroupPanel(ID_CHILDREN, Model.of(children));
        childrenPanel.setVisible(!children.getTriggers().isEmpty());
        frame.add(childrenPanel);
    }

}
