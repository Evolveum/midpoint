package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.org.OrgTabbedPannel;
import com.evolveum.midpoint.web.component.org.OrgTreeTablePannel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class AssignableOrgPopupContent extends AssignablePopupContent{
	
	private final static String ID_TABS = "tabs";
	
	public AssignableOrgPopupContent(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	protected Panel createPopupContent(){
		Task task = getPageBase().createSimpleTask("load orgs");
		
		 final IModel<List<ITab>> tabModel = new LoadableModel<List<ITab>>(false) {
			 
				
	         @Override
	         protected List<ITab> load() {
//	             LOGGER.debug("Loading org. roots for tabs for tabbed panel.");
	            List<PrismObject<OrgType>> roots = loadOrgRoots();

	             List<ITab> tabs = new ArrayList<>();
	             for (PrismObject<OrgType> root : roots) {
	                 final String oid = root.getOid();
	                 tabs.add(new AbstractTab(createTabTitle(root)) {

	                     @Override
	                     public WebMarkupContainer getPanel(String panelId) {
	                         return new OrgTreeTablePannel(panelId, new Model(oid));
	                     }
	                 });
	             }

//	             LOGGER.debug("Tab count is {}", new Object[]{tabs.size()});

	             if (tabs.isEmpty()) {
	                 getSession().warn(getString("PageOrgTree.message.noOrgStructDefined"));
	                 throw new RestartResponseException(PageUsers.class);
	             }

	             return tabs;
	         }
	     };

	     TabbedPanel tabbedPanel = new TabbedPanel(ID_TABS, tabModel, new Model<>(0));
	     tabbedPanel.setOutputMarkupId(true);
	     
//	     add(tabbedPanel);
	     return tabbedPanel;
		
//     	panel = new OrgTabbedPannel(ID_TABLE, getSession(), roots);
//     	orgBrowser.setMarkupId(ID_TABLE);
//     	add(panel);
	}
	
	private List<PrismObject<OrgType>> loadOrgRoots() {
//      Task task = createSimpleTask("load orgs");
      OperationResult result = new OperationResult("loading orgs");

      Task task = getPageBase().createSimpleTask("loadOrgs");
      List<PrismObject<OrgType>> list = new ArrayList<>();
      try {
          ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(getPageBase().getPrismContext());
          list = getPageBase().getModelService().searchObjects(OrgType.class, query, null, task, result);

          if (list.isEmpty()) {
              warn(getString("PageOrgTree.message.noOrgStructDefined"));
          }
      } catch (Exception ex) {
//          LoggingUtils.logException(LOGGER, "Unable to load org. unit", ex);
          result.recordFatalError("Unable to load org unit", ex);
      } finally {
          result.computeStatus();
      }

      if (WebMiscUtil.showResultInPage(result)) {
//          showResult(result);
      }

      return list;
  }
	
	 private IModel<String> createTabTitle(final PrismObject<OrgType> org) {
	        return new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                PolyString displayName = org.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class);
	                if (displayName != null) {
	                    return displayName.getOrig();
	                }

	                return WebMiscUtil.getName(org);
	            }
	        };
	    }
	 
	 @Override
	protected Panel getTablePanel() {
		return (TabbedPanel) get(ID_TABS);
	}
	 
	 public List<ObjectType> getSelectedObjects(){
		 List<ObjectType> selected = new ArrayList<>();
		TabbedPanel orgPanel = (TabbedPanel) getTablePanel();
		 OrgTreeTablePannel orgPanels = (OrgTreeTablePannel) orgPanel.get("panel");
     	List<OrgTableDto> orgs = orgPanels.getSelectedOrgs();
     	for (OrgTableDto org : orgs){
     		selected.add(org.getObject());
     	}
     	return selected;
	 }
	 
	 public void setType(Class<? extends ObjectType> type){
		 Validate.notNull(type, "Class must not be null.");
		 
		         this.type = type;
		 
		         TabbedPanel table = (TabbedPanel) getTablePanel();
		         if (table != null) {
////		             ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
////		             provider.setType(type);
		 //
		             //replace table with table with proper columns
		             replace(createPopupContent());
		         }
	 }

}
