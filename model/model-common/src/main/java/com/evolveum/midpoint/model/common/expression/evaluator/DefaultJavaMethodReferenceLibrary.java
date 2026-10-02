/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

/**
 * Temporary class to hold the default Java method reference library.
 *
 * This is a placeholder for the actual implementation that will be provided via MEL in the future.
 *
 * For Evolveum internal use only!
 *
 * NOTE: Do not change parameter names! They are bound to variable names at the place of use.
 */
@SuppressWarnings("unused") // called from the outside
@NullMarked
@Experimental
@Deprecated
public class DefaultJavaMethodReferenceLibrary {

    private static final String REPORT_UTILS_CLASS_NAME = "com.evolveum.midpoint.report.impl.ReportUtils";

    /**
     * Used by {@code 100-report-reconciliation}.
     */
    public static @Nullable QName qualifyObjectClassName(@Nullable String objectClass) {
        return objectClass != null ? new QName(SchemaConstants.NS_RI, objectClass) : null;
    }

    /**
     * Used by {@code 140-report-certification-campaigns}.
     */
    public static String certificationCasesDecidedPercentageAllStagesAllIterations(AccessCertificationCampaignType input) {
        return CertCampaignTypeUtil.getCasesDecidedPercentageAllStagesAllIterations(input) + " %";
    }

    /**
     * Formats an audit delta for report output, preserving support for unknown item types.
     *
     * Used by {@code 270-object-collection-audit}.
     */
    public static List<String> formatAuditDelta(List<ObjectDeltaOperationType> input) throws SchemaException {

        var formatted = new ArrayList<String>(input.size());

        for (ObjectDeltaOperationType deltaType : input) {
            var delta = DeltaConvertor.createObjectDeltaOperation(deltaType, true);
            formatted.add(printAuditDelta(delta));
        }

        return formatted;
    }

    private static String printAuditDelta(ObjectDeltaOperation<?> delta) {
        try {
            Class<?> reportUtils = Class.forName(REPORT_UTILS_CLASS_NAME);
            return (String) reportUtils
                    .getMethod("printDelta", ObjectDeltaOperation.class)
                    .invoke(null, delta);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Couldn't format audit delta", cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't access the report delta formatter", e);
        }
    }
}
