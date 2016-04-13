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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ColumnUtils {

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> createColumns(List<ColumnTypeDto> columns) {
		List<IColumn<SelectableBean<T>, String>> tableColumns = new ArrayList<IColumn<SelectableBean<T>, String>>();
		for (ColumnTypeDto column : columns) {
			PropertyColumn tableColumn = null;
			if (column.isSortable()) {
				tableColumn = createPropertyColumn(column.getColumnName(), column.getSortableColumn(),
						column.getColumnValue(), column.isMultivalue());

			} else {
				tableColumn = new PropertyColumn(createStringResource(column.getColumnName()),
						column.getColumnValue());
			}
			tableColumns.add(tableColumn);

		}
		return tableColumns;
	}

	private static PropertyColumn createPropertyColumn(String name, String sortableProperty,
			final String expression, final boolean multivalue) {

		return new PropertyColumn(createStringResource(name), sortableProperty, expression) {

			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				if (multivalue) {
					IModel<List> values = new PropertyModel<List>(rowModel, expression);
					RepeatingView repeater = new RepeatingView(componentId);
					for (final Object task : values.getObject()) {
						repeater.add(new Label(repeater.newChildId(), task.toString()));
					}
					item.add(repeater);
					return;
				}

				super.populateItem(item, componentId, rowModel);
			}
		};

	}

	public static <O extends ObjectType> List<IColumn<SelectableBean<O>, String>> getDefaultColumns(Class<O> type) {
		if (type == null) {
			return getDefaultUserColumns();
		}

		if (type.equals(UserType.class)) {
			return getDefaultUserColumns();
		} else if (RoleType.class.equals(type)) {
			return getDefaultRoleColumns();
		} else if (OrgType.class.equals(type)) {
			return getDefaultOrgColumns();
		} else if (ServiceType.class.equals(type)) {
			return getDefaultServiceColumns();
		} else if (type.equals(TaskType.class)) {
			return getDefaultTaskColumns();
		} else if (type.equals(ResourceType.class)) {
			return getDefaultResourceColumns();
		} else {
			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}
	
	public static <T extends ObjectType> IColumn<SelectableBean<T>, String> createIconColumn(Class<T> type){
		
		if (type.equals(ObjectType.class)){
			return getDefaultIcons();
		}
		
		if (type.equals(UserType.class)) {
			return getUserIconColumn();
		} else if (RoleType.class.equals(type)) {
			return getRoleIconColumn();
		} else if (OrgType.class.equals(type)) {
			return getOrgIconColumn();
		} else if (ServiceType.class.equals(type)) {
			return getServiceIconColumn();
		} else if (type.equals(TaskType.class)) {
			return getTaskIconColumn();
		} else if (type.equals(ResourceType.class)) {
			return getResourceIconColumn();
		} else {
			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getDefaultIcons(){
		return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						T object = rowModel.getObject().getValue();
						return WebComponentUtil.createDefaultIcon(object);
						
					}
				};
			}
		};
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getUserIconColumn(){
		return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

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
		};
	}
	
	public static <T extends ObjectType> IColumn<SelectableBean<T>, String> getShadowIconColumn(){
		return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						T shadow = rowModel.getObject().getValue();
						return WebComponentUtil.createShadowIcon(shadow.asPrismContainer());
					}
				};
			}
		};
	}
	
private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getRoleIconColumn(){
	return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

		@Override
		protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				@Override
				public String getObject() {
					T role = rowModel.getObject().getValue();
					return WebComponentUtil.createRoleIcon(role.asPrismContainer());
				}
			};
		}
	};
	}

private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getOrgIconColumn(){
	return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

		@Override
		protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				@Override
				public String getObject() {
					T org = rowModel.getObject().getValue();
					return WebComponentUtil.createOrgIcon(org.asPrismContainer());
				}
			};
		}
	};
}

private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getServiceIconColumn(){
	return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject() {
					T service = rowModel.getObject().getValue();
					return WebComponentUtil.createServiceIcon(service.asPrismContainer());
				}
			};
		}
	};
}

private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getTaskIconColumn(){
	return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				private static final long serialVersionUID = 1L;

				@Override
				public String getObject() {
					T task = rowModel.getObject().getValue();
					return WebComponentUtil.createTaskIcon(task.asPrismContainer());
				}
			};
		}
	};
}

