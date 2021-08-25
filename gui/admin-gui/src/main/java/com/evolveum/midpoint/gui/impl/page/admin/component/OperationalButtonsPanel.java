/*
 * Copyright (C) 2020-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import java.util.Iterator;

public class OperationalButtonsPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STATE_BUTTONS = "stateButtons";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";

    private final LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<>(false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected ExecuteChangeOptionsDto load() {
            return ExecuteChangeOptionsDto.createFromSystemConfiguration();
        }
    };

    public OperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        super(id, wrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        RepeatingView repeatingView = new RepeatingView(ID_BUTTONS);
        add(repeatingView);
        createSaveButton(repeatingView);

        addButtons(repeatingView);


        RepeatingView stateButtonsView = new RepeatingView(ID_STATE_BUTTONS);
        add(stateButtonsView);

        addStateButtons(stateButtonsView);

        //TODO temporary
//        ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS,
//                executeOptionsModel, true, false) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void reloadPanelOnOptionsUpdate(AjaxRequestTarget target) {
//                target.add(OperationalButtonsPanel.this);
//            }
//        };
//        optionsPanel.setOutputMarkupId(true);
//        optionsPanel.add(new VisibleEnableBehaviour() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return getOptionsPanelVisibility();
//            }
//
//        });
//        add(optionsPanel);
    }

    protected void addButtons(RepeatingView repeatingView) {

    }

    private void createSaveButton(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SAVE, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton save = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(), Model.of("Save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
//        save.add(getVisibilityForSaveButton());
        save.setOutputMarkupId(true);
        save.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(save);

    }

    protected void savePerformed(AjaxRequestTarget target) {

    }

    protected void addStateButtons(RepeatingView stateButtonsView) {

    }

    public boolean buttonsExist(){
        RepeatingView repeatingView = (RepeatingView) get(ID_BUTTONS);
        boolean buttonsExist = repeatingView != null && repeatingView.iterator().hasNext();
        if (buttonsExist) {
            Iterator<Component> buttonsIt = repeatingView.iterator();
            while (buttonsIt.hasNext()) {
                Component comp = buttonsIt.next();
                comp.configure();
                if (comp.isVisible()){
                    return true;
                }
            }
        }
        return false;
    }

    //TODO temporary
    protected boolean getOptionsPanelVisibility() {
        if (getModelObject().isReadOnly()) {
            return false;
        }
        return ItemStatus.NOT_CHANGED != getModelObject().getStatus()
                || getModelObject().canModify();
    }

    public ExecuteChangeOptionsDto getExecuteChangeOptions() {
        ExecuteChangeOptionsPanel optionsPanel = (ExecuteChangeOptionsPanel) get(ID_EXECUTE_OPTIONS);
        return optionsPanel.getModelObject();
    }
}
