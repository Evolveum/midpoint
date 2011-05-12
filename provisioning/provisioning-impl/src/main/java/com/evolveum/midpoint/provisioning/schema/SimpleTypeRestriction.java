/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.schema;

import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSimpleType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Vilo Repan
 */
public class SimpleTypeRestriction implements Serializable {

    private String[] enumeration = null;
    private String maxValue = null;
    private String minValue = null;
    private String length = null;
    private String maxLength = null;
    private String minLength = null;
    private String pattern = null;
    private String totalDigits = null;

    public String[] getEnumeration() {
        return enumeration;
    }

    public String getMaxLength() {
        return maxLength;
    }

    public String getMinLength() {
        return minLength;
    }

    public String getLength() {
        return length;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTotalDigits() {
        return totalDigits;
    }

    public static SimpleTypeRestriction createTypeRestriction(XSSimpleType type) {
        SimpleTypeRestriction typeRestriction = new SimpleTypeRestriction();

        XSRestrictionSimpleType restriction = type.asRestriction();
        if (restriction != null) {
            List<String> enumeration = new ArrayList<String>();
            Collection<? extends XSFacet> i = restriction.getDeclaredFacets();
            for (XSFacet facet : i) {
                if (facet.getName().equals(XSFacet.FACET_ENUMERATION)) {
                    enumeration.add(facet.getValue().value);
                }
                if (facet.getName().equals(XSFacet.FACET_MAXINCLUSIVE)) {
                    typeRestriction.maxValue = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_MININCLUSIVE)) {
                    typeRestriction.minValue = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_MAXEXCLUSIVE)) {
                    typeRestriction.maxValue = String.valueOf(Integer.parseInt(facet.getValue().value) - 1);
                }
                if (facet.getName().equals(XSFacet.FACET_MINEXCLUSIVE)) {
                    typeRestriction.minValue = String.valueOf(Integer.parseInt(facet.getValue().value) + 1);
                }
                if (facet.getName().equals(XSFacet.FACET_LENGTH)) {
                    typeRestriction.length = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_MAXLENGTH)) {
                    typeRestriction.maxLength = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_MINLENGTH)) {
                    typeRestriction.minLength = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_PATTERN)) {
                    typeRestriction.pattern = facet.getValue().value;
                }
                if (facet.getName().equals(XSFacet.FACET_TOTALDIGITS)) {
                    typeRestriction.totalDigits = facet.getValue().value;
                }
            }

            if (enumeration.size() > 0) {
                typeRestriction.enumeration = enumeration.toArray(new String[]{});
            }
        }
        return typeRestriction;
    }
}
