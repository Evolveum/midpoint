package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class OrgTreeAssignablePanel extends BasePanel{

	private static final Trace LOGGER = TraceManager.getTrace(OrgTreeAssignablePanel.class);
	
	 public static final String PARAM_ORG_RETURN = "org";

	    private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";
	    
	    private static final String ID_ORG_TABS = "orgTabs";
	    private static final String ID_ASSIGN = "assign";
	    private boolean selectable;
	    
	public OrgTreeAssignablePanel(String id, boolean selectable, PageBase parentPage) {
		super(id);
		this.selectable = selectable;
		setParent(parentPage);
		initLayout();
	}
	
	private void initLayout(){
		
		AbstractOrgTabPanel tabbedPanel = new AbstractOrgTabPanel(ID_ORG_TABS, getPageBase()) {
		
			
			@Override
			protected Panel createTreePanel(String id, Model<String> model, PageBase pageBase) {
				return  new OrgTreePanel(id, model, selectable) {
					
					@Override
					protected void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
						onItemSelect(selected, target);
					}
				};
				
			}
		};
		
		tabbedPanel.setOutputMarkupId(true);
		add(tabbedPanel);
		
		AjaxButton assignButton = new AjaxButton(ID_ASSIGN, getPageBase().createStringResource("Button.assign")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				AbstractOrgTabPanel orgPanel = (AbstractOrgTabPanel) getParent().get(ID_ORG_TABS);
				OrgTreePanel treePanel = (OrgTreePanel) orgPanel.getPanel();
				List<OrgType> selectedOrgs = treePanel.getSelectedOrgs();
				assignSelectedOrgPerformed(selectedOrgs, target);
				
			}
		};
		assignButton.setOutputMarkupId(true);
		assignButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return selectable;
			}
		});
		add(assignButton);
		
		
	}
	
	protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs, AjaxRequestTarget target) {
		
	}
	
	protected void onItemSelect(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
		
	}
	
	
}
