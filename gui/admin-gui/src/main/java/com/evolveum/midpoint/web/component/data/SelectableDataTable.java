package com.evolveum.midpoint.web.component.data;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

public class SelectableDataTable<T> extends DataTable<T>{

	public SelectableDataTable(String id, List<IColumn<T>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
		super(id, columns, dataProvider, rowsPerPage);
		
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	@Override
	protected Item<T> newRowItem(String id, int index, IModel<T> model) {
		final Item<T> rowItem = new Item<T>(id, index, model);

		rowItem.add(new AjaxEventBehavior("onclick") {

	        private static final long serialVersionUID = 6720512493017210281L;

	        @Override
	        protected void onEvent(AjaxRequestTarget target) {
	        	
	        	String id = rowItem.getId();
T object = rowItem.getModel().getObject();
	        	
	        	SelectableBean selectable = null;
	        	if (object instanceof SelectableBean<?>){
	        		
	        		selectable = (SelectableBean) object;
	        	}
	        	if (selectable == null){
	        		if (object instanceof ResourceDto){
	        			ResourceDto resource = (ResourceDto) object;
	        			boolean enabled = !resource.isSigned();
	        			resource.setSelected(enabled);
	        			resource.setSigned(enabled);
	        		}
	        		return;
	        	}
	        	boolean enabled = !selectable.isSigned(); 
	        		((SelectableBean) object).setSelected(enabled);
	        		((SelectableBean) object).setSigned(enabled);
	        	
	        }
	    });
	        
	   rowItem.setOutputMarkupId(true);
	    return rowItem;

	}
	
}
