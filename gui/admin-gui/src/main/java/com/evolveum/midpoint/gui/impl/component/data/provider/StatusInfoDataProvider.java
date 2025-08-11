/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Iterator;
import java.util.List;

/**
 * Data provider for {@link StatusInfo} objects containing {@link ObjectTypesSuggestionType}.
 * Supports optional case-insensitive filtering.
 */
public class StatusInfoDataProvider
        extends ListDataProvider<ObjectTypeSuggestionType> {

    public StatusInfoDataProvider(Component component,
            IModel<List<ObjectTypeSuggestionType>> model) {
        super(component, model);
    }

    @Override
    public Iterator<? extends ObjectTypeSuggestionType> internalIterator(long first, long count) {
        return super.internalIterator(first, count);
    }
}
