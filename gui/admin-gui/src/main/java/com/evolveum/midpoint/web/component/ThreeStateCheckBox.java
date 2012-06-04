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

package com.evolveum.midpoint.web.component;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class ThreeStateCheckBox extends FormComponent<ThreeCheckState> {

    public ThreeStateCheckBox(String id) {
        this(id, null);
    }

    public ThreeStateCheckBox(String id, IModel<ThreeCheckState> model) {
        super(id, model);
        setType(ThreeCheckState.class);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);    //To change body of overridden methods use File | Settings | File Templates.
    }

    //todo implementation
}
