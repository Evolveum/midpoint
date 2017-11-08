/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;
import org.apache.commons.lang3.Validate;

import javax.persistence.Embeddable;

/**
 * Created by Viliam Repan (lazyman).
 */
@Embeddable
public class RAutoassignSpecification {

    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAutoassignSpecification that = (RAutoassignSpecification) o;

        return enabled != null ? enabled.equals(that.enabled) : that.enabled == null;
    }

    @Override
    public int hashCode() {
        return enabled != null ? enabled.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RAutoassignSpecification{");
        sb.append("enabled=").append(enabled);
        sb.append('}');
        return sb.toString();
    }

    public static void copyFromJAXB(AutoassignSpecificationType aa, RAutoassignSpecification raa) {
        Validate.notNull(aa, "Autoassign specification type must not be null");
        Validate.notNull(raa, "Repo autoassign specification must not be null");

        raa.setEnabled(aa.isEnabled());
    }
}
