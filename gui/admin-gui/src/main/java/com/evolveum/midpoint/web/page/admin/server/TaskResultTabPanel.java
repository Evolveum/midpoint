/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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

import java.util.*;

/**
 * @author semancik
 */
public class TaskResultTabPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULT = "operationResult";
    private static final String ID_SHOW_RESULT = "showResult";

    private static final Trace LOGGER = TraceManager.getTrace(TaskResultTabPanel.class);

    public TaskResultTabPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel) {
        super(id, taskWrapperModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {

        LoadableModel<List<OperationResult>> resultModel= new LoadableModel<List<OperationResult>>() {
            @Override
            protected List<OperationResult> load() {
                PrismObject<TaskType> taskPrism = getModelObject().getObject();
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
        };

        SelectableListDataProvider<SelectableBean<OperationResult>, OperationResult> provider = new SelectableListDataProvider<>(this, resultModel);
        BoxedTablePanel<SelectableBean<OperationResult>> resultTablePanel = new BoxedTablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns());
        resultTablePanel.setOutputMarkupId(true);
        add(resultTablePanel);

        AjaxFallbackLink<Void> showResult = new AjaxFallbackLink<Void>(ID_SHOW_RESULT) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(Optional<AjaxRequestTarget> optionalTarget) {
                AjaxRequestTarget target = optionalTarget.get();
                PrismObjectWrapper<TaskType> taskWrapper = TaskResultTabPanel.this.getModelObject();
                TaskType taskType = taskWrapper.getObject().asObjectable();
                OperationResult opResult = OperationResult.createOperationResult(taskType.getResult());
                OperationResultPanel body = new OperationResultPanel(
                        getPageBase().getMainPopupBodyId(),
                        new Model<>(OpResult.getOpResult(getPageBase(), opResult)),
                        getPageBase());
                body.setOutputMarkupId(true);
                getPageBase().showMainPopup(body, target);
            }


        };
        showResult.setOutputMarkupId(true);
        add(showResult);

    }

    private List<IColumn<SelectableBean<OperationResult>, String>> initResultColumns() {
        List<IColumn<SelectableBean<OperationResult>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.token"), createPropertyExpression("token")));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.operation"), createPropertyExpression("operation")));
        columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.status"), createPropertyExpression("status")));
        columns.add(new AbstractColumn<SelectableBean<OperationResult>, String>(createStringResource("pageTaskEdit.opResult.timestamp")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<OperationResult>>> cellItem, String componentId,
                                     IModel<SelectableBean<OperationResult>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> {
                    Long resultEndTime = rowModel.getObject().getValue().getEnd();
                    return resultEndTime != null && resultEndTime > 0 ?
                            WebComponentUtil.getShortDateTimeFormattedValue(new Date(), getPageBase()) : "";
                });
                cellItem.add(label);
            }
        });
        columns.add(new AbstractColumn<SelectableBean<OperationResult>, String>(createStringResource("pageTaskEdit.opResult.message"), createPropertyExpression("message")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<OperationResult>>> cellItem, String componentId,
                    IModel<SelectableBean<OperationResult>> rowModel) {
                Label label = new Label(componentId, (IModel<String>) () -> WebComponentUtil.nl2br(rowModel.getObject().getValue().getMessage()));
                label.setEscapeModelStrings(false);
                cellItem.add(label);
            }
        });
        //columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
        return columns;
    }

    private String createPropertyExpression(String propertyName) {
        return SelectableBeanImpl.F_VALUE + "." + propertyName;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(get(ID_OPERATION_RESULT));
    }

    @Override
    protected void detachModel() {
        super.detachModel();
        ((LoadableModel) getModel()).reset();
    }
}
