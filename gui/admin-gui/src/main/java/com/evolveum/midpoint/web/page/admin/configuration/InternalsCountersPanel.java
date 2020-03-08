/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.repo.api.CounterSpecification;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

public class InternalsCountersPanel extends BasePanel<ListView<InternalCounters>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_COUNTERS_TABLE = "countersTable";
    private static final String ID_COUNTER_LABEL = "counterLabel";
    private static final String ID_COUNTER_VALUE = "counterValue";
    private static final String ID_THRESHOLD_COUNTER = "thresholdCounter";
    private static final String ID_THRESHOLD_COUNTERS_TABLE = "thresholdCountersTable";
    private static final String ID_COUNTER_TASK_LABEL = "counterTask";
    private static final String ID_COUNTER_POLICY_RULE_LABEL = "counterPolicyRule";
    private static final String ID_COUNTER_COUNT_LABEL = "counterCount";
    private static final String ID_RESET_THRESHOLD_COUNTER = "resetThresholdCounter";


    public InternalsCountersPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        Label thresholdCounter = new Label(ID_THRESHOLD_COUNTER, createStringResource("InternalsCountersPanel.thresholds"));
        add(thresholdCounter);

        ListView<CounterSpecification> thresholdCountersTable = new ListView<CounterSpecification>(ID_THRESHOLD_COUNTERS_TABLE, createThresholdCounterModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<CounterSpecification> item) {
                CounterSpecification counter = item.getModelObject();
                Label task = new Label(ID_COUNTER_TASK_LABEL, counter.getTaskName());
                item.add(task);

                Label policyRule = new Label(ID_COUNTER_POLICY_RULE_LABEL, counter.getPolicyRuleName());
                item.add(policyRule);

                Label count = new Label(ID_COUNTER_COUNT_LABEL, counter.getCount());
                item.add(count);

                AjaxLink<Void> resetCounter = new AjaxLink<Void>(ID_RESET_THRESHOLD_COUNTER) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ConfirmationPanel confirmPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createStringResource("InternalsCountersPanel.reset.confirm.message", counter.getTaskName(), counter.getPolicyRuleName())) {

                            private static final long serialVersionUID = 1L;

                            public void yesPerformed(AjaxRequestTarget target) {
                                getPageBase().getCounterManager().removeCounter(counter);
                                target.add(InternalsCountersPanel.this);
                            }
                        };
                        getPageBase().showMainPopup(confirmPanel, target);
                        target.add(InternalsCountersPanel.this);
                    }
                };
                item.add(resetCounter);
            }

        };
        add(thresholdCountersTable);

        ListView<InternalCounters> countersTable = new ListView<InternalCounters>(ID_COUNTERS_TABLE,
                Arrays.asList(InternalCounters.values())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InternalCounters> item) {
                InternalCounters counter = item.getModelObject();
                Label label = new Label(ID_COUNTER_LABEL, createStringResource("InternalCounters." + counter.getKey()));
                item.add(label);

                Label valueLabel = new Label(ID_COUNTER_VALUE, new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        long val = InternalMonitor.getCount(counter);
                        return Long.toString(val);
                    }
                });
                item.add(valueLabel);
            }

        };
        add(countersTable);
    }

    private IModel<List<CounterSpecification>> createThresholdCounterModel() {
        return new IModel<List<CounterSpecification>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<CounterSpecification> getObject() {
                Collection<CounterSpecification> thresholdCounters = getPageBase().getCounterManager().listCounters();
                return new ArrayList<>(thresholdCounters);
            }
        };
    }

}
