/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * TEMPORARY
 * =========
 *
 * @author mederly
 */
public class OutcomeUtils {

    public static String toUri(AccessCertificationResponseType response) {
        if (response == null) {
            return null;
        }
        switch (response) {
            case ACCEPT: return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT;
            case REVOKE: return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REVOKE;
            case REDUCE: return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE;
            case NOT_DECIDED: return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED;
            case NO_RESPONSE: return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE;
            default: throw new AssertionError("Unexpected response: " + response);
        }
    }

    @NotNull
    public static AccessCertificationResponseType normalizeToNonNull(AccessCertificationResponseType value) {
        return defaultIfNull(value, NO_RESPONSE);
    }

    @NotNull
    public static String normalizeToNonNull(String value) {
        return defaultIfNull(value, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE);
    }

    @Nullable
    public static AccessCertificationResponseType normalizeToNull(AccessCertificationResponseType value) {
        return value != NO_RESPONSE ? value : null;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String normalizeToNull(String value) {
        return SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE.equals(value) ? null : value;
    }

    public static AccessCertificationResponseType fromUri(String uri) {
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

    @SuppressWarnings("unused")
    public static List<AccessCertificationResponseType> fromUris(List<String> uris) {
        return uris.stream()
                .map(uri -> fromUri(uri))
                .collect(Collectors.toList());
    }

    public static boolean isNoneOrNotDecided(String outcome) {
        if (outcome == null) {
            return true;
        }
        QName outcomeQName = QNameUtil.uriToQName(outcome);

        return QNameUtil.match(outcomeQName, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE_QNAME)
                || QNameUtil.match(outcomeQName, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED_QNAME);
    }

    public static boolean isRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
        AccessCertificationResponseType outcome = fromUri(aCase.getOutcome());
        List<AccessCertificationResponseType> revokes;
        if (campaign.getRemediationDefinition() != null && !campaign.getRemediationDefinition().getRevokeOn().isEmpty()) {
            revokes = campaign.getRemediationDefinition().getRevokeOn();
        } else {
            revokes = Collections.singletonList(REVOKE);
        }
        return revokes.contains(outcome);
    }
}
