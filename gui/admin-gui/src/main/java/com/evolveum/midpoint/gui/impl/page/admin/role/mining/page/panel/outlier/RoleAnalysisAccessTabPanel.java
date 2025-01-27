/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.statistic.UserAccessDistribution;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class RoleAnalysisAccessTabPanel extends BasePanel<UserAccessDistribution> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    private static final String DOT_CLASS = RoleAnalysisAccessTabPanel.class.getName() + ".";
    private static final String OP_LOAD_ASSIGNMENT_TARGETS = DOT_CLASS + "loadAssignmentTargets";

    public RoleAnalysisAccessTabPanel(
            @NotNull String id,
            @NotNull IModel<UserAccessDistribution> userAccessDistributionIModel) {
        super(id, userAccessDistributionIModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        List<ITab> tabs = createTabs();
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_PANEL, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        assert target != null;
                        target.add(getPageBase().getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeAppender.append("class", "p-0 m-0"));
        container.add(tabPanel);
    }

    protected List<ITab> createTabs() {
        if (getAccessDistributionModel() == null || getAccessDistributionModel().getObject() == null) {
            return new ArrayList<>();
        }

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(getPageBase().createStringResource("Direct access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                UserAccessDistribution object = getAccessDistributionModel().getObject();
                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        extractOidList(object.getDirectAssignments()), RoleAnalysisProcessModeType.ROLE) {
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Indirect access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                UserAccessDistribution object = getAccessDistributionModel().getObject();

                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        extractOidList(object.getIndirectAssignments()), RoleAnalysisProcessModeType.ROLE) {
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Duplicated access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                UserAccessDistribution object = getAccessDistributionModel().getObject();
                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        extractOidList(object.getDuplicates()), RoleAnalysisProcessModeType.ROLE) {
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        return tabs;
    }

    private @NotNull List<String> extractOidList(@Nullable List<ObjectReferenceType> objectReferenceTypeList) {
        List<String> oidList = new ArrayList<>();

        if (objectReferenceTypeList == null) {
            return oidList;
        }

        for (ObjectReferenceType objectReferenceType : objectReferenceTypeList) {
            oidList.add(objectReferenceType.getOid());
        }
        return oidList;
    }

    public IModel<UserAccessDistribution> getAccessDistributionModel() {
        return getModel();
    }

}
