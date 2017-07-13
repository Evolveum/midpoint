/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
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

public class ColumnUtils {

	public static <T> List<IColumn<T, String>> createColumns(List<ColumnTypeDto<String>> columns) {
		List<IColumn<T, String>> tableColumns = new ArrayList<>();
		for (ColumnTypeDto<String> column : columns) {
			PropertyColumn<T, String> tableColumn = null;
			if (column.isSortable()) {
				tableColumn = createPropertyColumn(column.getColumnName(), column.getSortableColumn(),
						column.getColumnValue(), column.isMultivalue());

			} else {
				tableColumn = new PropertyColumn<T, String>(createStringResource(column.getColumnName()),
						column.getColumnValue());
			}
			tableColumns.add(tableColumn);

		}
		return tableColumns;
	}

	private static <T> PropertyColumn<T, String> createPropertyColumn(String name, String sortableProperty,
			final String expression, final boolean multivalue) {

		return new PropertyColumn<T, String>(createStringResource(name), sortableProperty, expression) {
			private static final long serialVersionUID = 1L;

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

	public static <O extends ObjectType> List<IColumn<SelectableBean<O>, String>> getDefaultColumns(Class<? extends O> type) {
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
			return new ArrayList<>();
//			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}
	
	public static <O extends ObjectType> IColumn<SelectableBean<O>, String> createIconColumn(Class<? extends O> type){
		
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
		} else if (ShadowType.class.equals(type)) {
			return getShadowIconColumn();
		} else if (type.equals(TaskType.class)) {
			return getTaskIconColumn();
		} else if (type.equals(ResourceType.class)) {
			return getResourceIconColumn();
		} else if (type.equals(AccessCertificationDefinitionType.class)) {
			return getAccessCertificationDefinitionIconColumn();
		} else {
			return getEmptyIconColumn();
//			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getEmptyIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return "";
					}
				};
			}
		};
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getDefaultIcons(){
		return new IconColumn<SelectableBean<T>>(createStringResource("userBrowserDialog.type")) {

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T object = rowModel.getObject().getValue();
						return object != null ? WebComponentUtil.createDefaultIcon(object.asPrismObject()) : null;
					}
				};

			}
		};
	}
	
	private static IModel<String> createIconColumnHeaderModel() {
		return new Model<String>() {
			@Override
			public String getObject() {
				return "";
			}
		};
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getUserIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T user = rowModel.getObject().getValue();
						return user != null ? WebComponentUtil.createUserIcon(user.asPrismContainer()) : null;
					}
				};
			}

            @Override
            protected IModel<String> createTitleModel(final IModel<SelectableBean<T>> rowModel) {

                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        T user = rowModel.getObject().getValue();
                        String iconClass = user != null ? WebComponentUtil.createUserIcon(user.asPrismContainer()) : null;
                        String compareStringValue = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE;
                        String titleValue = "";
                        if (iconClass != null &&
                                iconClass.startsWith(compareStringValue) &&
                                iconClass.length() > compareStringValue.length()){
                            titleValue = iconClass.substring(compareStringValue.length());
                        }
                        return createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue) == null ?
                                "" : createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue).getString();
                    }
                };
            }

        };
	}
	
	public static <T extends ObjectType> IColumn<SelectableBean<T>, String> getShadowIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T shadow = rowModel.getObject().getValue();
						if (shadow == null) {
							return WebComponentUtil.createErrorIcon(rowModel.getObject().getResult());
						} else {
							return WebComponentUtil.createShadowIcon(shadow.asPrismContainer());
						}
					}
				};
			}

			@Override
			public Component getHeader(String componentId) {
				return new Label(componentId, "");
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<T>> rowModel) {
				T shadow = rowModel.getObject().getValue();
				if (shadow == null){
					return super.getDataModel(rowModel);
				}
				return ShadowUtil.isProtected(shadow.asPrismContainer()) ?
						createStringResource("ThreeStateBooleanPanel.true") : createStringResource("ThreeStateBooleanPanel.false");
			}


			@Override
			public IModel<String> getDisplayModel(){
				return createStringResource("pageContentAccounts.isProtected");
			}
		};
	}
	
	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getRoleIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T role = rowModel.getObject().getValue();
						return role != null ? WebComponentUtil.createRoleIcon(role.asPrismContainer()) : null;
					}
				};
			}
		};
	}

	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getOrgIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T org = rowModel.getObject().getValue();
						return org != null ? WebComponentUtil.createOrgIcon(org.asPrismContainer()) : null;
					}
				};
			}
		};
	}

	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getServiceIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {

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
						return service != null ? WebComponentUtil.createServiceIcon(service.asPrismContainer()) : null;
					}
				};
			}
		};
	}

	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getTaskIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {

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
						return task != null ? WebComponentUtil.createTaskIcon(task.asPrismContainer()) : null;
					}
				};
			}
		};
	}

	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getAccessCertificationDefinitionIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
					}
				};
			}
		};
	}


	private static <T extends ObjectType> IColumn<SelectableBean<T>, String> getResourceIconColumn(){
		return new IconColumn<SelectableBean<T>>(createIconColumnHeaderModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<T>> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						T resource = rowModel.getObject().getValue();
						if (resource == null) {
							return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON;
						} else {
							return WebComponentUtil.createResourceIcon(resource.asPrismContainer());
						}
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

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("UserType.givenName", UserType.F_GIVEN_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".givenName.orig", false),
				new ColumnTypeDto<String>("UserType.familyName", UserType.F_FAMILY_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".familyName.orig", false),
				new ColumnTypeDto<String>("UserType.fullName", UserType.F_FULL_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".fullName.orig", false),
				new ColumnTypeDto<String>("UserType.emailAddress", UserType.F_EMAIL_ADDRESS.getLocalPart(),
						SelectableBean.F_VALUE + ".emailAddress", false)

		);
		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultTaskColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		columns.add(
				new AbstractColumn<SelectableBean<T>, String>(createStringResource("TaskType.kind")) {
					private static final long serialVersionUID = 1L;

					@Override
					public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
							String componentId, IModel<SelectableBean<T>> rowModel) {
						SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
						PrismProperty<ShadowKindType> pKind = object.getValue() != null ?
								object.getValue().asPrismObject().findProperty(
										new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
								: null;
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
				PrismProperty<String> pIntent = object.getValue() != null ?
						object.getValue().asPrismObject().findProperty(
								new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
						: null;
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
				PrismProperty<QName> pObjectClass = object.getValue() != null ?
						object.getValue().asPrismObject().findProperty(
								new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME))
						: null;
				if (pObjectClass != null) {
					cellItem.add(new Label(componentId, pObjectClass.getRealValue().getLocalPart()));
				} else {
					cellItem.add(new Label(componentId, ""));
				}

			}

		});

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("TaskType.executionStatus", TaskType.F_EXECUTION_STATUS.getLocalPart(),
						SelectableBean.F_VALUE + ".executionStatus", false));
		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

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
		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("AbstractRoleType.displayName",
						sortByDisplayName,
						SelectableBean.F_VALUE + ".displayName", false),
				new ColumnTypeDto<String>("AbstractRoleType.description",
						null,
						SelectableBean.F_VALUE + ".description", false),
				new ColumnTypeDto<String>("AbstractRoleType.identifier", sortByIdentifer,
						SelectableBean.F_VALUE + ".identifier", false)

		);
		return createColumns(columnsDefs);

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultResourceColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("AbstractRoleType.description", null,
						SelectableBean.F_VALUE + ".description", false)

		);

		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

		return columns;

	}

}
