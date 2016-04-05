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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ColumnUtils {

	public static List<IColumn> createColumns(List<ColumnTypeDto> columns) {
		List<IColumn> tableColumns = new ArrayList<IColumn>();
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

	public static <O extends ObjectType> List<IColumn> getDefaultColumns(Class<O> type) {
		if (type == null) {
			return getDefaultUserColumns();
		}

		if (type.equals(UserType.class)) {
			return getDefaultUserColumns();
		} else if (RoleType.class.equals(type)) {
			return getDefaultRoleColumns();
		} else if (OrgType.class.equals(type)) {
			return getDefaultOrgColumns();
		} else if (type.equals(TaskType.class)) {
			return getDefaultTaskColumns();
		} else if (type.equals(ResourceType.class)) {
			return getDefaultResourceColumns();
		} else {
			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}

	public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
				.setParameters(objects);
	}

	public static <T extends ObjectType> List<IColumn> getDefaultUserColumns() {
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
		columns.addAll(createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn> getDefaultTaskColumns() {
		List<IColumn> columns = new ArrayList<IColumn>();

		columns.add(
				new AbstractColumn<SelectableBean<TaskType>, String>(createStringResource("TaskType.kind")) {

					@Override
					public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem,
							String componentId, IModel<SelectableBean<TaskType>> rowModel) {
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

		columns.add(new AbstractColumn<SelectableBean<TaskType>, String>(
				createStringResource("TaskType.intent")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem,
					String componentId, IModel<SelectableBean<TaskType>> rowModel) {
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

		columns.add(new AbstractColumn<SelectableBean<TaskType>, String>(
				createStringResource("TaskType.objectClass")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem,
					String componentId, IModel<SelectableBean<TaskType>> rowModel) {
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
		columns.addAll(createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn> getDefaultRoleColumns() {
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

	public static <T extends ObjectType> List<IColumn> getDefaultOrgColumns() {
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

	private static <T extends ObjectType> List<IColumn> getDefaultAbstractRoleColumns() {

		List<ColumnTypeDto> columnsDefs = Arrays.asList(
				new ColumnTypeDto("AbstractRoleType.displayName",
						AbstractRoleType.F_DISPLAY_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".displayName", false),
				new ColumnTypeDto("AbstractRoleType.description",
						AbstractRoleType.F_DESCRIPTION.getLocalPart(),
						SelectableBean.F_VALUE + ".description", false),
				new ColumnTypeDto("AbstractRoleType.identifier", AbstractRoleType.F_IDENTIFIER.getLocalPart(),
						SelectableBean.F_VALUE + ".identifier", false)

		);
		return createColumns(columnsDefs);

	}

	public static <T extends ObjectType> List<IColumn> getDefaultResourceColumns() {
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
				// new ColumnTypeDto("AbstractRoleType.displayName",
				// SelectableBean.F_VALUE + ".displayName",
				// true, false),
				new ColumnTypeDto("AbstractRoleType.description", ResourceType.F_DESCRIPTION.getLocalPart(),
						SelectableBean.F_VALUE + ".description", false)
		// new ColumnTypeDto("AbstractRoleType.identifier",
		// SelectableBean.F_VALUE + ".identifier", true,
		// false)

		);

		columns.addAll(createColumns(columnsDefs));

		return columns;

	}

}
