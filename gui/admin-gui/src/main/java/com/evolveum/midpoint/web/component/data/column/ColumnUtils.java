/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ColumnUtils {

	public static List<IColumn> createColumns(List<ColumnTypeDto> columns){
		List<IColumn> tableColumns = new ArrayList<IColumn>();
		for (ColumnTypeDto column : columns) {
			// if (column.isMultivalue()) {
			// PropertyColumn tableColumn = new PropertyColumn(displayModel,
			// propertyExpression)
			// } else {
			PropertyColumn tableColumn = null;
			if (column.isSortable()){
				tableColumn = new PropertyColumn(createStringResource(column.getColumnName()),
					column.getColumnValue(), column.getColumnValue());
			} else {
				tableColumn = new PropertyColumn(createStringResource(column.getColumnName()),
						column.getColumnValue());
			}
			tableColumns.add(tableColumn);
			// }
		}
		return tableColumns;
	}
	
	public static <O extends ObjectType> List<IColumn> getDefaultColumns(Class<O> type) {
		if (type == null){
			return getDefaultUserColumns();
		}
		
		if (type.equals(UserType.class)){
			return getDefaultUserColumns();
		} else if (AbstractRoleType.class.isAssignableFrom(type)){
			return getDefaultAbstractRoleColumns();
		} else {
			throw new  UnsupportedOperationException("Will be implemented eventually");
		}
	}
	
	public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return new StringResourceModel(resourceKey).setModel(new Model<String>())
    			.setDefaultValue(resourceKey)
    			.setParameters(objects);
	}
	
	
    	public static <T extends ObjectType> List<IColumn> getDefaultUserColumns(){
    		List<IColumn> columns = new ArrayList<IColumn>();
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


    		List<ColumnTypeDto> columnsDefs = Arrays.asList(new ColumnTypeDto("UserType.givenName",
    				SelectableBean.F_VALUE + ".givenName.orig", true, false),
    				new ColumnTypeDto("UserType.familyName",
    						SelectableBean.F_VALUE + ".familyName.orig", true, false),
    				new ColumnTypeDto("UserType.fullName",
    						SelectableBean.F_VALUE + ".fullName.orig", true, false),
    				new ColumnTypeDto("UserType.emailAddress",
    						SelectableBean.F_VALUE + ".emailAddress", true, false)
    				
    				);
    		columns.addAll(createColumns(columnsDefs));
    		
    		
    		return columns;

    	}

    	public static <T extends ObjectType> List<IColumn> getDefaultRoleColumns(){
    		List<IColumn> columns = new ArrayList<IColumn>();
    		columns.add(new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

    			@Override
    			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
    				return new AbstractReadOnlyModel<String>() {

    					@Override
    					public String getObject() {
    						T user = rowModel.getObject().getValue();
    						return WebComponentUtil.createRoleIcon(user.asPrismContainer());
    					}
    				};
    			}
    		});
    		
    		columns.addAll(getDefaultAbstractRoleColumns());
    		
    		return columns;
    	}
    	
    	public static <T extends ObjectType> List<IColumn> getDefaultOrgColumns(){
    		List<IColumn> columns = new ArrayList<IColumn>();
    		columns.add(new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

    			@Override
    			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
    				return new AbstractReadOnlyModel<String>() {

    					@Override
    					public String getObject() {
    						T user = rowModel.getObject().getValue();
    						return WebComponentUtil.createOrgIcon(user.asPrismContainer());
    					}
    				};
    			}
    		});
    		
    		columns.addAll(getDefaultAbstractRoleColumns());
    		
    		return columns;
    	}
    	
    	
    	
    	private static <T extends ObjectType> List<IColumn> getDefaultAbstractRoleColumns(){
    		


		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				new ColumnTypeDto("AbstractRoleType.displayName",
    				 SelectableBean.F_VALUE + ".displayName", true, false),
    				new ColumnTypeDto("AbstractRoleType.description",
    						SelectableBean.F_VALUE + ".description", true, false),
    				new ColumnTypeDto("AbstractRoleType.identifier",
    						SelectableBean.F_VALUE + ".identifier", true,false)
    				
    				);
    		return createColumns(columnsDefs);
    

    	}
    	
    	public static <T extends ObjectType> List<IColumn> getDefaultResourceColumns(){
    		List<IColumn> columns = new ArrayList<IColumn>();
    		columns.add(new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

    			@Override
    			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
    				return new AbstractReadOnlyModel<String>() {

    					@Override
    					public String getObject() {
    						T user = rowModel.getObject().getValue();
    						return WebComponentUtil.createResourceIcon(user.asPrismContainer());
    					}
    				};
    			}
    		});

    		List<ColumnTypeDto> columnsDefs = Arrays.asList(
    				new ColumnTypeDto("AbstractRoleType.displayName",
        				 SelectableBean.F_VALUE + ".displayName", true, false),
        				new ColumnTypeDto("AbstractRoleType.description",
        						SelectableBean.F_VALUE + ".description", true, false),
        				new ColumnTypeDto("AbstractRoleType.identifier",
        						SelectableBean.F_VALUE + ".identifier", true,false)
        				
        				);
      	
    		columns.addAll(createColumns(columnsDefs));
    		
    		
    		return columns;

    	}
    	
    	
//return StringResourceModelMigration.of(resourceKey, this, new Model<String>(), resourceKey, objects);

}
