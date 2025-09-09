/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractWizardBasicPanel<AHD extends AssignmentHolderDetailsModel> extends AbstractWizardBasicInitializer {

    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_ICON = "bcIcon";
    private static final String ID_BC_NAME = "bcName";

    private final AHD detailsModel;
    public AbstractWizardBasicPanel(String id, AHD detailsModel) {
        super(id);
        this.detailsModel = detailsModel;
    }

    public AHD getAssignmentHolderDetailsModel() {
        return detailsModel;
    }

    @Override
    protected void onAfterSuperInitialize() {
        addBreadcrumb();
        initLayout();
    }

    private void addBreadcrumb() {
        List<Breadcrumb> breadcrumbs = getBreadcrumb();
        IModel<String> breadcrumbLabelModel = getBreadcrumbLabel();
        IModel<String> breadcrumbIcon = getBreadcrumbIcon();
        String breadcrumbLabel = breadcrumbLabelModel.getObject();
        if (StringUtils.isEmpty(breadcrumbLabel)) {
            return;
        }

        if (breadcrumbs.isEmpty() && StringUtils.isNotEmpty(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel, breadcrumbIcon));
            return;
        }

        String lastBreadcrumbLabel = breadcrumbs.get(breadcrumbs.size() - 1).getLabel().getObject();
        if (StringUtils.isNotEmpty(lastBreadcrumbLabel)
                && StringUtils.isNotEmpty(breadcrumbLabel)
                && !lastBreadcrumbLabel.equals(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel, breadcrumbIcon));
        }
    }

    @NotNull
    protected abstract IModel<String> getBreadcrumbLabel();

    @Nullable
    protected IModel<String> getBreadcrumbIcon() {
        return null;
    }

    protected void removeLastBreadcrumb() {
        if (!getBreadcrumb().isEmpty()) {
            int index = getBreadcrumb().size() - 1;
            getBreadcrumb().remove(index);
        }
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

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleBehaviour(() -> item.getModelObject().getIcon() != null && item.getModelObject().getIcon().getObject() != null));
                bcIcon.add(AttributeModifier.replace("class", item.getModelObject().getIcon()));
                item.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, item.getModelObject().getLabel());
                item.add(bcName);

                item.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
            }
        };
        add(breadcrumbs);
        breadcrumbs.add(new VisibleBehaviour(() -> getBreadcrumb().size() > 1));
    }

    private List<Breadcrumb> getBreadcrumb() {
        PageBase page = getPageBase();
        if (page instanceof PageAssignmentHolderDetails) {
            return ((PageAssignmentHolderDetails)page).getWizardBreadcrumbs();
        }
        return List.of();
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
        removeLastBreadcrumb();
        super.onExitPerformed(target);
    }
}
