package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.RelationSelectorAssignablePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.AssignmentListDataProvider;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public abstract class AssignmentPanel extends BasePanel<List<AssignmentDto>>{

	
	private static final long serialVersionUID = 1L;
	
	public static final String ID_ASSIGNMENTS = "assignments";
	private static final String ID_NEW_ASSIGNMENT_BUTTON = "newAssignmentButton";
	private static final String ID_ASSIGNMENTS_TABLE = "assignmentsTable";
	public static final String ID_ASSIGNMENTS_DETAILS = "assignmentsDetails";
	public static final String ID_ASSIGNMENT_DETAILS = "assignmentDetails";
	
	public static final String ID_DETAILS = "details";
	
	private final static String ID_DONE_BUTTON = "doneButton";
	
	protected boolean assignmentDetailsVisible;
	
	private PageBase pageBase;
	
	public AssignmentPanel(String id, IModel<List<AssignmentDto>> assignmentsModel, PageBase pageBase){
    	super(id, assignmentsModel);
    	this.pageBase = pageBase;
    	initPaging();
        initLayout();
        
    }
	
	protected abstract void initPaging();
	protected void initCustomLayout(WebMarkupContainer assignmentsContainer) {
		
	}
	
	private void initLayout() {
		 WebMarkupContainer assignmentsContainer = new WebMarkupContainer(ID_ASSIGNMENTS);
	        assignmentsContainer.setOutputMarkupId(true);
	        add(assignmentsContainer);

	        BoxedTablePanel<AssignmentDto> assignmentTable = initAssignmentTable();
	        assignmentsContainer.add(assignmentTable);

	        AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_ASSIGNMENT_BUTTON, new Model<>("fa fa-plus"),
	                createStringResource("MainObjectListPanel.newObject")) {

	            private static final long serialVersionUID = 1L;

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                newAssignmentClickPerformed(target);
	            }
	        };
	        newObjectIcon.add(new VisibleEnableBehaviour(){
	            private static final long serialVersionUID = 1L;

	            @Override
	            public boolean isVisible(){
	                return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
	            }
	        });
	        assignmentsContainer.add(newObjectIcon);
	        
	        initCustomLayout(assignmentsContainer);
	        setOutputMarkupId(true);
	        
	        assignmentsContainer.add(new VisibleEnableBehaviour() {
	        	
	        	private static final long serialVersionUID = 1L;
	        	
	        	@Override
	        	public boolean isVisible() {
	        		return !assignmentDetailsVisible;
	        	}
	        });
	        
	        WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
	        details.setOutputMarkupId(true);
	        details.add(new VisibleEnableBehaviour() {
	       
	        	private static final long serialVersionUID = 1L;

				@Override
	        	public boolean isVisible() {
	        		return assignmentDetailsVisible;
	        	}
	        });
	        
	        add(details);
	        
	        
	        IModel<List<AssignmentDto>> selectedAssignmnetList = new AbstractReadOnlyModel<List<AssignmentDto>>() {
			
	        	private static final long serialVersionUID = 1L;
	        	
	        	@Override
	        	public List<AssignmentDto> getObject() {
	        		return ((AssignmentListDataProvider) assignmentTable.getDataTable().getDataProvider()).getSelectedData();
	        	}
	        };
	        
	        ListView<AssignmentDto> assignmentDetailsView = new ListView<AssignmentDto>(ID_ASSIGNMENTS_DETAILS, selectedAssignmnetList) {

	        	private static final long serialVersionUID = 1L;
	        	
					@Override
					protected void populateItem(ListItem<AssignmentDto> item) {
						AbstractAssignmentDetailsPanel details = createDetailsPanel(ID_ASSIGNMENT_DETAILS, item.getModel(), getParentPage());
						item.add(details);
				        details.setOutputMarkupId(true);
				        
						
					}

					
			};
			
			assignmentDetailsView.setOutputMarkupId(true);
			details.add(assignmentDetailsView);
			
			AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON, createStringResource("AbstractAssignmentDetailsPanel.doneButton")) {
	            private static final long serialVersionUID = 1L;
	            @Override
	            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
	                assignmentDetailsVisible = false;
	                getSelectedAssignments().stream().forEach(a -> a.setSelected(false));
	                ajaxRequestTarget.add(AssignmentPanel.this);
	            }
	        };
	        details.add(doneButton);
	        
