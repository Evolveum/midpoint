/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.or0;

public class CertificationItemsTabbedPanel extends BasePanel<PrismObjectWrapper<AccessCertificationCampaignType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABBED_PANEL = "tabbedPanel";

    LoadableDetachableModel<List<ITab>> tabsModel;

    public CertificationItemsTabbedPanel(String id, IModel<PrismObjectWrapper<AccessCertificationCampaignType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initTabsModel();
        initLayout();
    }

    private void initTabsModel() {
        tabsModel = createTabsModel();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        TabbedPanel<ITab> tabbedPanel = WebComponentUtil.createTabPanel(ID_TABBED_PANEL, getPageBase(), tabsModel.getObject(), null);
        tabbedPanel.add(new VisibleBehaviour(() -> or0(getCampaign().getStageNumber()) > 0));
        selectCurrentStageTabPanel(tabbedPanel);
        mainForm.add(tabbedPanel);
    }

    private LoadableDetachableModel<List<ITab>> createTabsModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> load() {
                List<ITab> tabs = new ArrayList<>();
                int iteration = getCampaign().getIteration();
                int currentStage = getCampaign().getStage()
                        .stream().filter(stage -> stage.getIteration() == iteration)
                        .toList().size();
                for (int i = 1; i <= currentStage; i++) {
                    tabs.add(createCountablePanelTab(i));
                }
                return tabs;
            }
        };
    }

    private PanelTab createCountablePanelTab(int stageNumber) {
        return new PanelTab(createStringResource("CertificationItemsPanel.tabPanel.title", stageNumber)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                CertificationCasesPanel items = new CertificationCasesPanel(panelId, getModelObject().getOid(), stageNumber);
                items.setOutputMarkupId(true);
                return items;
            }
        };
    }

    private AccessCertificationCampaignType getCampaign() {
        return getModelObject().getObject().asObjectable();
    }

    private void selectCurrentStageTabPanel(TabbedPanel<ITab> tabbedPanel) {
        int currentStage = getCampaign().getStageNumber();
        if (currentStage > 0 && currentStage <= tabbedPanel.getTabs().getObject().size()) {
            tabbedPanel.setSelectedTab(currentStage - 1);
        }
    }
}
