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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceItemDefinitionType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
class AttributeEditorUtils {

	private static final String ID_MATCHING_RULE = "matchingRule";
	private static final String ID_UNKNOWN_MATCHING_RULE = "unknownMatchingRule";

	private static final Trace LOGGER = TraceManager.getTrace(AttributeEditorUtils.class);

	static void addMatchingRuleFields(final BasePanel<? extends ResourceItemDefinitionType> editor, final NonEmptyModel<Boolean> readOnlyModel) {

		// normalizes unqualified QNames
		final IModel<QName> matchingRuleModel = new IModel<QName>() {
			@Override
			public QName getObject() {
				QName rawRuleName = editor.getModelObject().getMatchingRule();
				if (rawRuleName == null) {
					return null;
				}
				try {
					MatchingRule<?> rule = editor.getPageBase().getMatchingRuleRegistry().getMatchingRule(rawRuleName, null);
					return rule.getName();
				} catch (SchemaException e) {
					// we could get here if invalid QName is specified - but we don't want to throw an exception in that case
					LoggingUtils.logUnexpectedException(LOGGER, "Invalid matching rule name encountered in resource wizard: {} -- continuing", e, rawRuleName);
					return rawRuleName;
				}
			}

			@Override
			public void setObject(QName value) {
				editor.getModelObject().setMatchingRule(value);
			}

			@Override
			public void detach() {
			}
		};


		final List<QName> matchingRuleList = WebComponentUtil.getMatchingRuleList();
		DropDownChoice matchingRule = new DropDownChoice<>(ID_MATCHING_RULE,
				matchingRuleModel, new AbstractReadOnlyModel<List<QName>>() {
			@Override
			public List<QName> getObject() {
				return matchingRuleList;
			}
		}, new QNameChoiceRenderer());
		matchingRule.setNullValid(true);
		matchingRule.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				QName ruleName = matchingRuleModel.getObject();
				return ruleName == null || WebComponentUtil.getMatchingRuleList().contains(ruleName);
			}
			@Override
			public boolean isEnabled() {
				return !readOnlyModel.getObject();
			}
		});
		editor.add(matchingRule);

		Label unknownMatchingRule = new Label(ID_UNKNOWN_MATCHING_RULE, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return editor.getString("ResourceAttributeEditor.label.unknownMatchingRule", matchingRuleModel.getObject());
			}
		});
		unknownMatchingRule.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				QName ruleName = matchingRuleModel.getObject();
				return ruleName != null && !WebComponentUtil.getMatchingRuleList().contains(ruleName);
			}
		});
		editor.add(unknownMatchingRule);

	}

	@NotNull
	public static VisibleEnableBehaviour createShowIfEditingOrOutboundExists(final IModel<? extends ResourceItemDefinitionType> model,
			final NonEmptyModel<Boolean> readOnlyModel) {
		return new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				ResourceItemDefinitionType itemDefinition = model.getObject();
				if (itemDefinition == null) {
					return false;
				}
				return !readOnlyModel.getObject() || itemDefinition.getOutbound() != null;
			}
		};
	}
}
