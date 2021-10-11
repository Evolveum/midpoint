/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAvailabilityStatus;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Embeddable;
import javax.persistence.Enumerated;

@Embeddable
public class ROperationalState {

    RAvailabilityStatus lastAvailabilityStatus;

    @Enumerated
    public RAvailabilityStatus getLastAvailabilityStatus() {
        return lastAvailabilityStatus;
    }

    public void setLastAvailabilityStatus(RAvailabilityStatus lastAvailabilityStatus) {
        this.lastAvailabilityStatus = lastAvailabilityStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROperationalState that = (ROperationalState) o;

        if (lastAvailabilityStatus != null ? !lastAvailabilityStatus.equals(that.lastAvailabilityStatus) :
                that.lastAvailabilityStatus != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lastAvailabilityStatus != null ? lastAvailabilityStatus.hashCode() : 0;
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(ROperationalState repo, OperationalStateType jaxb, ObjectType parent, ItemPath path,
                                  PrismContext prismContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            if (repo.getLastAvailabilityStatus() != null) {
                jaxb.setLastAvailabilityStatus(repo.getLastAvailabilityStatus().getSchemaValue());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void fromJaxb(OperationalStateType jaxb, ROperationalState repo) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            if (jaxb.getLastAvailabilityStatus() != null) {
                repo.setLastAvailabilityStatus(RUtil.getRepoEnumValue(jaxb.getLastAvailabilityStatus(),
                        RAvailabilityStatus.class));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public OperationalStateType toJAXB(ObjectType parent, ItemPath path, PrismContext prismContext) throws
            DtoTranslationException {
        OperationalStateType operationalState = new OperationalStateType();
        ROperationalState.copyToJAXB(this, operationalState, parent, path, prismContext);
        return operationalState;
    }
}
