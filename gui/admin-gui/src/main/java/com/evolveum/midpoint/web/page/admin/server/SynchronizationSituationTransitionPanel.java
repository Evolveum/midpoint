/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutcomeKeyedCounterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationTransitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class SynchronizationSituationTransitionPanel extends BasePanel<SynchronizationSituationTransitionType> {

    private static final String ID_PROCESSING_START = "processingStart";
    private static final String ID_SYNC_START =  "syncStart";
    private static final String ID_SYNC_END = "syncEnd";
    private static final String ID_TOTAL_COUNT = "totalCount";
    private static final String ID_OUTCOME_TABLE = "outcomeTable";
    private static final String ID_COLLAPSE_EXPAND = "collapseExpand";
    private static final String ID_BOX_BODY = "boxBody";
    private static final String ID_BUTTON_ICON = "buttonIcon";

    private boolean isExpanded = false;

    public SynchronizationSituationTransitionPanel(String id, IModel<SynchronizationSituationTransitionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createLabelItem(ID_PROCESSING_START, getModel(), SynchronizationSituationTransitionType.F_ON_PROCESSING_START));
        add(createLabelItem(ID_SYNC_START, getModel(), SynchronizationSituationTransitionType.F_ON_SYNCHRONIZATION_START));
        add(createLabelItem(ID_SYNC_END, getModel(), SynchronizationSituationTransitionType.F_ON_SYNCHRONIZATION_END));
        add(new Label(ID_TOTAL_COUNT, new ReadOnlyModel<>(() -> getTotalCount(getModel()))));

        IModel<List<OutcomeKeyedCounterType>> outcomeCounterModel = new PropertyModel<>(getModel(), SynchronizationSituationTransitionType.F_COUNTER.getLocalPart());
        ListDataProvider<OutcomeKeyedCounterType> outcomeCounterProvider = new ListDataProvider<>(this, outcomeCounterModel);

        WebMarkupContainer boxBody = new WebMarkupContainer(ID_BOX_BODY);
        boxBody.add(new VisibleBehaviour(() -> isExpanded));
        add(boxBody);

        BoxedTablePanel<OutcomeKeyedCounterType> table =
                new BoxedTablePanel<>(ID_OUTCOME_TABLE, outcomeCounterProvider, createOutcomeKeyedContainerColumns()) {
                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }
                };

        table.setOutputMarkupId(true);
        boxBody.add(table);

        AjaxButton collapseExpand = new AjaxButton(ID_COLLAPSE_EXPAND) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                isExpanded = !isExpanded;
                target.add(SynchronizationSituationTransitionPanel.this);
            }
        };
        collapseExpand.setOutputMarkupId(true);
        add(collapseExpand);

        WebMarkupContainer buttonIcon = new WebMarkupContainer(ID_BUTTON_ICON);
        buttonIcon.add(AttributeModifier.replace("class", new ReadOnlyModel<>(() -> isExpanded ? "fa fa-minus" : "fa fa-plus")));
        collapseExpand.add(buttonIcon);
        setOutputMarkupId(true);
    }

    private int incrementCounter(Integer totalCount, OutcomeKeyedCounterType counter) {
        if (counter == null) {
            return totalCount;
        }

        int counterCount = counter.getCount() == null ? 0 : counter.getCount();
        totalCount += counterCount;
        return totalCount;
    }
    private Label createLabelItem(String id, IModel<SynchronizationSituationTransitionType> syncSituationModel, QName stage) {
        Label label = new Label(id, createSituationLabel(syncSituationModel, stage.getLocalPart()));
        label.setRenderBodyOnly(true);
        return label;
    }

    private IModel<String> createSituationLabel(IModel<SynchronizationSituationTransitionType> syncSituationModel, String stage) {
        return new PropertyModel<>(syncSituationModel, stage);
    }


    private String getTotalCount(IModel<SynchronizationSituationTransitionType> syncSituationTrasitionModel) {
        if (syncSituationTrasitionModel == null) {
            return "0";
        }

        SynchronizationSituationTransitionType transition = syncSituationTrasitionModel.getObject();
        if (transition == null) {
            return "0";
        }

        int totalCount = 0;
        for (OutcomeKeyedCounterType counter: transition.getCounter()) {
            totalCount = incrementCounter(totalCount, counter);
        }

        return Integer.toString(totalCount);
    }

    private List<IColumn<OutcomeKeyedCounterType, String>> createOutcomeKeyedContainerColumns() {
        List<IColumn<OutcomeKeyedCounterType, String>> syncColumns = new ArrayList<>();
        syncColumns.add(createQualifiedItemPropertyColumn(QualifiedItemProcessingOutcomeType.F_QUALIFIER_URI));
        syncColumns.add(createQualifiedItemPropertyColumn(QualifiedItemProcessingOutcomeType.F_OUTCOME));
        syncColumns.add(createPropertyColumn());
        return syncColumns;
    }

    private <T> PropertyColumn<T, String> createPropertyColumn() {
        String columnName = OutcomeKeyedCounterType.F_COUNT.getLocalPart();
        return new PropertyColumn<>(createStringResource("OutcomeKeyedCounterType." + columnName), columnName);
    }

    private PropertyColumn<OutcomeKeyedCounterType, String> createQualifiedItemPropertyColumn(QName columnName) {
        String columnNameString = columnName.getLocalPart();
        StringResourceModel columnDisplayName = createStringResource("QualifiedItemProcessingOutcomeType." + columnNameString);
        return new PropertyColumn<>(columnDisplayName, createQualifiedItemExpression(columnNameString));
    }

    private String createQualifiedItemExpression(String itemName) {
        return OutcomeKeyedCounterType.F_OUTCOME.getLocalPart() + "." + itemName;
    }
}
