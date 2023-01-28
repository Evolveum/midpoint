/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.wf;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * @author lazyman
 */
public class DecisionsPanel extends BasePanel<List<DecisionDto>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_DECISIONS_TABLE = "decisionsTable";

    // todo options to select which columns will be shown
    public DecisionsPanel(String id, IModel<List<DecisionDto>> model, UserProfileStorage.TableId tableId) {
        super(id, model);
        initLayout(tableId);
    }

    protected void initLayout(UserProfileStorage.TableId tableId) {
        List<IColumn<DecisionDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.user"), DecisionDto.F_USER));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.attorney"), DecisionDto.F_ATTORNEY));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.originalAssignee"), DecisionDto.F_ORIGINAL_ASSIGNEE));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.assigneeChange"), DecisionDto.F_ASSIGNEE_CHANGE));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.stage"), DecisionDto.F_STAGE));
        columns.add(createOutcomeColumn());
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.comment"), DecisionDto.F_COMMENT));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.when"), DecisionDto.F_TIME));
        columns.add(new PropertyColumn<>(createStringResource("DecisionsPanel.escalation"), DecisionDto.F_ESCALATION_LEVEL_NUMBER));

        ISortableDataProvider<?, ?> provider = new ListDataProvider<>(this, getModel());
        BoxedTablePanel<?> decisionsTable = new BoxedTablePanel<>(
                ID_DECISIONS_TABLE, provider, columns, tableId);
        add(decisionsTable);
    }

    @NotNull
    private IconColumn<DecisionDto> createOutcomeColumn() {
        return new IconColumn<DecisionDto>(createStringResource("DecisionsPanel.result")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(final IModel<DecisionDto> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(choose(rowModel,
                        ApprovalOutcomeIcon.IN_PROGRESS.getIcon(), ApprovalOutcomeIcon.FORWARDED.getIcon(),
                        ApprovalOutcomeIcon.APPROVED.getIcon(), ApprovalOutcomeIcon.REJECTED.getIcon()),
                        "",
                        choose(rowModel,
                                createStringResource("MyRequestsPanel.inProgress").getString(),
                                createStringResource("MyRequestsPanel.forwarded").getString(),
                                createStringResource("MyRequestsPanel.approved").getString(),
                                createStringResource("MyRequestsPanel.rejected").getString()));
            }

            @Override
            public String getCssClass() {
                return "shrink";
            }

            private String choose(IModel<DecisionDto> rowModel, String inProgress, String forwarded, String approved, String rejected) {
                DecisionDto dto = rowModel.getObject();
                if (StringUtils.isNotEmpty(dto.getAssigneeChange())) {
                    return forwarded;
                }
                if (dto.getOutcome() == null) {
                    return inProgress;
                } else {
                    return dto.getOutcome() ? approved : rejected;
                }
            }
        };
    }

}
