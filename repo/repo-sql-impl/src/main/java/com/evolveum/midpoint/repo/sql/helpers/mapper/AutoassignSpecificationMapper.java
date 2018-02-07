/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        RAutoassignSpecification.copyFromJAXB(input, rspec);
        return rspec;
    }
}