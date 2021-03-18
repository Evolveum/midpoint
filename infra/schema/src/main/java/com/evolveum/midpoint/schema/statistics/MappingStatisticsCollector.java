/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

public interface MappingStatisticsCollector {

    void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName, long duration);

}
