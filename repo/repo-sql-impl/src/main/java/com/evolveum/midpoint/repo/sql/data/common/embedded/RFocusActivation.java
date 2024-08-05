/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.enums.RLockoutStatus;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * Embeddable mapping for ActivationType, specific for R_FOCUS.
 * Other entities with activation has no need for lockout status.
 */
@Embeddable
@JaxbType(type = ActivationType.class)
public class RFocusActivation extends RActivation {

    private RLockoutStatus lockoutStatus;

    @JdbcType(IntegerJdbcType.class)
    public RLockoutStatus getLockoutStatus() {
        return lockoutStatus;
    }

    public void setLockoutStatus(RLockoutStatus lockoutStatus) {
        this.lockoutStatus = lockoutStatus;
    }

    public static void fromJaxb(ActivationType jaxb, RFocusActivation repo)
            throws DtoTranslationException {
        RActivation.fromJaxb(jaxb, repo);

        repo.setLockoutStatus(RUtil.getRepoEnumValue(jaxb.getLockoutStatus(), RLockoutStatus.class));
    }

    public static void copyToJAXB(RFocusActivation repo, ActivationType jaxb, PrismContext prismContext) {
        RActivation.copyToJAXB(repo, jaxb, prismContext);

        if (repo.getLockoutStatus() != null) {
            jaxb.setLockoutStatus(repo.getLockoutStatus().getSchemaValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        RFocusActivation that = (RFocusActivation) o;
        return lockoutStatus == that.lockoutStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lockoutStatus);
    }
}
