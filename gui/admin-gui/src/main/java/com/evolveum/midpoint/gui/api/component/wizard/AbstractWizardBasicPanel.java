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
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

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
        addBreadcrumb();
        initLayout();
    }

    private void addBreadcrumb() {
        List<Breadcrumb> breadcrumbs = getBreadcrumb();
        IModel<String> breadcrumbLabelModel = getBreadcrumbLabel();
        String breadcrumbLabel = breadcrumbLabelModel.getObject();

        if (breadcrumbs.isEmpty() && StringUtils.isNotEmpty(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel));
        return;
        }

        String lastBreadcrumbLabel = breadcrumbs.get(breadcrumbs.size() - 1).getLabel().getObject();
        if (StringUtils.isNotEmpty(lastBreadcrumbLabel)
                && StringUtils.isNotEmpty(breadcrumbLabel)
                && !lastBreadcrumbLabel.equals(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel));
        }
    }

    @NotNull protected abstract IModel<String> getBreadcrumbLabel();

    private void removeLastBreadcrumb() {
        int index = getBreadcrumb().size() - 1;
        getBreadcrumb().remove(index);
    }

    private void initLayout() {

        IModel<List<Breadcrumb>> breadcrumb = () -> getBreadcrumb();
        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, breadcrumb) {

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
        breadcrumbs.add(new VisibleBehaviour(() -> getBreadcrumb().size() > 1));

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
                removeLastBreadcrumb();
                onExitPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(AttributeAppender.append("class", "btn btn-outline-primary"));
        buttons.add(exit);

        addCustomButtons(buttons);
        add(buttons);
    }

    private List<Breadcrumb> getBreadcrumb() {
        PageBase page = getPageBase();
        if (page instanceof PageResource) {
            return ((PageResource)page).getWizardBreadcrumbs();
        }
        return List.of();
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
