/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prints provisioning statistics.
 */
public class ProvisioningStatisticsPrinter extends AbstractStatisticsPrinter<ProvisioningStatisticsType> {

    public ProvisioningStatisticsPrinter(ProvisioningStatisticsType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        createData();
        createFormatting();

        return applyFormatting() + "\n";
    }

    private void createData() {
        initData();

        List<ProvisioningStatisticsEntryType> entries = new ArrayList<>(information.getEntry());
        entries.sort(ProvisioningStatisticsTypeUtil.createEntryComparator());

        for (ProvisioningStatisticsEntryType entry : entries) {
            List<ProvisioningStatisticsOperationEntryType> operations = new ArrayList<>(entry.getOperation());
            operations.sort(ProvisioningStatisticsTypeUtil.createOperationComparator());

            for (ProvisioningStatisticsOperationEntryType operation : operations) {
                Data.Record record = data.createRecord();
                record.add(ProvisioningStatisticsTypeUtil.getResourceName(entry));
                record.add(ProvisioningStatisticsTypeUtil.getObjectClassLocalName(entry));
                record.add(operation.getOperation());
                record.add(operation.getStatus());
                record.add(operation.getCount());
                record.add(operation.getTotalTime());
                record.add(operation.getMinTime());
                record.add(operation.getMaxTime());
                record.add(avg(operation.getTotalTime(), operation.getCount()));
            }
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Resource", LEFT, formatString());
        addColumn("Object class", LEFT, formatString());
        addColumn("Operation", LEFT, formatString());
        addColumn("Status", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        addColumn("Total time (ms)", RIGHT, formatInt());
        addColumn("Min", RIGHT, formatInt());
        addColumn("Max", RIGHT, formatInt());
        addColumn("Avg", RIGHT, formatFloat1());
    }
}
