/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;

import org.jetbrains.annotations.Nullable;

public class PartitionPopup extends BasePanel<RoleAnalysisOutlierPartitionType> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    public PartitionPopup(@NotNull String id, @Nullable IModel<RoleAnalysisOutlierPartitionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        addOrReplaceTabPanels(form);

        MessagePanel<?> warningMessage = new MessagePanel<>(
                ID_WARNING_MESSAGE,
                MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

    }

    private void addOrReplaceTabPanels(@NotNull Form<?> form) {
        List<ITab> tabs = createPartitionTabs();
        TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TABS_PANEL, getPageBase(), tabs, null);
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        form.addOrReplace(tabPanel);
    }

    protected List<ITab> createPartitionTabs() {
        List<ITab> tabs = prepareCustomTabPanels();
        if (!tabs.isEmpty() && initDefaultTabPanels()) {
            return tabs;
        }

        tabs.add(new PanelTab(getPageBase().createStringResource("test1"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new WebMarkupContainer(panelId);
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("test2"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new WebMarkupContainer(panelId);
            }
        });

        return tabs;
    }

    protected @NotNull List<ITab> prepareCustomTabPanels() {
        return new ArrayList<>();
    }

    protected boolean initDefaultTabPanels() {
        return true;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target) {
        getTabbedPanel().reloadCountLabels(target);
    }

    @SuppressWarnings("rawtypes")
    private TabbedPanel<?> getTabbedPanel() {
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    public StringResourceModel getTitle() {
        return createStringResource("TypedAssignablePanel.selectObjects");
    }

    public Component getContent() {
        return this;
    }
}