private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getResourceIconColumn(){
	return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

		@Override
		protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				@Override
				public String getObject() {
					T resource = rowModel.getObject().getValue();
					return WebComponentUtil.createResourceIcon(resource.asPrismContainer());
				}
			};
		}
	};
}

	public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
				.setParameters(objects);
	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultUserColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				new ColumnTypeDto("UserType.givenName", UserType.F_GIVEN_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".givenName.orig", false),
				new ColumnTypeDto("UserType.familyName", UserType.F_FAMILY_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".familyName.orig", false),
				new ColumnTypeDto("UserType.fullName", UserType.F_FULL_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".fullName.orig", false),
				new ColumnTypeDto("UserType.emailAddress", UserType.F_EMAIL_ADDRESS.getLocalPart(),
						SelectableBean.F_VALUE + ".emailAddress", false)

		);
		columns.addAll((Collection)createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultTaskColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		columns.add(
				new AbstractColumn<SelectableBean<T>, String>(createStringResource("TaskType.kind")) {

					@Override
					public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
							String componentId, IModel<SelectableBean<T>> rowModel) {
						SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
						PrismProperty<ShadowKindType> pKind = object.getValue().asPrismObject().findProperty(
								new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
						if (pKind != null) {
							cellItem.add(new Label(componentId, WebComponentUtil
									.createLocalizedModelForEnum(pKind.getRealValue(), cellItem)));
						} else {
							cellItem.add(new Label(componentId));
						}

					}

				});

		columns.add(new AbstractColumn<SelectableBean<T>, String>(
				createStringResource("TaskType.intent")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
					String componentId, IModel<SelectableBean<T>> rowModel) {
				SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
				PrismProperty<String> pIntent = object.getValue().asPrismObject().findProperty(
						new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
				if (pIntent != null) {
					cellItem.add(new Label(componentId, pIntent.getRealValue()));
				} else {
					cellItem.add(new Label(componentId));
				}
			}

		});

		columns.add(new AbstractColumn<SelectableBean<T>, String>(
				createStringResource("TaskType.objectClass")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
					String componentId, IModel<SelectableBean<T>> rowModel) {
				SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
				PrismProperty<QName> pObjectClass = object.getValue().asPrismObject().findProperty(
						new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME));
				if (pObjectClass != null) {
					cellItem.add(new Label(componentId, pObjectClass.getRealValue().getLocalPart()));
				} else {
					cellItem.add(new Label(componentId, ""));
				}

			}

		});

		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				new ColumnTypeDto("TaskType.executionStatus", TaskType.F_EXECUTION_STATUS.getLocalPart(),
						SelectableBean.F_VALUE + ".executionStatus", false));
		columns.addAll((Collection)createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultRoleColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();
		

		columns.addAll((Collection)getDefaultAbstractRoleColumns(RoleType.COMPLEX_TYPE));

		return columns;
	}
	
	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultServiceColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();
	
		columns.addAll((Collection)getDefaultAbstractRoleColumns(ServiceType.COMPLEX_TYPE));

		return columns;
	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultOrgColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		columns.addAll((Collection)getDefaultAbstractRoleColumns(OrgType.COMPLEX_TYPE));

		return columns;
	}

	private static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultAbstractRoleColumns(QName type) {

		String sortByDisplayName = null;
		String sortByIdentifer = null;
		if (OrgType.COMPLEX_TYPE.equals(type)) {
			sortByDisplayName = AbstractRoleType.F_DISPLAY_NAME.getLocalPart();
			sortByIdentifer = AbstractRoleType.F_IDENTIFIER.getLocalPart();
		}
		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				new ColumnTypeDto("AbstractRoleType.displayName",
						sortByDisplayName,
						SelectableBean.F_VALUE + ".displayName", false),
				new ColumnTypeDto("AbstractRoleType.description",
						null,
						SelectableBean.F_VALUE + ".description", false),
				new ColumnTypeDto("AbstractRoleType.identifier", sortByIdentifer,
						SelectableBean.F_VALUE + ".identifier", false)

		);
		return createColumns(columnsDefs);

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultResourceColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				// new ColumnTypeDto("AbstractRoleType.displayName",
				// SelectableBean.F_VALUE + ".displayName",
				// true, false),
				new ColumnTypeDto("AbstractRoleType.description", ResourceType.F_DESCRIPTION.getLocalPart(),
						SelectableBean.F_VALUE + ".description", false)
		// new ColumnTypeDto("AbstractRoleType.identifier",
		// SelectableBean.F_VALUE + ".identifier", true,
		// false)

		);

		columns.addAll((Collection) createColumns(columnsDefs));

		return columns;

	}

}
