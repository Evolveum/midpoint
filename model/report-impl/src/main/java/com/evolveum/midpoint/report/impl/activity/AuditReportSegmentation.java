/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Helps with the segmentation of audit reports.
 *
 * Pulled from {@link ReportDataCreationActivityExecution} into a separate class because:
 *
 * 1. needs to keep the state (from-to timestamps and generated segmentation),
 * 2. is specific only for audit records.
 */
class AuditReportSegmentation {

    private static final Trace LOGGER = TraceManager.getTrace(AuditReportSegmentation.class);

    @NotNull private final XMLGregorianCalendar reportFrom;
    @NotNull private final XMLGregorianCalendar reportTo;

    private ExplicitWorkSegmentationType explicitSegmentation;

    AuditReportSegmentation(@NotNull XMLGregorianCalendar reportFrom, @NotNull XMLGregorianCalendar reportTo) {
        this.reportFrom = reportFrom;
        this.reportTo = reportTo;
    }

    /**
     * This method converts implicit segmentation (containing typically the number of buckets, maybe with the discriminator)
     * into full segmentation specification (discriminator, segmentation type, start/end value, and so on).
     *
     * Currently, only the number of buckets is supported in the implicit segmentation. Discriminator and matching rule
     * are not allowed here.
     *
     * We return ExplicitWorkSegmentationType, because dateTime-based buckets are not available now.
     * The disadvantage is that the list of buckets is re-created at each bucket acquisition, so if it's
     * large, the performance will suffer.
     */
    AbstractWorkSegmentationType resolveImplicitSegmentation(ImplicitWorkSegmentationType segmentation) {
        if (explicitSegmentation != null) {
            return explicitSegmentation;
        }

        argCheck(segmentation.getDiscriminator() == null, "Discriminator specification is not supported");
        argCheck(segmentation.getMatchingRule() == null, "Matching rule specification is not supported");
        argCheck(segmentation.getNumberOfBuckets() != null, "Number of buckets must be specified");

        long reportFromMillis = XmlTypeConverter.toMillis(reportFrom);
        long reportToMillis = XmlTypeConverter.toMillis(reportTo);
        long step = (reportToMillis - reportFromMillis) / segmentation.getNumberOfBuckets();

        LOGGER.trace("Creating segmentation: from = {}, to = {}, step = {}",
                reportFrom, reportTo, step);

        explicitSegmentation = new ExplicitWorkSegmentationType(PrismContext.get());
        for (long bucketFromMillis = reportFromMillis; bucketFromMillis < reportToMillis; bucketFromMillis += step) {
            explicitSegmentation.getContent().add(
                    createBucketContent(bucketFromMillis, step, reportToMillis));
        }
        return explicitSegmentation;
    }

    /**
     * Creates a single bucket, given the start of the bucket (`bucketFromMillis`), step (`step`), and
     * the global end (`reportToMillis`).
     *
     * Note that start of the interval is inclusive, whereas the end is exclusive.
     */
    private AbstractWorkBucketContentType createBucketContent(long bucketFromMillis, long step, long reportToMillis) {
        XMLGregorianCalendar bucketFrom = XmlTypeConverter.createXMLGregorianCalendar(bucketFromMillis);
        XMLGregorianCalendar bucketTo;
        if (bucketFromMillis + step < reportToMillis) {
            bucketTo = XmlTypeConverter.createXMLGregorianCalendar(bucketFromMillis + step);
        } else {
            bucketTo = null;
        }

        ObjectFilter filter;
        if (bucketTo != null) {
            filter = PrismContext.get().queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TIMESTAMP).ge(bucketFrom) // inclusive
                    .and().item(AuditEventRecordType.F_TIMESTAMP).lt(bucketTo) // exclusive
                    .buildFilter();
        } else {
            filter = PrismContext.get().queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TIMESTAMP).ge(bucketFrom)
                    .buildFilter();
        }
        SearchFilterType filterBean;
        try {
            filterBean = PrismContext.get().getQueryConverter().createSearchFilterType(filter);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while converting bucket filter: " + e.getMessage(), e);
        }

        return new FilterWorkBucketContentType()
                .filter(filterBean);
    }
}
