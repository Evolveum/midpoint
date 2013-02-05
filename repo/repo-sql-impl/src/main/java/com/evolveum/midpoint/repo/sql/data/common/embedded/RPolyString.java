/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Embeddable
public class RPolyString implements Serializable {

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

    public static PolyStringType copyToJAXB(RPolyString string) {
        if (string == null) {
            return null;
        }

        PolyStringType poly = new PolyStringType();
        poly.setOrig(string.getOrig());
        poly.setNorm(string.getNorm());

        return poly;
    }
}
