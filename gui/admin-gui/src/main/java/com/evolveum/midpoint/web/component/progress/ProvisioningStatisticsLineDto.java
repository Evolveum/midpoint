/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsOperationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsType;

/**
 * @author Pavol Mederly
 */
public class ProvisioningStatisticsLineDto {

    public static final String F_RESOURCE_REF = "resourceRef";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_OPERATIONS = "operations";

    private ObjectReferenceType resourceRef;
    private QName objectClass;
    private List<ProvisioningStatisticsOperationDto> operations;

    public ProvisioningStatisticsLineDto(ProvisioningStatisticsEntryType entry) {
        this.resourceRef = entry.getResourceRef();
        this.objectClass = entry.getObjectClass();
        this.operations = ProvisioningStatisticsOperationDto.extractFromOperationalInformation(entry.getOperation());
    }

    public ObjectReferenceType getResourceRef() {
        return resourceRef;
    }

    public static List<ProvisioningStatisticsLineDto> extractFromOperationalInformation(ProvisioningStatisticsType provisioningStatisticsType) {
        List<ProvisioningStatisticsLineDto> retval = new ArrayList<>();
        if (provisioningStatisticsType == null) {
            return retval;
        }
        for (ProvisioningStatisticsEntryType entry : provisioningStatisticsType.getEntry()) {
            retval.add(new ProvisioningStatisticsLineDto(entry));
        }
        return retval;
    }

    public List<ProvisioningStatisticsOperationDto> getOperations() {
        return operations;
    }
}
