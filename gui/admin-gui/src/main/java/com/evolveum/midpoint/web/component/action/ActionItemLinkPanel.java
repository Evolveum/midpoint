/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.List;

public class ActionItemLinkPanel<C extends Containerable> extends BasePanel<AbstractGuiAction<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ACTION_LINK = "actionLink";
    private static final String ID_ACTION_LABEL = "actionLabel";

    private final List<C> objectsToProcess;

    public ActionItemLinkPanel(String id, IModel<AbstractGuiAction<C>> model, List<C> objectsToProcess) {
        super(id, model);
        this.objectsToProcess = objectsToProcess;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        AjaxLink<Void> a = new AjaxLink<>(ID_ACTION_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractGuiAction<C> action = ActionItemLinkPanel.this.getModelObject();
                objectsToProcess.forEach(obj -> action.onActionPerformed(obj, getPageBase(), target));
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }
        };

        add(a);

        Label span = new Label(ID_ACTION_LABEL, getActionLabelModel());
        span.setRenderBodyOnly(true);
        a.add(span);
    }

    private IModel<String> getActionLabelModel() {
        return () -> GuiDisplayTypeUtil.getTranslatedLabel(getModelObject().getActionDisplayType());
    }

//    protected void onError(AjaxRequestTarget target, AbstractGuiAction<C> action) {
//        target.add(getPageBase().getFeedbackPanel());
//    }
}
