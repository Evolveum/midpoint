/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

import java.io.Serializable;

/**
 * Created by matus on 9/19/2017.
 */
public class ValuePolicyDto implements Serializable {

    public static final String F_PRISM_OBJECT = "prismObject";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";


    private ValuePolicyType valuePolicy;

    public ValuePolicyDto(ValuePolicyType valuePolicy) {
        this.valuePolicy = valuePolicy;
    }

    public PrismObject<ValuePolicyType> getPrismObject() {
        return valuePolicy.asPrismObject();
    }


}
