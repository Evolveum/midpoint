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

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class BottomMenuItem implements Serializable {

    private IModel<String> label;
    private Class<? extends Page> page;
    private VisibleEnableBehaviour visible;

    public BottomMenuItem(IModel<String> label, Class<? extends Page> page) {
        this(label, page, null);
    }

    public BottomMenuItem(IModel<String> label, Class<? extends Page> page, VisibleEnableBehaviour visible) {
        this.label = label;
        this.page = page;
        this.visible = visible;
    }

    public IModel<String> getLabel() {
        return label;
    }

    public Class<? extends Page> getPage() {
        return page;
    }

    public VisibleEnableBehaviour getVisible() {
        return visible;
    }
}
