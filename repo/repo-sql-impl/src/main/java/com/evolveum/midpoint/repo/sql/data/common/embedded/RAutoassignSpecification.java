/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;
import jakarta.persistence.Embeddable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;

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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RAutoassignSpecification that = (RAutoassignSpecification) o;

        return Objects.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return enabled != null ? enabled.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RAutoassignSpecification{enabled=" + enabled + '}';
    }

    public static void formJaxb(AutoassignSpecificationType aa, RAutoassignSpecification raa) {
        Objects.requireNonNull(aa, "Autoassign specification type must not be null");
        Objects.requireNonNull(raa, "Repo autoassign specification must not be null");

        raa.setEnabled(aa.isEnabled());
    }
}
