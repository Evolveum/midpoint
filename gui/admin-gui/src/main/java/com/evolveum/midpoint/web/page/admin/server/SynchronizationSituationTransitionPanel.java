/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;

public class SynchronizationSituationTransitionPanel extends BasePanel<List<SynchronizationSituationTransitionType>> {

    private static final String ID_SYNCHORNIZATION_SITUATIONS_TRANSITIONS = "synchronizationSituationTransitions";
    private static final String ID_SYNCHRONIZATION_SITUATIONS_TRANSITION_TITLE = "syncSituationTranstitionTitle";

    public SynchronizationSituationTransitionPanel(String id, IModel<List<SynchronizationSituationTransitionType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        BoxedTablePanel<SynchronizationSituationTransitionType> transitions = new BoxedTablePanel<>(ID_SYNCHORNIZATION_SITUATIONS_TRANSITIONS, new ListDataProvider(this, getModel()), createColumns()) {

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new HeaderFragment(headerId, ID_SYNCHRONIZATION_SITUATIONS_TRANSITION_TITLE, SynchronizationSituationTransitionPanel.this);
            }

        };
        transitions.setOutputMarkupId(true);
        add(transitions);

    }

    private List<IColumn<SynchronizationSituationTransitionType, String>> createColumns() {
        List<IColumn<SynchronizationSituationTransitionType, String>> columns = new ArrayList<>();
        columns.add(new EnumPropertyColumn<>(createStringResource("SynchronizationSituationTransitionType.onProcessingStart"), SynchronizationSituationTransitionType.F_ON_PROCESSING_START.getLocalPart(), "SynchronizationSituationTransitionType.UNKNOWN"));
        columns.add(new EnumPropertyColumn<>(createStringResource("SynchronizationSituationTransitionType.onSynchronizationStart"), SynchronizationSituationTransitionType.F_ON_SYNCHRONIZATION_START.getLocalPart(), "SynchronizationSituationTransitionType.UNKNOWN"));
        columns.add(new EnumPropertyColumn<>(createStringResource("SynchronizationSituationTransitionType.onSynchronizationEnd"), SynchronizationSituationTransitionType.F_ON_SYNCHRONIZATION_END.getLocalPart(), "SynchronizationSituationTransitionType.UNKNOWN"));
        columns.add(new EnumPropertyColumn<>(createStringResource("SynchronizationSituationTransitionType.exclusionReason"), SynchronizationSituationTransitionType.F_EXCLUSION_REASON.getLocalPart()));
        columns.add(new AbstractColumn<>(createStringResource("SynchronizationSituationTransitionType.countSuccess")) {

            @Override
            public void populateItem(Item<ICellPopulator<SynchronizationSituationTransitionType>> item, String s, IModel<SynchronizationSituationTransitionType> iModel) {
                item.add(new Label(s, getCountFor(iModel.getObject(), ItemProcessingOutcomeType.SUCCESS)));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("SynchronizationSituationTransitionType.countError")) {

            @Override
            public void populateItem(Item<ICellPopulator<SynchronizationSituationTransitionType>> item, String s, IModel<SynchronizationSituationTransitionType> iModel) {
                item.add(new Label(s, getCountFor(iModel.getObject(), ItemProcessingOutcomeType.FAILURE)));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("SynchronizationSituationTransitionType.countSkip")) {

            @Override
            public void populateItem(Item<ICellPopulator<SynchronizationSituationTransitionType>> item, String s, IModel<SynchronizationSituationTransitionType> iModel) {
                item.add(new Label(s, getCountFor(iModel.getObject(), ItemProcessingOutcomeType.SKIP)));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("SynchronizationSituationTransitionType.totalCount")) {

            @Override
            public void populateItem(Item<ICellPopulator<SynchronizationSituationTransitionType>> item, String s, IModel<SynchronizationSituationTransitionType> iModel) {
                item.add(new Label(s, getTotalCount(iModel)));
            }
        });
        return columns;
    }

    private String getCountFor(SynchronizationSituationTransitionType item, ItemProcessingOutcomeType outcome) {
        if (item == null) {
            return "0";
        }

        int count = 0;
        List<OutcomeKeyedCounterType> counters = item.getCounter();
        for (OutcomeKeyedCounterType counter : counters) {
            QualifiedItemProcessingOutcomeType counterOutcome = counter.getOutcome();
            if (counterOutcome == null || outcome != counterOutcome.getOutcome()) {
                continue;
            }
            count += getCount(counter);
        }
        return Integer.toString(count);
    }

    private int getCount(OutcomeKeyedCounterType counter) {
        if (counter == null || counter.getCount() == null) {
            return 0;
        }

        return counter.getCount();
    }

    private int incrementCounter(Integer totalCount, OutcomeKeyedCounterType counter) {
        if (counter == null) {
            return totalCount;
        }

        int counterCount = counter.getCount() == null ? 0 : counter.getCount();
        totalCount += counterCount;
        return totalCount;
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

    class HeaderFragment extends Fragment {

        public HeaderFragment(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);
        }

    }
}
