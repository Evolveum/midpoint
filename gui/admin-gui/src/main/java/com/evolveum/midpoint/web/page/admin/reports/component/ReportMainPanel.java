/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.PageReport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class ReportMainPanel extends AbstractObjectMainPanel<ReportType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_SAVE_AND_RUN = "saveAndRun";

    public ReportMainPanel(String id, LoadableModel<PrismObjectWrapper<ReportType>> objectModel, PageAdminObjectDetails<ReportType> parentPage) {
        super(id, objectModel, parentPage);
    }

    @Override
    protected List<ITab> createTabs(PageAdminObjectDetails<ReportType> parentPage) {
        return getTabs(parentPage);
    }

    @Override
    protected void initLayoutButtons(PageAdminObjectDetails<ReportType> parentPage) {
        super.initLayoutButtons(parentPage);
        initLayoutSaveAndRunButton(parentPage);
    }

    private void initLayoutSaveAndRunButton(PageAdminObjectDetails<ReportType> parentPage) {
        AjaxSubmitButton saveAndRunButton = new AjaxSubmitButton(ID_SAVE_AND_RUN, parentPage.createStringResource("pageReport.button.saveAndRun")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                ((PageReport)getDetailsPage()).saveAndRunPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(parentPage.getFeedbackPanel());
            }
        };
        saveAndRunButton.add(getVisibilityForSaveAndRunButton());
        saveAndRunButton.setOutputMarkupId(true);
        saveAndRunButton.setOutputMarkupPlaceholderTag(true);
        getMainForm().add(saveAndRunButton);
    }

    private VisibleEnableBehaviour getVisibilityForSaveAndRunButton() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getObjectWrapper().isReadOnly() &&
                        !getDetailsPage().isForcedPreview();
            }

            @Override
            public boolean isEnabled() {
                return !getExecuteChangeOptionsDto().isSaveInBackground();
            }
        };
    }

    @Override
    public void reloadSavePreviewButtons(AjaxRequestTarget target) {
        super.reloadSavePreviewButtons(target);
        target.add(ReportMainPanel.this.get(ID_MAIN_FORM).get(ID_SAVE_AND_RUN));

    }

    private List<ITab> getTabs(PageAdminObjectDetails<ReportType> parentPage){
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.basic.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ObjectBasicPanel<ReportType>(panelId, getObjectModel()){
                    @Override
                    protected QName getType() {
                        return ReportType.COMPLEX_TYPE;
                    }

                    @Override
                    protected ItemVisibility getBasicTabVisibility(ItemWrapper<?, ?> itemWrapper) {
                        if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_LIFECYCLE_STATE))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DIAGNOSTIC_INFORMATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DEFAULT_SCRIPT_CONFIGURATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_POST_REPORT_SCRIPT))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });
        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.engine.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new EngineReportTabPanel(panelId, getObjectModel());
            }
        });

        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.export.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                PrismContainerWrapperModel<ReportType, Containerable> model = PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ReportType.F_FILE_FORMAT);
                return new SingleContainerPanel(panelId, model, FileFormatConfigurationType.COMPLEX_TYPE){
                    @Override
                    protected ItemVisibility getVisibility(ItemPath itemPath) {
                        if(itemPath.isSubPathOrEquivalent(ItemPath.create(ReportType.F_FILE_FORMAT, FileFormatConfigurationType.F_HTML))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });

        return tabs;
    }
}
