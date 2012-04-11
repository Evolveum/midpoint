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

package com.evolveum.midpoint.web.component.objectform;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class PropertyDivider extends PropertyItem {

    private PrismContainerDefinition definition;

    public PropertyDivider(PropertyPath path, PrismContainerDefinition definition) {
        super(path);
        this.definition = definition;
    }

    public PrismContainerDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getDisplayName() {
        String displayName = definition.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            displayName = definition.getName().getLocalPart();
        }
        return displayName;
    }
}
