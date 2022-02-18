/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

public class Issue implements Serializable {

    public enum Severity {    // ordered from most to least severe
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
