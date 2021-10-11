/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RAutoassignSpecification;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AutoassignSpecificationMapper implements Mapper<AutoassignSpecificationType, RAutoassignSpecification> {

    @Override
    public RAutoassignSpecification map(AutoassignSpecificationType input, MapperContext context) {
        RAutoassignSpecification rspec = new RAutoassignSpecification();
        RAutoassignSpecification.formJaxb(input, rspec);
        return rspec;
    }
}
