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

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.WizardIssuesDto;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author mederly
 */
public class WizardIssuesPanel extends BasePanel<WizardIssuesDto> {

	private static final String ID_PANEL = "panel";
	private static final String ID_TITLE = "title";
	private static final String ID_TABLE = "table";
	private static final String ID_ROW = "row";
	private static final String ID_SEVERITY = "severity";
	private static final String ID_TEXT = "text";

	public WizardIssuesPanel(String id, @NotNull NonEmptyModel<WizardIssuesDto> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);
		panel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().hasIssues();
			}
		});
		panel.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				WizardIssuesDto issuesDto = WizardIssuesPanel.this.getModelObject();
				WizardIssuesDto.Severity severity = issuesDto.getSeverity();
				return severity != null ? "box-" + severity.getColorStyle() : null;
			}
		}));
		add(panel);

		Label title = new Label(ID_TITLE, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				WizardIssuesDto issuesDto = WizardIssuesPanel.this.getModelObject();
				WizardIssuesDto.Severity severity = issuesDto.getSeverity();
				if (severity == null) {
					return "";
				} else if (severity == WizardIssuesDto.Severity.INFO) {
					return getString("Wizard.Notes");
				} else {
					return getString("Wizard.Issues");
				}
			}
		});
		panel.add(title);

		WebMarkupContainer table = new WebMarkupContainer(ID_TABLE);
		panel.add(table);

		ListView<WizardIssuesDto.Issue> issues = new ListView<WizardIssuesDto.Issue>(ID_ROW,
            new PropertyModel<>(getModel(), WizardIssuesDto.F_ISSUES)) {
			@Override
			protected void populateItem(ListItem<WizardIssuesDto.Issue> item) {
				WizardIssuesDto.Issue issue = item.getModelObject();
				Label severityLabel = new Label(ID_SEVERITY, "");
				severityLabel.add(AttributeAppender.replace("class", issue.getSeverityClass()));
				item.add(severityLabel);
				item.add(new Label(ID_TEXT, issue.getText()));
			}
		};
		table.add(issues);

	}

}
