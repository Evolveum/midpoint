/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class AbstractWizardBasicPanel extends BasePanel {

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_BUTTONS = "buttons";

    private final ResourceDetailsModel resourceModel;
    public AbstractWizardBasicPanel(String id, ResourceDetailsModel resourceModel) {
        super(id);
        this.resourceModel = resourceModel;
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        Label mainText = new Label(ID_TEXT, getTextModel());
        mainText.add(new VisibleBehaviour(() -> getTextModel().getObject() != null));
        add(mainText);

        Label secondaryText = new Label(ID_SUBTEXT, getSubTextModel());
        secondaryText.add(new VisibleBehaviour(() -> getSubTextModel().getObject() != null));
        add(secondaryText);

        RepeatingView buttons = new RepeatingView(ID_BUTTONS);

        AjaxIconButton exit = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-right-from-bracket"),
                getPageBase().createStringResource("ResourceWizard.exit")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(AttributeAppender.append("class", "btn btn-outline-primary"));
        buttons.add(exit);

        addCustomButtons(buttons);
        add(buttons);
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
        getPageBase().navigateToNext(PageResources.class);
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    protected abstract IModel<String> getSubTextModel();

    protected abstract IModel<String> getTextModel();
}
