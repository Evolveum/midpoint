/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api.info;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.Data;

/**
 * Ugly hack to print {@link StatusInfo} objects of {@link ObjectTypesSuggestionType} kind in a table format.
 *
 * @see ObjectClassInfoPrinter
 */
public class ObjectTypesSuggestionStatusInfoPrinter extends AbstractStatisticsPrinter<List<StatusInfo<ObjectTypesSuggestionType>>> {

    public ObjectTypesSuggestionStatusInfoPrinter(@NotNull List<StatusInfo<ObjectTypesSuggestionType>> information, Options options) {
        super(information, options, null, null);
    }

    @Override
    public void prepare() {
        createData();
        createFormatting();
    }

    private void createData() {
        initData();
        createData(information);
    }

    private void createData(List<StatusInfo<ObjectTypesSuggestionType>> infos) {
        for (var info : infos) {
            Data.Record record = data.createRecord();
            record.add(info.token());
            record.add(info.getObjectClassNameLocalPart());
            record.add(info.status());
            record.add(info.started());
            record.add(info.finished());
            var suggestionsBean = info.result();
            record.add(suggestionsBean != null ? suggestionsBean.getObjectType().size() : null);
            record.add(info.message() != null ? info.message().getFallbackMessage() : null); // FIXME this hack with fallback
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Token", LEFT, formatString());
        addColumn("Object class", LEFT, formatString());
        addColumn("Status", LEFT, formatString());
        addColumn("Started", LEFT, formatString());
        addColumn("Finished", LEFT, formatString());
        addColumn("Suggestions", RIGHT, formatInt());
        addColumn("Message", LEFT, formatString());
    }
}
