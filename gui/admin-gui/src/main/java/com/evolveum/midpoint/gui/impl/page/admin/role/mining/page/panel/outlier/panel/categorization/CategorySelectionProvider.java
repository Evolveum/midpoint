/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisObjectCategorizationType;

import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public class CategorySelectionProvider extends ChoiceProvider<RoleAnalysisObjectCategorizationType> {
    @Serial private static final long serialVersionUID = 1L;

    public CategorySelectionProvider() {

    }

    @Override
    public String getDisplayValue(RoleAnalysisObjectCategorizationType value) {
        return getIdValue(value);
    }

    @Override
    public String getIdValue(RoleAnalysisObjectCategorizationType value) {
        return value.toString();
    }

    @Override
    public void query(String text, int page, Response<RoleAnalysisObjectCategorizationType> response) {
        if(text == null) {
            response.addAll(List.of(RoleAnalysisObjectCategorizationType.values()));
            return;
        }
        RoleAnalysisObjectCategorizationType[] values = RoleAnalysisObjectCategorizationType.values();
        for (RoleAnalysisObjectCategorizationType value : values) {
            if (value.toString().toLowerCase().contains(text)) {
                response.add(value);
            }
        }
    }

    @Override
    public Collection<RoleAnalysisObjectCategorizationType> toChoices(Collection<String> values) {
        List<RoleAnalysisObjectCategorizationType> choices = new ArrayList<>();
        values.stream().map(RoleAnalysisObjectCategorizationType::valueOf).collect(Collectors.toCollection(() -> choices));
        return choices;
    }
}
