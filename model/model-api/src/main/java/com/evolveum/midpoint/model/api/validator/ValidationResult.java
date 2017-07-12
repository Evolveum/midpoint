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

package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationResultType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ValidationResult {

	@NotNull private final List<Issue> issues = new ArrayList<>();

	public boolean hasIssues() {
		return !issues.isEmpty();
	}

	public boolean hasIssuesOfAtLeast(Issue.Severity severity) {
		for (Issue issue : issues) {
			if (issue.hasSeverityAtLeast(severity)) {
				return true;
			}
		}
		return false;
	}

	public void add(@NotNull Issue.Severity severity, @NotNull String category, @NotNull String code, @NotNull String text, @Nullable ObjectReferenceType objectRef, @Nullable ItemPath itemPath) {
		issues.add(new Issue(severity, category, code, text, objectRef, itemPath));
	}

	@NotNull
	public List<Issue> getIssues() {
		return issues;
	}

	public ValidationResultType toValidationResultType() {
		ValidationResultType rv = new ValidationResultType();
		for (Issue issue : issues) {
			rv.getIssue().add(issue.toValidationIssueType());
		}
		return rv;
	}

}
