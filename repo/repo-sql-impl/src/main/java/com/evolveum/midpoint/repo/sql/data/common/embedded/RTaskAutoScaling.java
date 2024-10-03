/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;

import jakarta.persistence.Embeddable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.enums.RTaskAutoScalingMode;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAutoScalingType;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * Embeddable mapping for TaskAutoScalingType.
 */
@Embeddable
@JaxbType(type = TaskAutoScalingType.class)
public class RTaskAutoScaling {

    private RTaskAutoScalingMode mode;

    @JdbcType(IntegerJdbcType.class)
    public RTaskAutoScalingMode getMode() {
        return mode;
    }

    public void setMode(RTaskAutoScalingMode mode) {
        this.mode = mode;
    }

    public static void fromJaxb(TaskAutoScalingType jaxb, RTaskAutoScaling repo)
            throws DtoTranslationException {
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        Objects.requireNonNull(repo, "Repo object must not be null.");

        repo.setMode(RUtil.getRepoEnumValue(jaxb.getMode(), RTaskAutoScalingMode.class));
    }

    public static void copyToJAXB(RTaskAutoScaling repo, TaskAutoScalingType jaxb,
            @SuppressWarnings("unused") PrismContext prismContext) {
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        Objects.requireNonNull(repo, "Repo object must not be null.");

        if (repo.getMode() != null) {
            jaxb.setMode(repo.getMode().getSchemaValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        if (!super.equals(o)) {return false;}

        RTaskAutoScaling that = (RTaskAutoScaling) o;
        return mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode);
    }
}
