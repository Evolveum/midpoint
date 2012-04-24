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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.Comparator;

/**
 * @author lazyman
 */
public class PropertyWrapperComparator implements Comparator<PropertyWrapper> {

    private PrismContainerDefinition definition;

    public PropertyWrapperComparator(PrismContainerDefinition definition) {
        Validate.notNull(definition, "Prism container definition must not be null.");
        this.definition = definition;
    }

    @Override
    public int compare(PropertyWrapper p1, PropertyWrapper p2) {
        ItemDefinition def1 = definition.findPropertyDefinition(p1.getProperty().getName());
        ItemDefinition def2 = definition.findPropertyDefinition(p2.getProperty().getName());

        return String.CASE_INSENSITIVE_ORDER.compare(getDisplayName(def1), getDisplayName(def2));
    }

    private String getDisplayName(ItemDefinition def) {
        String displayName = def.getDisplayName();

        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }

        return def.getNameOrDefaultName().getLocalPart();
    }
}
