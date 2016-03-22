package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDto;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class FocusBrowserDialogPanel<T extends FocusType> extends BasePanel<T>{

	 private static final String ID_SEARCH_FORM = "searchForm";
	    private static final String ID_MAIN_FORM = "mainForm";
	    private static final String ID_CHECK_NAME = "nameCheck";
	    private static final String ID_CHECK_FULL_NAME = "fullNameCheck";
	    private static final String ID_CHECK_GIVEN_NAME = "givenNameCheck";
	    private static final String ID_CHECK_FAMILY_NAME = "familyNameCheck";
	    private static final String ID_BASIC_SEARCH = "basicSearch";
	    private static final String ID_BUTTON_CANCEL = "cancelButton";
	    private static final String ID_BUTTON_ADD = "addButton";
	    private static final String ID_TABLE = "table";

	    private static final Trace LOGGER = TraceManager.getTrace(UserBrowserDialog.class);
//	    private IModel<T> model;
	    private boolean initialized;
	    private Class type;
	    private PageBase parentPage;
	    
	
	public FocusBrowserDialogPanel(String id, Class type, PageBase parentPage) {
		super(id);
		this.type = type;
		this.parentPage = parentPage;
//		this.model = model;
		initLayout();
	}
	
	

	    public void setType(Class type) {
			this.type = type;
			
			TablePanel table = (TablePanel) getTable();

	        if (table != null) {
		        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
		        provider.setType(type);

		        Form mainForm = (Form) get(ID_MAIN_FORM);
		        //replace table with table with proper columns
		        mainForm.replace(createTable());
		    }
		}
	
	private void initLayout(){
	  Form mainForm = new Form(ID_MAIN_FORM);
      add(mainForm);

//      Form searchForm = new Form(ID_SEARCH_FORM);
//      searchForm.setOutputMarkupId(true);
//      add(searchForm);

//      TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
//      mainForm.add(search);

//      CheckBox nameCheck = new CheckBox(ID_CHECK_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_NAME));
//      searchForm.add(nameCheck);
//      CheckBox fullNameCheck = new CheckBox(ID_CHECK_FULL_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_FULL_NAME));
//      searchForm.add(fullNameCheck);
//      fullNameCheck.setVisible(UserType.class.equals(type));
//      CheckBox givenNameCheck = new CheckBox(ID_CHECK_GIVEN_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_GIVEN_NAME));
//      searchForm.add(givenNameCheck);
//      givenNameCheck.setVisible(UserType.class.equals(type));
//      CheckBox familyNameCheck = new CheckBox(ID_CHECK_FAMILY_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_FAMILY_NAME));
//      searchForm.add(familyNameCheck);
//      familyNameCheck.setVisible(UserType.class.equals(type));
      
//      BasicSearchPanel<UserBrowserDto> basicSearch = new BasicSearchPanel<UserBrowserDto>(ID_BASIC_SEARCH) {
//
//          @Override
//          protected IModel<String> createSearchTextModel() {
//              return new PropertyModel<>(model, UserBrowserDto.F_SEARCH_TEXT);
//          }
//
//          @Override
//          protected void searchPerformed(AjaxRequestTarget target) {
//              FocusBrowserDialogPanel.this.searchPerformed(target);
//          }
//
//          @Override
//          protected void clearSearchPerformed(AjaxRequestTarget target) {
//        	  FocusBrowserDialogPanel.this.clearSearchPerformed(target);
//          }
//      };
//      searchForm.add(basicSearch);

      TablePanel table = createTable();
      mainForm.add(table);

      AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
              createStringResource("userBrowserDialog.button.cancelButton")) {

          @Override
          public void onClick(AjaxRequestTarget target) {
              cancelPerformed(target);
          }
      };
      mainForm.add(cancelButton);
      
          AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
	                createStringResource("userBrowserDialog.button.addButton")) {
	
	            @Override
	            public void onClick(AjaxRequestTarget target) {
	            	DataTable table = getTable().getDataTable();
	            	List<T> selected = ((ObjectDataProvider)table.getDataProvider()).getSelectedData();
//	            	List<T> selected = new ArrayList<>();
//	            	for (SelectableBean o : availableData){
//	            		if (o.isSelected()){
//	            			selected.add((T)o.getValue());
//	            		}
//	            	}
	                addPerformed(target, selected);
	            }
	        };
      addButton.add(new VisibleEnableBehaviour(){
      	@Override
      	public boolean isVisible() {
      		// TODO Auto-generated method stub
      		return isCheckBoxVisible();
      	}
      });
      mainForm.add(addButton);
  }
	
	  protected boolean isCheckBoxVisible(){
	    	return false;
	    }


  private TablePanel createTable(){
  	List<IColumn<SelectableBean<T>, String>> columns = initColumns();
      TablePanel table = new TablePanel<>(ID_TABLE,
              new ObjectDataProvider(parentPage, type){
      	
      	@Override
      	public ObjectQuery getQuery() {
      		ObjectQuery customQuery = createContentQuery();
      		if (customQuery == null){
      			return super.getQuery();
      		}
      		return customQuery;
      	}
      	
      }, columns);
      table.setOutputMarkupId(true);
      return table;
  }
  
  protected ObjectQuery createContentQuery(){
  	return null;
  }
  
