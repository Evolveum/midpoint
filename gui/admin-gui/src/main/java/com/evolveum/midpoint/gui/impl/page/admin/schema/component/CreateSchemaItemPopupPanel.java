/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;

import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyPanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import javax.xml.namespace.QName;

public class CreateSchemaItemPopupPanel extends SimplePopupable {

    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTON_CREATE = "createButton";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_FORM = "form";

    private Fragment footer;

    private final IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> model;

    public CreateSchemaItemPopupPanel(String id, IModel<PrismContainerValueWrapper<PrismItemDefinitionType>> model) {
        super(id, 500, 600, () -> LocalizationUtil.translate("CreateSchemaItemPopupPanel.title"));
        this.model = model;
        initFooter();
        initLayout();
    }

    private void initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxButton cancel = new AjaxButton(ID_BUTTON_CANCEL) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                processHide(target);
            }
        };
        cancel.setOutputMarkupId(true);
        footer.add(cancel);

        AjaxSubmitButton create = new AjaxSubmitButton(ID_BUTTON_CREATE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                createPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                getPanel().visitChildren(FormComponent.class, (formComponent, object) -> {
                    ((FormComponent) formComponent).validate();
                    if (formComponent.hasErrorMessage()) {
                        target.add(formComponent);
                        InputPanel inputParent = formComponent.findParent(InputPanel.class);
                        if (inputParent != null && inputParent.getParent() != null) {
                            target.addChildren(inputParent.getParent(), FeedbackLabels.class);
                        }
                    }
                });
            }
        };
        create.setOutputMarkupId(true);
        footer.add(create);
        this.footer = footer;
    }

    protected void createPerform(AjaxRequestTarget target) {

    }

    private void initLayout() {

        MidpointForm<Object> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        form.setDefaultButton((AjaxSubmitButton)footer.get(ID_BUTTON_CREATE));
        add(form);

        VerticalFormPrismPropertyPanel<QName> panel = new VerticalFormPrismPropertyPanel<>(
                ID_PANEL,
                PrismPropertyWrapperModel.fromContainerValueWrapper(model, PrismItemDefinitionType.F_TYPE),
                new ItemPanelSettingsBuilder()
                        .mandatoryHandler(itemWrapper -> true)
                        .build());
        form.add(panel);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected void processHide(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected VerticalFormPrismPropertyPanel<QName> getPanel() {
        return (VerticalFormPrismPropertyPanel<QName>) get(createComponentPath(ID_FORM, ID_PANEL));
    }
}
