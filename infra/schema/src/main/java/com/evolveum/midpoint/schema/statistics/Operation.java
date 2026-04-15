/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Operation being recorded: represents an object to which the client reports the end of the operation.
 * It is called simply {@link Operation} to avoid confusing the clients.
 */
public interface Operation {

    default void succeeded() {
        done(ItemProcessingOutcomeType.SUCCESS, null);
    }

    default void skipped() {
        done(ItemProcessingOutcomeType.SKIP, null);
    }

    default void failed(Throwable t) {
        done(ItemProcessingOutcomeType.FAILURE, t);
    }

    default void done(ItemProcessingOutcomeType outcome, Throwable exception) {
        QualifiedItemProcessingOutcomeType qualifiedOutcome =
                new QualifiedItemProcessingOutcomeType()
                        .outcome(outcome);
        done(qualifiedOutcome, exception);
    }

    void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception);

    double getDurationRounded();

    long getEndTimeMillis();

    /** Returns the item characterization for this operation. */
    @NotNull IterationItemInformation getIterationItemInformation();

    /** Returns start info for this operation. */
    @NotNull IterativeOperationStartInfo getStartInfo();

    default @Nullable XMLGregorianCalendar getEndTimestamp() {
        return getEndTimeMillis() != 0 ?
                XmlTypeConverter.createXMLGregorianCalendar(getEndTimeMillis()) : null;
    }
}
