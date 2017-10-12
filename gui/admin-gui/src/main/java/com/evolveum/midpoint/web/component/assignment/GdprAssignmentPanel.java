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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;

public class GdprAssignmentPanel extends AbstractRoleAssignmentPanel {

	private static final long serialVersionUID = 1L;

	public GdprAssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}


	@Override
	protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
		List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
		columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("AssignmentType.lifecycleState")) {
			private static final long serialVersionUID = 1L;
				@Override
				public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
					item.add(new Label(componentId, rowModel.getObject().getContainerValue().asContainerable().getLifecycleState()));
				}
		});

		columns.add(new CheckBoxColumn<ContainerValueWrapper<AssignmentType>>(createStringResource("AssignmnetType.accepted")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<Boolean> getEnabled() {
				return Model.of(Boolean.FALSE);
			}

			@Override
			protected IModel<Boolean> getCheckBoxValueModel(IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
				return new AbstractReadOnlyModel<Boolean>() {

					private static final long serialVersionUID = 1L;

					@Override
					public Boolean getObject() {
						AssignmentType assignmentType = rowModel.getObject().getContainerValue().getValue();
						if (assignmentType.getLifecycleState() == null) {
							return Boolean.FALSE;
						}

						if (assignmentType.getLifecycleState().equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
							return Boolean.TRUE;
						}

						return Boolean.FALSE;
					}
				};
			}

		});

		return columns;
	}

	@Override
	protected boolean isRelationVisible() {
		return false;
	}

	@Override
	protected <T extends ObjectType> void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<T> assignmentsList,
			QName relation) {
		super.addSelectedAssignmentsPerformed(target, assignmentsList, SchemaConstants.ORG_CONSENT);
	}

	protected ObjectQuery createObjectQuery() {
		return QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
				.block()
				.item(new ItemPath(AssignmentType.F_TARGET_REF))
				.ref(SchemaConstants.ORG_CONSENT)
				.endBlock()
				.build();
	}
}