//	        AbstractRoleAssignmentDetailsPanel details = new AbstractRoleAssignmentDetailsPanel(ID_ASSIGNMENTS_DETAILS, rowModel, getParentPage());
//	        add(details);
//	        details.add(new VisibleEnableBehaviour() {
//	        	
//	        	@Override
//	        	public boolean isVisible() {
//	        		return assignmentDetailsVisible;
//	        	}
//	        });
	}
	
	  protected AssignmentsTabStorage getAssignmentsStorage(){
	        return pageBase.getSessionStorage().getAssignmentsTabStorage();
	    }

	  
	  protected abstract AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails,
				IModel<AssignmentDto> model, PageBase parentPage);
	  private BoxedTablePanel<AssignmentDto> initAssignmentTable(){
	        
//  	   Map<RelationTypes, List<AssignmentDto>> relationAssignmentsMap = fillInRelationAssignmentsMap();
      AssignmentListDataProvider assignmentsProvider = new AssignmentListDataProvider(this, Model.ofList(getModelList())){
          private static final long serialVersionUID = 1L;

          @Override
          protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
              getAssignmentsStorage().setPaging(paging);
          }
          
          @Override
        public ObjectQuery getQuery() {
        	return createObjectQuery();
        }
       
      };
      
      List<IColumn<AssignmentDto, String>> columns =  initColumns();
      if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
          columns.add(new InlineMenuButtonColumn<AssignmentDto>(getAssignmentMenuActions(), 1, getParentPage()));
      }
      
      BoxedTablePanel<AssignmentDto> assignmentTable = new BoxedTablePanel<AssignmentDto>(ID_ASSIGNMENTS_TABLE,
              assignmentsProvider, columns, getTableId(),
              getItemsPerPage()){
          private static final long serialVersionUID = 1L;

         
          @Override
          public int getItemsPerPage() {
              return pageBase.getSessionStorage().getUserProfile().getTables().get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
          }
          
          @Override
          protected Item<AssignmentDto> customizeNewRowItem(Item<AssignmentDto> item, IModel<AssignmentDto> model) {
                  item.add(AttributeModifier.append("class", AssignmentsUtil.createAssignmentStatusClassModel(model)));
              return item;
          }

      };
      assignmentTable.setOutputMarkupId(true);
      assignmentTable.setCurrentPage(getAssignmentsStorage().getPaging());
      return assignmentTable;

  }
	 
	 private List<InlineMenuItem> getAssignmentMenuActions(){
	        List<InlineMenuItem> menuItems = new ArrayList<>();
	        menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
	                new Model<Boolean>(true), new Model<Boolean>(true), false,
	                new ColumnMenuAction<Selectable<AssignmentDto>>() {
	                    private static final long serialVersionUID = 1L;

	                    @Override
	                    public void onClick(AjaxRequestTarget target) {
	                        if (getRowModel() == null){
	                            deleteAssignmentPerformed(target, getSelectedAssignments());
	                        } else {
	                            AssignmentDto rowDto = (AssignmentDto)getRowModel().getObject();
	                            List<AssignmentDto> toDelete = new ArrayList<>();
	                            toDelete.add(rowDto);
	                            deleteAssignmentPerformed(target, toDelete);
	                        }
	                    }
	                }, 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM, DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));
	        return menuItems;
	    }
	 
	 private List<AssignmentDto> getSelectedAssignments() {
		 BoxedTablePanel<AssignmentDto> assignemntTable = getAssignmentTable();
		 AssignmentListDataProvider assignmentProvider = (AssignmentListDataProvider) assignemntTable.getDataTable().getDataProvider();
		 return assignmentProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
	 }
	 
	 protected void assignmentDetailsPerformed(AjaxRequestTarget target, IModel<AssignmentDto> rowModel) {
	    	assignmentDetailsVisible = true;
	    	rowModel.getObject().setSelected(true);
	    	target.add(AssignmentPanel.this);
	    }
	 
	 protected abstract TableId getTableId();
	 protected abstract int getItemsPerPage();
	 
	 protected void refreshTable(AjaxRequestTarget target) {
		 target.add(getAssignmentContainer().addOrReplace(initAssignmentTable()));		 
	 }
	 
	 protected abstract List<AssignmentDto> getModelList();
	 
	 protected abstract List<IColumn<AssignmentDto, String>> initColumns();
	 
	 protected abstract void newAssignmentClickPerformed(AjaxRequestTarget target);
	 
	 protected abstract ObjectQuery createObjectQuery();
	 
	 protected void deleteAssignmentPerformed(AjaxRequestTarget target, List<AssignmentDto> toDelete) {
		 toDelete.forEach(a -> a.setStatus(UserDtoStatus.DELETE));
		 refreshTable(target);
	 }
	 
	 protected WebMarkupContainer getAssignmentContainer() {
		 return (WebMarkupContainer) get(ID_ASSIGNMENTS);
	 }
	 
	 protected BoxedTablePanel<AssignmentDto> getAssignmentTable() {
		 return (BoxedTablePanel<AssignmentDto>) get(createComponentPath(ID_ASSIGNMENTS, ID_ASSIGNMENTS_TABLE));
	 }
	 
	 public PageBase getParentPage() {
		return pageBase;
	}
}
