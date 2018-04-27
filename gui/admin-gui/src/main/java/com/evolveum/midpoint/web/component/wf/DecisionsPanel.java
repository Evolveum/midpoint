/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.component.wf;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author lazyman
 * @author mederly
 */
public class DecisionsPanel extends BasePanel<List<DecisionDto>> {
	private static final long serialVersionUID = 1L;

	private static final String ID_DECISIONS_TABLE = "decisionsTable";

    // todo options to select which columns will be shown
    public DecisionsPanel(String id, IModel<List<DecisionDto>> model, UserProfileStorage.TableId tableId, int pageSize) {
        super(id, model);
		initLayout(tableId, pageSize);
    }

    protected void initLayout(UserProfileStorage.TableId tableId, int pageSize) {
        List<IColumn<DecisionDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.user"), DecisionDto.F_USER));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.attorney"), DecisionDto.F_ATTORNEY));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.originalActor"), DecisionDto.F_ORIGINAL_ACTOR));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.stage"), DecisionDto.F_STAGE));
		columns.add(createOutcomeColumn());
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.comment"), DecisionDto.F_COMMENT));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.when"), DecisionDto.F_TIME));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.escalation"), DecisionDto.F_ESCALATION_LEVEL_NUMBER));

        ISortableDataProvider provider = new ListDataProvider<>(this, getModel());
        BoxedTablePanel decisionsTable = new BoxedTablePanel<>(ID_DECISIONS_TABLE, provider, columns, tableId, pageSize);
        add(decisionsTable);
    }

	@NotNull
	private IconColumn<DecisionDto> createOutcomeColumn() {
		return new IconColumn<DecisionDto>(createStringResource("DecisionsPanel.result")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(final IModel<DecisionDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return choose(rowModel, ApprovalOutcomeIcon.IN_PROGRESS.getIcon(), ApprovalOutcomeIcon.APPROVED.getIcon(), ApprovalOutcomeIcon.REJECTED.getIcon());
					}
				};
			}
			
			@Override
		    public String getCssClass() {
		        return "shrink";
		    }

			@Override
			protected IModel<String> createTitleModel(final IModel<DecisionDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return choose(rowModel,
								createStringResource("MyRequestsPanel.inProgress").getString(),
								createStringResource("MyRequestsPanel.approved").getString(),
								createStringResource("MyRequestsPanel.rejected").getString());
					}
				};
			}

			private String choose(IModel<DecisionDto> rowModel, String inProgress, String approved, String rejected) {
				DecisionDto dto = rowModel.getObject();
				if (dto.getOutcome() == null) {
					return inProgress;
				} else {
					return dto.getOutcome() ? approved : rejected;
				}
			}
		};
	}

}
