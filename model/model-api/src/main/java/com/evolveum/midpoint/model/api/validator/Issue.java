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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationIssueSeverityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationIssueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public class Issue implements Serializable {

	public enum Severity {	// ordered from most to least severe
		ERROR, WARNING, INFO;

		public boolean isAtLeast(@NotNull Severity other) {
			return ordinal() <= other.ordinal();
		}

		public ValidationIssueSeverityType toSeverityType() {
			switch (this) {
				case ERROR: return ValidationIssueSeverityType.ERROR;
				case WARNING: return ValidationIssueSeverityType.WARNING;
				case INFO: return ValidationIssueSeverityType.INFO;
				default: throw new AssertionError("Invalid issue value: " + this);
			}
		}
	}

	@NotNull private final Severity severity;
	@NotNull private final String category;
	@NotNull private final String code;
	@NotNull private final String text;
	@Nullable private final ObjectReferenceType objectRef;
	@Nullable private final ItemPath itemPath;

	public Issue(@NotNull Severity severity, @NotNull String category, @NotNull String code, @NotNull String text, ObjectReferenceType objectRef,
			ItemPath itemPath) {
		this.severity = severity;
		this.category = category;
		this.code = code;
		this.text = text;
		this.objectRef = objectRef;
		this.itemPath = itemPath;
	}

	@NotNull
	public Severity getSeverity() {
		return severity;
	}

	@NotNull
	public String getCategory() {
		return category;
	}

	@NotNull
	public String getCode() {
		return code;
	}

	@NotNull
	public String getText() {
		return text;
	}

	@Nullable
	public ObjectReferenceType getObjectRef() {
		return objectRef;
	}

	@Nullable
	public ItemPath getItemPath() {
		return itemPath;
	}

	public boolean hasSeverityAtLeast(@NotNull Severity severity) {
		return this.severity.isAtLeast(severity);
	}

	@Nullable
	public static Severity getSeverity(List<Issue> issues) {
		Severity max = null;
		for (Issue issue : issues) {
			if (max == null || issue.getSeverity().isAtLeast(max)) {
				max = issue.getSeverity();
			}
		}
		return max;
	}

	public ValidationIssueType toValidationIssueType() {
		ValidationIssueType rv = new ValidationIssueType();
		rv.setSeverity(severity.toSeverityType());
		rv.setCategory(category);
		rv.setCode(code);
		rv.setText(text);
		rv.setObjectRef(objectRef);
		if (itemPath != null) {
			rv.setItemPath(itemPath.toString());
		}
		return rv;
	}
}
