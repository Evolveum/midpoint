/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api.info;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Ugly hack to print {@link ObjectClassInfo} objects in a table format.
 *
 * (It is a hack because this information is not really statistics, but it is useful to print it in a table format.
 * Maybe we should generalize {@link AbstractStatisticsPrinter} to be able to print any list of objects in a table format.)
 */
public class ObjectClassInfoPrinter extends AbstractStatisticsPrinter<List<ObjectClassInfo>> {

    public ObjectClassInfoPrinter(@NotNull List<ObjectClassInfo> information, Options options) {
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

    private void createData(List<ObjectClassInfo> infos) {
        for (var info : infos) {
            Data.Record record = data.createRecord();
            record.add(info.getObjectClassName().getLocalPart());
            record.add("%d (%s)".formatted(info.sizeEstimation().getValue(), info.sizeEstimation().getPrecision().value()));
            var stats = info.statistics();
            if (stats != null) {
                record.add("%s object(s) @ %s".formatted(stats.getSize(), stats.getTimestamp()));
            } else {
                record.add("");
            }
            record.add(
                    info.objectTypes().stream()
                            .map(t -> t.getTypeIdentification().toString())
                            .collect(Collectors.joining(", ")));
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Object class", LEFT, formatString());
        addColumn("Estimated size", LEFT, formatString());
        addColumn("Latest statistics", LEFT, formatString());
        addColumn("Defined types", LEFT, formatString());
    }
}
