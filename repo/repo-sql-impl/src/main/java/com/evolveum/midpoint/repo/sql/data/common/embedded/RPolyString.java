/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * @author lazyman
 */
@Embeddable
@JaxbType(type = PolyString.class)
public class RPolyString implements Serializable {

    public static final String F_NORM = "norm";
    public static final String F_ORIG = "orig";

    private String orig;
    private String norm;

    public RPolyString() {
    }

    public RPolyString(String orig, String norm) {
        this.norm = norm;
        this.orig = orig;
    }

    @Column(nullable = true)
    public String getNorm() {
        return norm;
    }

    @Column(nullable = true)
    public String getOrig() {
        return orig;
    }

    public void setNorm(String norm) {
        this.norm = norm;
    }

    public void setOrig(String orig) {
        this.orig = orig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RPolyString that = (RPolyString) o;

        if (norm != null ? !norm.equals(that.norm) : that.norm != null) return false;
        if (orig != null ? !orig.equals(that.orig) : that.orig != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = orig != null ? orig.hashCode() : 0;
        result = 31 * result + (norm != null ? norm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static RPolyString copyFromJAXB(PolyStringType string) {
        if (string == null) {
            return null;
        }

        return new RPolyString(string.getOrig(), string.getNorm());
    }

    public static RPolyString toRepo(PolyString string) {
        if (string == null) {
            return null;
        }

        return new RPolyString(string.getOrig(), string.getNorm());
    }

    public static PolyStringType copyToJAXB(RPolyString string, PrismContext ctx) {
        if (string == null) {
            return null;
        }

        String orig = string.getOrig();
        String norm = string.getNorm();

        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);

        if (orig != null && norm == null) {
            norm = ctx.getDefaultPolyStringNormalizer().normalize(orig);
            poly.setNorm(norm);
        } else {
            poly.setNorm(norm);
        }

        return poly;
    }

    public static PolyString fromRepo(RPolyString string, PrismContext prismContext) {
        if (string == null) {
            return null;
        }

        return copyToJAXB(string, prismContext).toPolyString();
    }
}
