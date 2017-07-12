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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * @author mederly
 */
public class WizardIssuesDto implements Serializable {

	public static final String F_ISSUES = "issues";

	private static final Map<String, Class<? extends WizardStep>> STEPS = new LinkedHashMap<>();
	static {
		STEPS.put(ResourceValidator.CAT_BASIC, NameStep.class);
		STEPS.put(ResourceValidator.CAT_CONFIGURATION, ConfigurationStep.class);
		STEPS.put(ResourceValidator.CAT_SCHEMA_HANDLING, SchemaHandlingStep.class);
		STEPS.put(ResourceValidator.CAT_SYNCHRONIZATION, SynchronizationStep.class);
		STEPS.put(ResourceValidator.CAT_CAPABILITIES, CapabilityStep.class);
	}

	@NotNull private final List<Issue> issues = new ArrayList<>();

	public boolean hasIssues() {
		return !issues.isEmpty();
	}

	public boolean hasErrors() {
		for (Issue issue : issues) {
			if (issue.severity == Severity.ERROR) {
				return true;
			}
		}
		return false;
	}

	public Severity getSeverity() {
		Severity max = null;
		for (Issue issue : issues) {
			if (max == null || issue.severity.ordinal() < max.ordinal()) {
				max = issue.severity;
			}
		}
		return max;
	}

	public void add(@NotNull Severity severity, @NotNull String text, @Nullable Class<? extends WizardStep> relatedStep) {
		issues.add(new Issue(severity, text, relatedStep));
	}

	public boolean hasErrorsFor(Class<? extends WizardStep> stepClass) {
		for (Issue issue : issues) {
			if (issue.severity == Severity.ERROR && issue.isRelatedTo(stepClass)) {
				return true;
			}
		}
		return false;
	}

	public void sortIssues() {
		Collections.sort(issues, new Comparator<Issue>() {
			@Override
			public int compare(Issue o1, Issue o2) {
				int severity = Integer.compare(o1.severity.ordinal(), o2.severity.ordinal());
				if (severity != 0) {
					return severity;
				}
				return Integer.compare(o1.getStepNumber(), o2.getStepNumber());
			}
		});
	}

	public void fillFrom(@NotNull ValidationResult validationResult) {
		for (com.evolveum.midpoint.model.api.validator.Issue issue : validationResult.getIssues()) {
			add(Severity.fromModel(issue.getSeverity()), issue.getText(), STEPS.get(issue.getCategory()));
		}
	}

	public enum Severity {
		ERROR("fa fa-fw fa-exclamation-circle text-danger", "danger"),
		WARNING("fa fa-fw fa-exclamation-triangle text-warning", "warning"),
		INFO("fa fa-fw fa-info-circle text-primary", "primary");

		private String icon;
		private String colorStyle;

		Severity(String icon, String colorStyle) {
			this.icon = icon;
			this.colorStyle = colorStyle;
		}

		public String getIcon() {
			return icon;
		}

		public String getColorStyle() {
			return colorStyle;
		}

		@NotNull
		static Severity fromModel(@NotNull com.evolveum.midpoint.model.api.validator.Issue.Severity s) {
			switch (s) {
				case ERROR: return ERROR;
				case WARNING: return WARNING;
				case INFO: return INFO;
			}
			throw new IllegalArgumentException(String.valueOf(s));
		}
	}

	public class Issue implements Serializable {
		@NotNull private final Severity severity;
		@NotNull private final String text;
		@Nullable private final Class<? extends WizardStep> relatedStep;

		Issue(@NotNull Severity severity, @NotNull String text, @Nullable Class<? extends WizardStep> relatedStep) {
			this.severity = severity;
			this.text = text;
			this.relatedStep = relatedStep;
		}

		@NotNull
		public String getSeverityClass() {
			return severity.getIcon();
		}

		@NotNull
		public String getText() {
			return text;
		}

		boolean isRelatedTo(Class<? extends WizardStep> stepClass) {
			return relatedStep != null && stepClass.isAssignableFrom(relatedStep);
		}

		int getStepNumber() {
			if (relatedStep == null) {
				return 0;
			}
			int i = 1;
			Iterator<Class<? extends WizardStep>> iterator = STEPS.values().iterator();
			while (iterator.hasNext() && iterator.next() != relatedStep) {
				i++;
			}
			return i;
		}
	}
}
