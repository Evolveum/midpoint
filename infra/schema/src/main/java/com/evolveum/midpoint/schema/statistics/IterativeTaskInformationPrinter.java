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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prints iterative task performance information.
 */
public class IterativeTaskInformationPrinter extends AbstractStatisticsPrinter<IterativeTaskInformationType> {

    public IterativeTaskInformationPrinter(@NotNull IterativeTaskInformationType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        createData();
        createFormatting();

        return applyFormatting() + "\n";
    }

    private void createData() {
        initData();
        information.getPart().forEach(this::createData);
    }

    private void createData(IterativeTaskPartItemsProcessingInformationType component) {
        List<ProcessedItemSetType> processed = new ArrayList<>(component.getProcessed());
        processed.sort(OutcomeKeyedCounterTypeUtil.createOutcomeKeyedCounterComparator());

        for (ProcessedItemSetType set : processed) {
            Data.Record record = data.createRecord();
            record.add(component.getPartUri());
            record.add(OutcomeKeyedCounterTypeUtil.getOutcome(set));
            record.add(OutcomeKeyedCounterTypeUtil.getOutcomeQualifierUri(set));
            record.add(set.getCount());
            record.add(set.getDuration());
            record.add(div(set.getDuration(), set.getCount()));
            if (set.getLastItem() != null) {
                record.add(getItemDescription(set.getLastItem()));
                record.add(XmlTypeConverter.toDate(set.getLastItem().getStartTimestamp()));
                record.add(XmlTypeConverter.toDate(set.getLastItem().getEndTimestamp()));
                record.add(getDuration(set.getLastItem()));
            } else {
                record.add(null);
                record.add(null);
                record.add(null);
                record.add(null);
            }
        }

        // TODO current
    }

    private Long getDuration(ProcessedItemType item) {
        if (item != null && item.getStartTimestamp() != null && item.getEndTimestamp() != null) {
            return XmlTypeConverter.toMillis(item.getEndTimestamp()) - XmlTypeConverter.toMillis(item.getStartTimestamp());
        } else {
            return null;
        }
    }

    private String getItemDescription(ProcessedItemType item) {
        if (item != null) {
            return String.format("%s:%s (%s, %s)", QNameUtil.getLocalPart(item.getType()),
                    item.getName(), item.getDisplayName(), item.getOid());
        } else {
            return null;
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Part", LEFT, formatString());
        addColumn("Outcome", LEFT, formatString());
        addColumn("Qualifier", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        addColumn("Total time (ms)", RIGHT, formatInt());
        addColumn("Per object", RIGHT, formatFloat1());
        addColumn("Current/last object", LEFT, formatString());
        addColumn("Started on", LEFT, formatString());
        addColumn("Finished on", LEFT, formatString());
        addColumn("Finished in (ms)", RIGHT, formatInt());
    }
}
