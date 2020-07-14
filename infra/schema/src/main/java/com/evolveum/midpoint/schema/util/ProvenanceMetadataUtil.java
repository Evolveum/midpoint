/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceYieldType;

@Experimental
public class ProvenanceMetadataUtil {

    public static boolean hasOrigin(ProvenanceYieldType yield, String originOid) {
        return yield.getAcquisition().stream()
                .anyMatch(acquisition -> hasOrigin(acquisition, originOid));
    }

    public static boolean hasOrigin(ProvenanceAcquisitionType acquisition, String originOid) {
        return acquisition.getOriginRef() != null && originOid.equals(acquisition.getOriginRef().getOid());
    }
}
