package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LookupPropertyModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AssignableSelectionPanel <T extends ObjectType> extends AbstractAssignableSelectionPanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignableSelectionPanel.class);

    private static final String DOT_CLASS = AssignableSelectionPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_TYPES = DOT_CLASS + "loadRoleTypes";

    private QName searchParameter = RoleType.F_NAME;

    private static final String ID_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_TYPE_SEARCH = "typeSelect";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    
	public AssignableSelectionPanel(String id, AbstractAssignableSelectionPanel.Context context) {
		super(id, context);
	}

    public static abstract class Context extends AbstractAssignableSelectionPanel.Context {
        protected QName defaultSearchParameter;

        public Context(Component callingComponent) {
            super(callingComponent);
        }

        public void setSearchParameter(QName searchParameter) {
            defaultSearchParameter = searchParameter;
            if (modalWindowPageReference != null) {
                AssignableSelectionPanel panel = (AssignableSelectionPanel) modalWindowPageReference.getPage().get(AssignableSelectionPage.ID_ASSIGNABLE_SELECTION_PANEL);
                if (panel != null) {
                    panel.setSearchParameter(searchParameter);
                }
            }
        }

        public QName getDefaultSearchParameter() {
            return defaultSearchParameter;
        }
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

        DropDownChoice typeSearch = new DropDownChoice<>(ID_TYPE_SEARCH,
                new LookupPropertyModel<String>(searchModel, AssignmentSearchDto.F_SEARCH_ROLE_TYPE, null){

                    @Override
                    public String getObject() {
                        final Object target = getInnermostModelOrObject();

                        if (target != null){
                            String key = (String) PropertyResolver.getValue(expression, target);

                            if(key == null){
                                return null;
                            }

                            for(DisplayableValue<String> displayable: getLookupDisplayableList()){
                                if(key.equals(displayable.getValue())){
                                    return displayable.getLabel();
                                }
                            }
                        }

                        return null;
                    }

                    @Override
                    public void setObject(String object) {
                        final String expression = propertyExpression();

                        PropertyResolverConverter prc;
                        prc = new PropertyResolverConverter(Application.get().getConverterLocator(),
                                Session.get().getLocale());

                        if(object != null){
                            String key;

                            for(DisplayableValue<String> displayable: getLookupDisplayableList()){
                                if(object.equals(displayable.getLabel())){
                                    key = displayable.getValue();

                                    PropertyResolver.setValue(expression, getInnermostModelOrObject(), key, prc);
                                    return;
                                }
                            }
                        }

                        PropertyResolver.setValue(expression, getInnermostModelOrObject(), null, prc);
                    }
                },
                createAvailableRoleTypesList());
        typeSearch.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                assignmentSearchPerformed(target);
            }
        });
        add(typeSearch);

        TablePanel table = createTable();
        add(table);
        return table;
	}

    private List<? extends DisplayableValue<String>> getLookupDisplayableList(){
        List<DisplayableValue<String>> list = new ArrayList<>();
        ModelInteractionService interactionService = WebMiscUtil.getPageBase(this).getModelInteractionService();
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE_TYPES);

        try {
            RoleSelectionSpecification roleSpecification = interactionService.getAssignableRoleSpecification(getUserDefinition(), result);
            return roleSpecification.getRoleTypes();

        } catch (SchemaException | ConfigurationException | ObjectNotFoundException e) {
            LOGGER.error("Could not retrieve available role types for search purposes.", e);
            result.recordFatalError("Could not retrieve available role types for search purposes.", e);
        }

        return list;
    }

    private List<String> createAvailableRoleTypesList(){
        List<String> roleTypes = new ArrayList<>();
        List<? extends DisplayableValue<String>> displayableValues = getLookupDisplayableList();

        if (displayableValues != null) {
            for (DisplayableValue<String> displayable : displayableValues) {
                roleTypes.add(displayable.getLabel());
            }
        }

        return roleTypes;
    }

    protected PrismObject<UserType> getUserDefinition(){
        return context.getUserDefinition();
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
		ObjectDataProvider<SelectableBean<T>, T> provider = new ObjectDataProvider<SelectableBean<T>, T>(WebMiscUtil.getPageBase(this), getType()){

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

	    try{
            List<ObjectFilter> filters = new ArrayList<>();

            if(getProviderQuery() != null){
                filters.add(getProviderQuery().getFilter());
            }

            if(dto.getText() != null && StringUtils.isNotEmpty(dto.getText())){
                PrismContext prismContext = WebMiscUtil.getPageBase(this).getPrismContext();
                PolyStringNormalizer normalizer = prismContext.getDefaultPolyStringNormalizer();
                String normalized = normalizer.normalize(dto.getText());

                SubstringFilter substring = SubstringFilter.createSubstring(searchParameter, type, prismContext,
                        PolyStringNormMatchingRule.NAME, normalized);
                filters.add(substring);
            }

            if(dto.getType() != null){
                EqualFilter typeEquals = EqualFilter.createEqual(RoleType.F_ROLE_TYPE, RoleType.class, WebMiscUtil.getPageBase(this).getPrismContext(),
                        null, dto.getType());
                filters.add(typeEquals);
            }

            query = filters.isEmpty() ? null : ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));

	    } catch (Exception e){
	        error(getString("OrgUnitBrowser.message.queryError") + " " + e.getMessage());
	        LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
	    }

	    return query;
	}
	  
	protected List<T> getSelectedObjects(){
	    List<T> selected = new ArrayList<>();
	    TablePanel table = (TablePanel) getTablePanel();
	    ObjectDataProvider<SelectableBean<T>, T> provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
//	    for (SelectableBean<T> bean : provider.getSelectedData()) {
//	        if (!bean.isSelected()) {
//	            continue;
//	        }
//
//	        selected.add(bean.getValue());
//	    }
//	    return selected;
	    return provider.getSelectedData();
	}
	  
	public void setType(Class type){
	    this.type = type;
		TablePanel table = (TablePanel) getTablePanel();

        if (table != null) {
	        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
	        provider.setType(type);

	        //replace table with table with proper columns
	        addOrReplace(createTable());
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