//  private PageBase getPageBase() {
//      return (PageBase) getPage();
//  }

  public StringResourceModel createStringResource(String resourceKey, Object... objects) {
  	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//      return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
  }

  private List<IColumn<SelectableBean<T>, String>> initColumns() {
      List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

      if (isCheckBoxVisible()){
      	columns.add(new CheckBoxHeaderColumn());
      	IColumn column = new PropertyColumn(createStringResource("userBrowserDialog.name"), UserType.F_NAME.getLocalPart(), "value.name");
          columns.add(column);
      } 
      
      if (UserType.class.equals(type)){
      	initUserColumns(columns);
      } else if (RoleType.class.equals(type) || OrgType.class.equals(type)){
      	initAbstractRoleColumns(columns);
      }
      
      return columns;
  }
  
  private void initUserColumns(List<IColumn<SelectableBean<T>, String>> columns){
  	columns.add(new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

          @Override
          protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
              return new AbstractReadOnlyModel<String>() {

                  @Override
                  public String getObject() {
                      T user = rowModel.getObject().getValue();
                      return WebComponentUtil.createUserIcon(user.asPrismContainer());
                  }
              };
          }
      });

      if (!isCheckBoxVisible()){
      IColumn column = new LinkColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.name"), UserType.F_NAME.getLocalPart(), "value.name") {

          @Override
          public void onClick(AjaxRequestTarget target, IModel<SelectableBean<T>> rowModel) {
              T user = rowModel.getObject().getValue();
              userDetailsPerformed(target, user);
          }
      };
      columns.add(column);
      }
      

      IColumn column = new PropertyColumn(createStringResource("userBrowserDialog.givenName"), UserType.F_GIVEN_NAME.getLocalPart(), SelectableBean.F_VALUE + ".givenName");
      columns.add(column);

      column = new PropertyColumn(createStringResource("userBrowserDialog.familyName"), UserType.F_FAMILY_NAME.getLocalPart(), SelectableBean.F_VALUE + ".familyName");
      columns.add(column);

      column = new PropertyColumn(createStringResource("userBrowserDialog.fullName"), UserType.F_FULL_NAME.getLocalPart(), SelectableBean.F_VALUE + ".fullName.orig");
      columns.add(column);

