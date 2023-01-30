/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
@PanelType(name = "results")
@PanelInstance(identifier = "results", applicableForType = TaskType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageTask.result.title", icon = GuiStyleConstants.CLASS_ICON_TASK_RESULTS, order = 70))
public class TaskResultPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULT = "operationResult";
    private static final String ID_SHOW_RESULT = "showResult";

    private static final Trace LOGGER = TraceManager.getTrace(TaskResultPanel.class);

    public TaskResultPanel(String id, TaskDetailsModel taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);

    }

    protected void initLayout() {

        IModel<List<OperationResult>> resultModel= new ReadOnlyModel<>(this::createOperationResultList);

        SelectableListDataProvider<SelectableBean<OperationResult>, OperationResult> provider = new SelectableListDataProvider<>(this, resultModel);
        BoxedTablePanel<SelectableBean<OperationResult>> resultTablePanel = new BoxedTablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns());
        resultTablePanel.setOutputMarkupId(true);
        add(resultTablePanel);

        AjaxFallbackLink<Void> showResult = new AjaxFallbackLink<>(ID_SHOW_RESULT) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(Optional<AjaxRequestTarget> optionalTarget) {
                if (optionalTarget.isEmpty()) {
                    LOGGER.warn("Cannot show result in interactive way, request target not present.");
                    return;
                }
                AjaxRequestTarget target = optionalTarget.get();
                OperationResultPopupPanel body = new OperationResultPopupPanel(
                        getPageBase().getMainPopupBodyId(),
                        new Model<>(getTaskOperationResult()));
                body.setOutputMarkupId(true);
                getPageBase().showMainPopup(body, target);
            }

        };
        showResult.add(new VisibleBehaviour(() -> getTaskOperationResult() != null));
        showResult.setOutputMarkupId(true);
        add(showResult);

    }

    private OperationResult getTaskOperationResult() {
        PrismObjectWrapper<TaskType> taskWrapper = TaskResultPanel.this.getObjectWrapper();
        TaskType taskType = taskWrapper.getObject().asObjectable();
        return OperationResult.createOperationResult(taskType.getResult());
    }

    private List<OperationResult> createOperationResultList() {
        PrismObject<TaskType> taskPrism = getObjectWrapper().getObject();
        if (taskPrism == null) {
            return null;
        }

        OperationResultType result = taskPrism.asObjectable().getResult();
        if (result == null) {
            return null;
        }

        List<OperationResult> results = new ArrayList<>();
        OperationResult opResult = OperationResult.createOperationResult(result);
        results.add(opResult);
        results.addAll(opResult.getSubresults());
        return results;
    }

    private List<IColumn<SelectableBean<OperationResult>, String>> initResultColumns() {
        List<IColumn<SelectableBean<OperationResult>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.token"), OperationResultType.F_TOKEN.getLocalPart(), createPropertyExpression("token")));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.operation"), OperationResultType.F_OPERATION.getLocalPart(), createPropertyExpression("operation")));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.status"), OperationResultType.F_STATUS.getLocalPart(), createPropertyExpression("status")));
        columns.add(new AbstractColumn<>(createStringResource("pageTaskEdit.opResult.timestamp"), OperationResultType.F_END.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<OperationResult>>> cellItem, String componentId,
                    IModel<SelectableBean<OperationResult>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> {
                    Long resultEndTime = rowModel.getObject().getValue().getEnd();
                    return resultEndTime != null && resultEndTime > 0 ?
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(resultEndTime), getPageBase()) : "";
                });
                cellItem.add(label);
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("pageTaskEdit.opResult.message"), OperationResultType.F_MESSAGE.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<OperationResult>>> cellItem, String componentId,
                    IModel<SelectableBean<OperationResult>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> WebComponentUtil.nl2br(rowModel.getObject().getValue().getMessage()));
                label.setEscapeModelStrings(false);
                cellItem.add(label);
            }
        });
        return columns;
    }

    private String createPropertyExpression(String propertyName) {
        return SelectableBeanImpl.F_VALUE + "." + propertyName;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(get(ID_OPERATION_RESULT));
    }

}
