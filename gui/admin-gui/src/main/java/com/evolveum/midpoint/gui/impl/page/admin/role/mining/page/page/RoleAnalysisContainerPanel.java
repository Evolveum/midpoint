/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;

public class RoleAnalysisContainerPanel
        extends BasePanel<AssignmentHolderDetailsModel<?>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panel";

    public RoleAnalysisContainerPanel(String id, AssignmentHolderDetailsModel<?> model) {
        super(id, Model.of(model));
        initLayout();
    }

    public void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);
        FormSessionOptionPanel<?> components = new FormSessionOptionPanel<>(ID_PANEL, getModel()) {

            @Override
            protected VerticalFormPanel<?> getVerticalForm() {
                return (VerticalFormPanel<?>) super.getVerticalForm();
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected boolean checkMandatory(ItemWrapper itemWrapper) {
                return RoleAnalysisContainerPanel.this.checkMandatory(itemWrapper);
            }

            @Override
            protected ItemVisibilityHandler getVisibilityHandler() {
                return RoleAnalysisContainerPanel.this.getVisibilityHandler();
            }

            @Override
            protected IModel<? extends PrismContainerWrapper<?>> getContainerFormModel() {
                return RoleAnalysisContainerPanel.this.getContainerFormModel();
            }

            @Override
            protected IModel<String> getFormTitle() {
                return RoleAnalysisContainerPanel.this.getFormTitle();
            }
        };
        components.setOutputMarkupId(true);
        container.add(components);
    }

    public IModel<? extends PrismContainerWrapper<?>> getContainerFormModel() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        return true;
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected IModel<String> getFormTitle() {
        return Model.of();
    }
}
