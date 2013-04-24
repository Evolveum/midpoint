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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.GuiComponents;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

/**
 * @author mserbak
 */
public class TriStateComboPanel extends InputPanel {

    private static final String ID_COMBO = "combo";

    public TriStateComboPanel(String id, final IModel<Boolean> model) {
        super(id);

        DropDownChoice combo = GuiComponents.createTriStateCombo(ID_COMBO, model);
        add(combo);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_COMBO);
    }
}
