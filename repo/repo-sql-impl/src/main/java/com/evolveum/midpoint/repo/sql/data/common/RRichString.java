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

package com.evolveum.midpoint.repo.sql.data.common;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author lazyman
 */
@Embeddable
public class RRichString {

    private String original;
    private String normalized;

    @Column(nullable = true)
    public String getNormalized() {
        return normalized;
    }

    @Column(nullable = true)
    public String getOriginal() {
        return original;
    }

    public void setNormalized(String normalized) {
        this.normalized = normalized;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RRichString that = (RRichString) o;

        if (normalized != null ? !normalized.equals(that.normalized) : that.normalized != null) return false;
        if (original != null ? !original.equals(that.original) : that.original != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = original != null ? original.hashCode() : 0;
        result = 31 * result + (normalized != null ? normalized.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RRichString[");
        builder.append(original);
        builder.append(",");
        builder.append(normalized);
        builder.append("]");

        return builder.toString();
    }
}
