/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportConfigurationType;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class ReportExportConfigurationMapper implements Mapper<ExportConfigurationType, RExportType> {

    @Override
    public RExportType map(ExportConfigurationType input, MapperContext context) {
        if (input.getType() == null) {
            return null;
        }
        return RExportType.valueOf(input.getType().name());
    }
}
