/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;

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

    public static PolyStringType copyToJAXB(RPolyString string) {
        if (string == null) {
            return null;
        }

        PolyStringType poly = new PolyStringType();
        poly.setOrig(string.getOrig());
        poly.setNorm(string.getNorm());

        return poly;
    }

    public static PolyString fromRepo(RPolyString string) {
        if (string == null) {
            return null;
        }

        return new PolyString(string.getOrig(), string.getNorm());
    }
}
