/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO that contains provisioning run-time configuration and diagnostic information.
 *
 * All information contained in this class are meant for information purposes only.
 * They are not meant to be used by a machine or algorithm, they are meant to be displayed
 * to a human user.
 *
 * @author Radovan Semancik
 * @author mederly
 *
 * TODO Remove GUI-specific things from this class (e.g. localized/default values).
 */
public class ProvisioningDiag implements Serializable {

    public static final String DETAILS_CONNECTOR_FRAMEWORK_VERSION = "ConnId framework version";
    private static final String UNKNOWN_VERSION = "unknown";

    // TODO some information about connector frameworks used etc

    /**
     * Additional information that do not fit the structured data above.
     * May be anything that the implementations thinks is important.
     *
     * Currently used as a hack to display connId version
     */
    private List<LabeledString> additionalDetails = new ArrayList<>();

    public List<LabeledString> getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(List<LabeledString> additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    @Override
    public String toString() {
        return "ProvisioningDiag(additionalDetails=" + additionalDetails + ")";
    }

    public void setConnectorFrameworkVersion(String version) {
        additionalDetails.add(new LabeledString(
                DETAILS_CONNECTOR_FRAMEWORK_VERSION,
                ObjectUtils.defaultIfNull(version, UNKNOWN_VERSION)));
    }
}
