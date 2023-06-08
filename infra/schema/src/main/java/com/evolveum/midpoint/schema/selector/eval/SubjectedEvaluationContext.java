/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/** When evaluation subjected clauses, we have to know who the subject is. */
public interface SubjectedEvaluationContext {

    @Nullable String getPrincipalOid();

    @Nullable FocusType getPrincipalFocus();

    @NotNull Collection<String> getSelfOids();

    @NotNull Collection<String> getSelfOids(@Nullable Delegation delegation);

    enum Delegation {

        ASSIGNEE,
        REQUESTOR,
        RELATED_OBJECT

    }
}
