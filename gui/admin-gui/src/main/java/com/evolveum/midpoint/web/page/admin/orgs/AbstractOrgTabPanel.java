package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public abstract class AbstractOrgTabPanel extends BasePanel{
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractOrgTabPanel.class);
	
	public static final String PARAM_ORG_RETURN = "org";

    private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";
    
    private String ID_TABS = "tabs";
	
	public AbstractOrgTabPanel(String id, PageBase pageBase) {
		super(id);
		setParent(pageBase);
		initLayout();
	}
	
	private void initLayout() {
        final IModel<List<ITab>> tabModel = new LoadableModel<List<ITab>>(false) {

            @Override
            protected List<ITab> load() {
                LOGGER.debug("Loading org. roots for tabs for tabbed panel.");
                List<PrismObject<OrgType>> roots = loadOrgRoots();

                final List<ITab> tabs = new ArrayList<>();
                for (PrismObject<OrgType> root : roots) {
                    final String oid = root.getOid();
                    tabs.add(new AbstractTab(createTabTitle(root)) {
                        private int tabId = tabs.size();

                        @Override
                        public WebMarkupContainer getPanel(String panelId) {
                            add(new AjaxEventBehavior("onload") {
                                    protected void onEvent(final AjaxRequestTarget target) {
                                        SessionStorage storage = getPageBase().getSessionStorage();
                                        storage.getUsers().setSelectedTabId(tabId);
                                    }
                                }
                            );
//                            return new OrgChildrenPanel(panelId, new Model(oid), PageOrgTree.this);
                            return createTreePanel(panelId, new Model(oid), getPageBase());
//                            return new TreeTablePanel(panelId, new Model(oid), AbstractOrgTabPanel.this);
                        }
                        
                        
                    });
                }

                LOGGER.debug("Tab count is {}", new Object[]{tabs.size()});

                return tabs;
            }
        };

        SessionStorage storage = getPageBase().getSessionStorage();
        int selectedTab = storage.getUsers().getSelectedTabId() == -1 ? 0 : storage.getUsers().getSelectedTabId();
        List<ITab> tabsList = tabModel.getObject();
        if (tabsList == null || (selectedTab > tabsList.size() - 1)){
            storage.getUsers().setSelectedTabId(0);
            selectedTab = 0;
        }
        TabbedPanel tabbedPanel = new TabbedPanel(ID_TABS, tabModel, new Model<>(selectedTab), null);
        tabbedPanel.setOutputMarkupId(true);
        
        if (tabsList == null || tabsList.size() == 0){
            tabbedPanel.setVisible(false);
        }
        add(tabbedPanel);
    }
	
	protected Panel getPanel(){
		return (Panel) get(ID_TABS).get("panel");
	}
	
	protected abstract Panel createTreePanel(String id, Model<String> model, PageBase pageBase);

    private IModel<String> createTabTitle(final PrismObject<OrgType> org) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PolyString displayName = org.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class);
                if (displayName != null) {
                    return displayName.getOrig();
                }

                return WebComponentUtil.getName(org);
            }
        };
    }

    private List<PrismObject<OrgType>> loadOrgRoots() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ORG_UNIT);
        OperationResult result = new OperationResult(OPERATION_LOAD_ORG_UNIT);

        List<PrismObject<OrgType>> list = new ArrayList<>();
        try {
            ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(getPageBase().getPrismContext());
            list = getPageBase().getModelService().searchObjects(OrgType.class, query, null, task, result);

            if (list.isEmpty()) {
                warn(getString("PageOrgTree.message.noOrgStructDefined"));
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unable to load org. unit", ex);
            result.recordFatalError("Unable to load org unit", ex);
        } finally {
            result.computeStatus();
        }

        if (WebComponentUtil.showResultInPage(result)) {
        	getPageBase().showResult(result);
        }

        return list;
    }

}
