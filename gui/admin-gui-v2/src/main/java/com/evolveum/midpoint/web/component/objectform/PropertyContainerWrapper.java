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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import org.apache.commons.lang.Validate;

import java.util.List;

/**
 * @author lazyman
 */
public class PropertyContainerWrapper implements ItemWrapper {

    private PrismContainer container;
    private ItemStatus status;
    private List<ItemWrapper> items;

    public PropertyContainerWrapper(PrismContainer container, ItemStatus status) {
        Validate.notNull(container, "Property container must not be null.");
        Validate.notNull(status, "Status must not be null.");

        this.container = container;
        this.status = status;
    }

    @Override
    public void cleanup() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getPosition() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    public String getDescription() {
        return null;   //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObjectDelta createObjectDelta() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
