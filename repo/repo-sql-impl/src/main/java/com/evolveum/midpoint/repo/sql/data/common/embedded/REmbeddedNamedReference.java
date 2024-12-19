/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Reference containing the name of referenced object.
 * Originally thought to be used in certifications, but replaced by dynamically joining RObject table.
 * So keeping this for possible future use only.
 */
@Embeddable
public class REmbeddedNamedReference extends REmbeddedReference {

    private RPolyString targetName;

    public RPolyString getTargetName() {
        return targetName;
    }

    public void setTargetName(RPolyString targetName) {
        this.targetName = targetName;
    }

    // these three methods are repeated here because of some hibernate weirdness

    @Column(length = RUtil.COLUMN_LENGTH_QNAME)
    @Override
    public String getRelation() {
        return super.getRelation();
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    @Override
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getTargetType() {
        return super.getTargetType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof REmbeddedNamedReference)) { return false; }
        if (!super.equals(o)) { return false; }

        REmbeddedNamedReference that = (REmbeddedNamedReference) o;

        return Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetName);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(REmbeddedNamedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        RSimpleEmbeddedReference.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setTargetName(RPolyString.copyToJAXB(repo.getTargetName(), prismContext));
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);
        return ref;
    }
}
