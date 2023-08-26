/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.session.OrgStructurePanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public abstract class AbstractOrgTabPanel extends BasePanel<OrgType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractOrgTabPanel.class);

    private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";

    private static final String ID_TABS = "tabs";

    public AbstractOrgTabPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final IModel<List<ITab>> tabModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> load() {
                LOGGER.debug("Loading org. roots for tabs for tabbed panel.");
                List<PrismObject<OrgType>> roots = loadOrgRoots();

                final List<ITab> tabs = new ArrayList<>();
                for (PrismObject<OrgType> root : roots) {
                    final String oid = root.getOid();
                    tabs.add(new AbstractTab(createTabTitle(root)) {
                        private static final long serialVersionUID = 1L;
                        private final int tabId = tabs.size();

                        @Override
                        public WebMarkupContainer getPanel(String panelId) {
                            add(new AjaxEventBehavior("load") {
                                    private static final long serialVersionUID = 1L;

                                    protected void onEvent(final AjaxRequestTarget target) {
                                        OrgStructurePanelStorage usersStorage = getOrgStructurePanelStorage();
                                        if (usersStorage != null) {
                                            usersStorage.setSelectedTabId(tabId);
                                        }
                                    }
                                }
                            );
                            Panel panel = createTreePanel(panelId, new Model<>(oid), getPageBase());
                            panel.setOutputMarkupId(true);
                            return panel;
                        }

                    });
                }

                LOGGER.debug("Tab count is {}", tabs.size());

                return tabs;
            }
        };

        List<ITab> tabsList = tabModel.getObject();
        OrgStructurePanelStorage orgStructurePanelStorage = getOrgStructurePanelStorage();
        int selectedTab = 0;
        if (orgStructurePanelStorage != null) {
            selectedTab = orgStructurePanelStorage.getSelectedTabId() == -1 ? 0 : orgStructurePanelStorage.getSelectedTabId();
            if (tabsList == null || (selectedTab > tabsList.size() - 1)) {
                orgStructurePanelStorage.setSelectedTabId(0);
            }
        }
        AjaxTabbedPanel<ITab> tabbedPanel = new AjaxTabbedPanel<>(ID_TABS, tabModel.getObject(), new Model<>(selectedTab), null) {

            private static final long serialVersionUID = 1L;

            @Override
            public TabbedPanel<ITab> setSelectedTab(int index) {
                changeTabPerformed(index);
                return super.setSelectedTab(index);
            }
        };
        tabbedPanel.setOutputMarkupId(true);

        if (tabsList == null || tabsList.size() == 0) {
            tabbedPanel.setVisible(false);
        }
        add(tabbedPanel);
    }

    protected Panel getPanel() {
        if (get(ID_TABS).get("panel") instanceof Panel) {
            return (Panel) get(ID_TABS).get("panel");
        }
        return null;
    }

    public AjaxTabbedPanel<ITab> getTabbedPanel() {
        //noinspection unchecked
        return (AjaxTabbedPanel<ITab>) get(ID_TABS);
    }

    protected abstract Panel createTreePanel(String id, Model<String> model, PageBase pageBase);

    private IModel<String> createTabTitle(final PrismObject<OrgType> org) {
        return Model.of(WebComponentUtil.getDisplayNameOrName(org));
    }

    private List<PrismObject<OrgType>> loadOrgRoots() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ORG_UNIT);
        OperationResult result = new OperationResult(OPERATION_LOAD_ORG_UNIT);

        List<PrismObject<OrgType>> list = new ArrayList<>();
        try {
            ObjectQuery query = getPageBase().getPrismContext().queryFor(OrgType.class)
                    .isRoot()
                    .asc(OrgType.F_NAME)
                    .build();
            ObjectFilter assignableItemsFilter = getAssignableItemsFilter();
            if (assignableItemsFilter != null) {
                query.addFilter(assignableItemsFilter);
            }
            list = getPageBase().getModelService().searchObjects(OrgType.class, query, null, task, result);
            // Sort org roots by displayOrder, if not set push the org to the end
            list.sort((o1, o2) -> {
                Comparator<PrismObject<OrgType>> intComparator =
                        Comparator.comparingInt(o -> ((ObjectUtils.defaultIfNull(o.getRealValue().getDisplayOrder(), Integer.MAX_VALUE))));
                int compare = intComparator.compare(o1, o2);
                if (compare == 0){
                    String display1 = WebComponentUtil.getDisplayName(o1);
                    if (StringUtils.isBlank(display1)) {
                        display1 = LocalizationUtil.translatePolyString(o1.getName());
                    }
                    String display2 = WebComponentUtil.getDisplayName(o2);
                    if (StringUtils.isBlank(display2)) {
                        display2 = LocalizationUtil.translatePolyString(o2.getName());
                    }

                    if(StringUtils.isEmpty(display1) && StringUtils.isEmpty(display2)) {
                        return compare;
                    }
                    if(StringUtils.isEmpty(display1)) {
                        return 1;
                    }
                    if(StringUtils.isEmpty(display2)) {
                        return -1;
                    }

                    return String.CASE_INSENSITIVE_ORDER.compare(display1, display2);
                }
                return compare;
            });

            if (list.isEmpty() && isWarnMessageVisible()) {
                warn(getString("PageOrgTree.message.noOrgStructDefined"));
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unable to load org. unit", ex);
            result.recordFatalError(getString("AbstractOrgTabPanel.message.loadOrgRoots.fatalError"), ex);
        } finally {
            result.computeStatus();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result);
        }
        return list;
    }

    protected ObjectFilter getAssignableItemsFilter() {
        return null;
    }

    protected boolean isWarnMessageVisible() {
        return true;
    }

    protected void changeTabPerformed(int index) {
//        if (roots != null && index >= 0 && index <= roots.size()) {
            OrgStructurePanelStorage orgStructureStorage = getOrgStructurePanelStorage();
            if (orgStructureStorage != null) {
                orgStructureStorage.setSelectedTabId(index);
            }
//        }
    }

    protected OrgStructurePanelStorage getOrgStructurePanelStorage() {
        return getPageBase().getSessionStorage().getOrgStructurePanelStorage();
    }
}
