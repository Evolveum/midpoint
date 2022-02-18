/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationResultType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
