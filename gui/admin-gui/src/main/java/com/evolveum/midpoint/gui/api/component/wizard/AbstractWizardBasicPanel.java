/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

public abstract class AbstractWizardBasicPanel extends BasePanel {


    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_NAME = "bcName";
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
        getBreadcrumbsModel().getObject().add(new Breadcrumb(getBreadcrumbLabel()));
    }

    protected abstract IModel<String> getBreadcrumbLabel();

    protected void removeLastBreadcrumb() {
        int index = getBreadcrumbsModel().getObject().size() - 1;
        getBreadcrumbsModel().getObject().remove(index);
    }

    private void initLayout() {
        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, getBreadcrumbsModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                int index = item.getIndex() + 1;
                if (index == getList().size()) {
                    item.add(AttributeAppender.append("class", "text-primary"));
                }

                Label bcName = new Label(ID_BC_NAME, item.getModelObject().getLabel());
                item.add(bcName);

                item.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
            }
        };
        add(breadcrumbs);
        breadcrumbs.add(new VisibleBehaviour(() -> getBreadcrumbsModel().getObject().size() > 1));

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
                getExitLabel()) {
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

    private IModel<List<Breadcrumb>> getBreadcrumbsModel() {
        PageBase page = getPageBase();
        if (page instanceof PageResource) {
            return ((PageResource)page).getWizardBreadcrumbs();
        }
        return Model.ofList(List.of());
    }

    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("WizardPanel.exit");
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
        getPageBase().navigateToNext(PageResources.class);
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    protected IModel<String> getSubTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".text");
    };

    protected IModel<String> getTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".subText");
    }
}
