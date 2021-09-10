/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

public class FocusOperationalButtonsPanel<F extends FocusType> extends AssignmentHolderOperationalButtonsPanel<F> {

    private static final String ID_EXECUTE_OPTIONS = "executeOptions";

    public FocusOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<F>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS) {

            @Override
            protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
                target.add(FocusOperationalButtonsPanel.this);
            }
        };
        optionsPanel.setOutputMarkupId(true);
        optionsPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getOptionsPanelVisibility();
            }

        });
        add(optionsPanel);
    }

    @Override
    protected void addButtons(RepeatingView repeatingView) {
        super.addButtons(repeatingView);
        createPreviewButton(repeatingView);
    }

    private void createPreviewButton(RepeatingView repeatingView) {
        AjaxIconButton preview = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_PREVIEW), Model.of("Preview changes")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                previewPerformed(ajaxRequestTarget);
            }
        };
        preview.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(preview);
    }

    protected boolean getOptionsPanelVisibility() {
        if (getModelObject().isReadOnly()) {
            return false;
        }
        return ItemStatus.NOT_CHANGED != getModelObject().getStatus()
                || getModelObject().canModify();
    }

    public ExecuteChangeOptionsDto getExecuteChangeOptions() {
        ExecuteChangeOptionsPanel optionsPanel = (ExecuteChangeOptionsPanel) get(ID_EXECUTE_OPTIONS);
        return optionsPanel != null ? optionsPanel.getModelObject() : new ExecuteChangeOptionsDto();
    }

    //TODO make abstract
    protected void previewPerformed(AjaxRequestTarget target) {

    }


}
