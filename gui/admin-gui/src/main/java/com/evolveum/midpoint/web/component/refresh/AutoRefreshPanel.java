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

package com.evolveum.midpoint.web.component.refresh;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Provides simple "auto refresh" panel: buttons for start/stop auto refreshing, requesting manual refresh, and status label.
 *
 * @see Refreshable
 *
 * @author mederly
 */
public class AutoRefreshPanel extends BasePanel<AutoRefreshDto> {

	private static final String ID_REFRESH_NOW = "refreshNow";
	private static final String ID_START = "start";
	private static final String ID_PAUSE = "pause";
	private static final String ID_STATUS = "status";

	public AutoRefreshPanel(String id, IModel<AutoRefreshDto> model, Refreshable refreshable, boolean inSummaryPanel) {
		super(id, model);
		initLayout(refreshable, inSummaryPanel);
	}

	private void initLayout(final Refreshable refreshable, boolean inSummaryPanel) {

		final LinkIconPanel refreshNow = new LinkIconPanel(ID_REFRESH_NOW, new Model("fa fa-refresh"), createStringResource("autoRefreshPanel.refreshNow")) {
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				refreshable.refresh(target);
			}
		};
		refreshNow.setRenderBodyOnly(true);
		add(refreshNow);

		final LinkIconPanel resumeRefreshing = new LinkIconPanel(ID_START, new Model("fa fa-play"), createStringResource("autoRefreshPanel.resumeRefreshing")) {
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				getModelObject().setEnabled(true);
				refreshable.refresh(target);
				startRefreshing(refreshable, target);
			}
		};
		resumeRefreshing.setRenderBodyOnly(true);
		resumeRefreshing.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !getModelObject().isEnabled();
			}
		});
		add(resumeRefreshing);

		final LinkIconPanel pauseRefreshing = new LinkIconPanel(ID_PAUSE, new Model("fa fa-pause"), createStringResource("autoRefreshPanel.pauseRefreshing")) {
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				getModelObject().setEnabled(false);
				refreshable.refresh(target);
				stopRefreshing(refreshable, target);
			}
		};
		pauseRefreshing.setRenderBodyOnly(true);
		pauseRefreshing.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().isEnabled();
			}
		});
		add(pauseRefreshing);

		final Label status = new Label(ID_STATUS, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				AutoRefreshDto dto = getModelObject();
				if (dto.isEnabled()) {
					return createStringResource("autoRefreshPanel.refreshingEach", dto.getInterval() / 1000).getString();
				} else {
					return createStringResource("autoRefreshPanel.noRefreshing").getString();
				}
			}
		});
		status.setRenderBodyOnly(true);
		add(status);

		if (inSummaryPanel) {
			add(new AttributeModifier("class", "summary-tag"));
		}
	}

	public void startRefreshing(final Refreshable refreshable, AjaxRequestTarget target) {
		stopRefreshing(refreshable, target);
		int refreshInterval = refreshable.getRefreshInterval();
		getModel().getObject().setInterval(refreshInterval);

		Component parent = refreshable.getRefreshingBehaviorParent();
		RemovableAjaxTimerBehavior refreshingBehavior = new RemovableAjaxTimerBehavior(parent, refreshInterval) {
			@Override
			protected void handleOnTimer(AjaxRequestTarget target1) {
				refreshable.refresh(target1);
			}
		};
		parent.add(refreshingBehavior);
	}

	public void stopRefreshing(Refreshable refreshable, AjaxRequestTarget target) {
		Component refreshingBehaviorParent = refreshable.getRefreshingBehaviorParent();
		for (Behavior behavior : refreshingBehaviorParent.getBehaviors()) {
			if (behavior instanceof RemovableAjaxTimerBehavior) {
				((RemovableAjaxTimerBehavior) behavior).remove(target);
			}
		}
	}

}
