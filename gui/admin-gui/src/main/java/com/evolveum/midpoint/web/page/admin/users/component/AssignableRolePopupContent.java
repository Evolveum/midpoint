package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class AssignableRolePopupContent extends AssignablePopupContent{

    private static final Trace LOGGER = TraceManager.getTrace(AssignableRolePopupContent.class);

    private QName searchParameter = RoleType.F_NAME;

    private static final String ID_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";

	public AssignableRolePopupContent(String id) {
		super(id);
	}

	protected Panel createPopupContent(){
		Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);
        BasicSearchPanel<AssignmentSearchDto> basicSearch = new BasicSearchPanel<AssignmentSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, AssignmentSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                assignmentSearchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                assignmentClearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);

   	 
        TablePanel panel = createTable();
//        add(panel);
        return panel;
	}
	
	  private void assignmentSearchPerformed(AjaxRequestTarget target){
	        ObjectQuery query = createSearchQuery();
	        TablePanel panel = (TablePanel) getTablePanel();
	        DataTable table = panel.getDataTable();
	        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
	        provider.setQuery(query);

	        target.add(get(ID_TABLE));
	        target.add(panel);
	    }
	  
	  protected void assignmentClearSearchPerformed(AjaxRequestTarget target){
	        searchModel.setObject(new AssignmentSearchDto());

	        TablePanel panel = (TablePanel) getTablePanel();
	        DataTable table = panel.getDataTable();
	        ObjectDataProvider provider = (ObjectDataProvider)table.getDataProvider();

	        if(getProviderQuery() != null){
	            provider.setQuery(getProviderQuery());
	        } else {
	            provider.setQuery(null);
	        }

	        target.add(panel);
	    }
	  
	  protected Panel getTablePanel(){
	        return (TablePanel) get(ID_TABLE);
	    }
	  
	  private TablePanel createTable() {
	        List<IColumn> columns = createMultiSelectColumns();
	        ObjectDataProvider provider = new ObjectDataProvider(getPageBase(), getType()){

	            @Override
	            protected void handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
	                if(result.isPartialError()){
	                    handlePartialError(result);
	                } else {
	                    super.handleNotSuccessOrHandledErrorInIterator(result);
	                }
	            }
	        };
	        provider.setQuery(getProviderQuery());
	        TablePanel table = new TablePanel(ID_TABLE, provider, columns);
	        table.setOutputMarkupId(true);

	        return table;
	    }
	  
	  private ObjectQuery createSearchQuery(){
	        AssignmentSearchDto dto = searchModel.getObject();
	        ObjectQuery query = null;

	        if(StringUtils.isEmpty(dto.getText())){
	            if(getProviderQuery() != null){
	                return getProviderQuery();
	            } else {
	                return null;
	            }
	        }

	        try{
	        	PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
	            String normalized = normalizer.normalize(dto.getText());

	            SubstringFilter substring = SubstringFilter.createSubstring(searchParameter, type, getPageBase().getPrismContext(),
	                    PolyStringNormMatchingRule.NAME, normalized);

	            if(getProviderQuery() != null){
	                AndFilter and = AndFilter.createAnd(getProviderQuery().getFilter(), substring);
	                query = ObjectQuery.createObjectQuery(and);
	            } else {
	                query = ObjectQuery.createObjectQuery(substring);
	            }

	        } catch (Exception e){
	            error(getString("OrgUnitBrowser.message.queryError") + " " + e.getMessage());
	            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
	        }

	        return query;
	    }
	  
	  protected <T extends ObjectType> List<ObjectType> getSelectedObjects(){
		  List<ObjectType> selected = new ArrayList<>();
		  TablePanel table = (TablePanel) getTablePanel();
	        ObjectDataProvider<SelectableBean<T>, T> provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
	        for (SelectableBean<T> bean : provider.getAvailableData()) {
	            if (!bean.isSelected()) {
	                continue;
	            }

	            selected.add(bean.getValue());
	        }
	        return selected;
	  }
	  
	  public void setType(Class type){
		  this.type = type;
		  TablePanel table = (TablePanel) getTablePanel();
	        if (table != null) {
	            ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
	            provider.setType(type);

	            //replace table with table with proper columns
	            replace(createTable());
	        }
	  }
	  
	  public QName getSearchParameter() {
	        return searchParameter;
	    }

	    public void setSearchParameter(QName searchParameter) {
	        Validate.notNull(searchParameter, "Search Parameter must not be null.");
	        this.searchParameter = searchParameter;
	    }
}