//      column = new AbstractColumn<SelectableBean<T>, String>(createStringResource("userBrowserDialog.email")) {
//
//          @Override
//          public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem, String componentId,
//                                   IModel<SelectableBean<T>> rowModel) {
//
//              String email = rowModel.getObject().getValue().getEmailAddress();
//              cellItem.add(new Label(componentId, new Model<String>(email)));
//          }
//      };
//      columns.add(column);

  }
  
  private void initAbstractRoleColumns(List<IColumn<SelectableBean<T>, String>> columns){
  	        if (!isCheckBoxVisible()){
      IColumn column = new LinkColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.name"), FocusType.F_NAME.getLocalPart(), "value.name") {

          @Override
          public void onClick(AjaxRequestTarget target, IModel<SelectableBean<T>> rowModel) {
              T user = rowModel.getObject().getValue();
              userDetailsPerformed(target, user);
          }
      };
      columns.add(column);
      }
      

      
  	        IColumn column = new PropertyColumn(createStringResource("userBrowserDialog.displayName"), null, SelectableBean.F_VALUE + ".displayName");
  	        columns.add(column);
  	        column = new PropertyColumn(createStringResource("userBrowserDialog.description"), null, SelectableBean.F_VALUE + ".description");
      columns.add(column);

      

      column = new PropertyColumn(createStringResource("userBrowserDialog.identifier"), null, SelectableBean.F_VALUE + ".identifier");
      columns.add(column);


  }

  private void searchPerformed(AjaxRequestTarget target) {
      ObjectQuery query = createQuery();
      target.add(parentPage.getFeedbackPanel());

      TablePanel panel = getTable();
      DataTable table = panel.getDataTable();
      ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
      provider.setQuery(query);

      table.setCurrentPage(0);

      target.add(panel);
  }

  private TablePanel getTable() {
      return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
  }

  private ObjectQuery createQuery() {
//      T dto = model.getObject();
      ObjectQuery query = null;
//      if (StringUtils.isEmpty(dto.getSearchText())) {
//          return null;
//      }
//
//      try {
//          List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
//
//          PrismContext prismContext = getPageBase().getPrismContext();
//          PolyStringNormalizer normalizer = prismContext.getDefaultPolyStringNormalizer();
//          if (normalizer == null) {
//              normalizer = new PrismDefaultPolyStringNormalizer();
//          }
//
//          String normalizedString = normalizer.normalize(dto.getSearchText());
//
//			if (dto.isName()) {
//				filters.add(SubstringFilter.createSubstring(T.F_NAME, type, prismContext, 
//						normalizedString));
//			}
//
//			if (dto.isFamilyName()) {
//				filters.add(SubstringFilter.createSubstring(UserType.F_FAMILY_NAME, UserType.class, prismContext,
//						normalizedString));
//			}
//			if (dto.isFullName()) {
//				filters.add(SubstringFilter.createSubstring(UserType.F_FULL_NAME, UserType.class, prismContext,
//						normalizedString));
//			}
//			if (dto.isGivenName()) {
//				filters.add(SubstringFilter.createSubstring(UserType.F_GIVEN_NAME, UserType.class, prismContext,
//						normalizedString));
//			}
//
//          if (!filters.isEmpty()) {
//              query = new ObjectQuery().createObjectQuery(OrFilter.createOr(filters));
//          }
//      } catch (Exception ex) {
//          error(getString("userBrowserDialog.message.queryError") + " " + ex.getMessage());
//          LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
//      }

      return query;
  }

  private void clearSearchPerformed(AjaxRequestTarget target){
//      model.setObject(new UserBrowserDto());
//
      TablePanel panel = getTable();
      DataTable table = panel.getDataTable();
      ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
      provider.setQuery(null);

      target.add(get(ID_SEARCH_FORM));
      target.add(panel);
  }

  private void cancelPerformed(AjaxRequestTarget target) {
      parentPage.hideMainPopup(target);
  }
  
  public void addPerformed(AjaxRequestTarget target, List<T> selected) {
	  parentPage.hideMainPopup(target);
  }

  public void userDetailsPerformed(AjaxRequestTarget target, T user) {
	  parentPage.hideMainPopup(target);
  }
	
	

}
