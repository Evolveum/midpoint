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

package com.evolveum.midpoint.web.jsf;

import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;

/**
 *
 * @author Vilo Repan
 */
public abstract class AbstractStateHolder implements StateHolder {

    private boolean isTransient;

    @Override
    public Object saveState(FacesContext context) {
        Object[] object = new Object[1];
        object[0] = isTransient;

        return object;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] object = (Object[]) state;
        isTransient = (Boolean) object[0];
    }

    @Override
    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        isTransient = newTransientValue;
    }
}
