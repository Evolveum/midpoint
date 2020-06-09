/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Reference containing the name of referenced object.
 * Originally thought to be used in certifications, but replaced by dynamically joining RObject table.
 * So keeping this for possible future use only.
 *
 * @author mederly
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
    public RObjectType getType() {
        return super.getType();
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
        int result = super.hashCode();
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(REmbeddedNamedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        REmbeddedReference.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setTargetName(RPolyString.copyToJAXB(repo.getTargetName(), prismContext));
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);
        return ref;
    }
}
