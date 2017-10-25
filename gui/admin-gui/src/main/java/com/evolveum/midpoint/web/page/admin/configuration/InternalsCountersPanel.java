package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.Arrays;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

public class InternalsCountersPanel extends BasePanel<ListView<InternalCounters>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_COUNTERS_TABLE = "countersTable";
	private static final String ID_COUNTER_LABEL = "counterLabel";
	private static final String ID_COUNTER_VALUE = "counterValue";

	public InternalsCountersPanel(String id) {
		super(id);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		setOutputMarkupId(true);
		
		ListView<InternalCounters> countersTable = new ListView<InternalCounters>(ID_COUNTERS_TABLE,
				Arrays.asList(InternalCounters.values())) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<InternalCounters> item) {
				InternalCounters counter = item.getModelObject();
				Label label = new Label(ID_COUNTER_LABEL, createStringResource("InternalCounters." + counter.getKey()));
				item.add(label);

				Label valueLabel = new Label(ID_COUNTER_VALUE, new AbstractReadOnlyModel<String>() {
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
}
