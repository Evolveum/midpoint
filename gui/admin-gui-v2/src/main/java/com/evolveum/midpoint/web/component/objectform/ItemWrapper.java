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

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import java.io.Serializable;

/**
 * @author lazyman
 */
public interface ItemWrapper extends Serializable {

    /**
     * This method reads annotation in property container
     * definition which can contain position for this element in form.
     * Administrators can therefore set input field positions (order).
     *
     * @return position from annotation.
     */
    int getPosition();

    /**
     * This method is used as label for input text field. Also used for
     * default field ordering.
     *
     * @return field name.
     */
    String getName();

    /**
     * Object form can create place holders for property containers, properties
     * and property values. This method should remove them.
     */
    void cleanup();

    /**
     * Object form remembers changes created by user
     * @return
     */
    ObjectDelta createObjectDelta();
}
