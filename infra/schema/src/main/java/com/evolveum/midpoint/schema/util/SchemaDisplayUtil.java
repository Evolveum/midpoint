/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

/**
 * Utility methods for converting schema values to display-friendly representations.
 */
public class SchemaDisplayUtil {

    public static String formatReference(@Nullable Referencable reference) {
        return formatReference(reference, true);
    }

    public static String formatReference(@Nullable Referencable prv, boolean showType) {
        if (prv == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (showType || prv.getTargetName() == null) {
            sb.append(getTypeDisplayName(prv.getType()));
            sb.append(": ");
        }
        if (prv.getTargetName() != null) {
            sb.append(prv.getTargetName());
        } else {
            sb.append(prv.getOid());
        }
        return sb.toString();
    }

    public static String formatCertificationOutcome(@Nullable String uri, boolean noResponseIfEmpty) {
        return formatCertificationOutcome(certificationOutcomeFromUri(uri), noResponseIfEmpty);
    }

    public static String formatCertificationOutcome(@Nullable AccessCertificationResponseType response, boolean noResponseIfEmpty) {
        if (noResponseIfEmpty) {
            if (response == null) {
                response = AccessCertificationResponseType.NO_RESPONSE;
            }
        } else {
            if (response == null || response == AccessCertificationResponseType.NO_RESPONSE) {
                return "";
            }
        }
        return getPropertyString("AccessCertificationResponseType." + response.name());
    }

    public static @Nullable AccessCertificationResponseType certificationOutcomeFromUri(@Nullable String uri) {
        if (uri == null) {
            return null;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT)) {
            return AccessCertificationResponseType.ACCEPT;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REVOKE)) {
            return AccessCertificationResponseType.REVOKE;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE)) {
            return AccessCertificationResponseType.REDUCE;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED)) {
            return AccessCertificationResponseType.NOT_DECIDED;
        } else if (QNameUtil.matchUri(uri, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)) {
            return AccessCertificationResponseType.NO_RESPONSE;
        } else {
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }
    }

    public static String getTypeDisplayName(@Nullable QName typeName) {
        if (typeName == null) {
            return null;
        }
        return getPropertyString(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + typeName.getLocalPart(), typeName.getLocalPart());
    }

    public static String getPropertyString(String key) {
        return getPropertyString(key, null);
    }

    public static String getPropertyString(String key, @Nullable String defaultValue) {
        String val = (defaultValue == null) ? key : defaultValue;
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("localization/schema", new Locale("en", "US"));
        } catch (MissingResourceException e) {
            return (defaultValue != null) ? defaultValue : key; //workaround for reports
        }
        if (bundle != null && bundle.containsKey(key)) {
            val = bundle.getString(key);
        }
        return val;
    }
}
