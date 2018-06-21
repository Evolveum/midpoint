/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
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

        DropDownChoice combo = WebComponentUtil.createTriStateCombo(ID_COMBO, model);
        add(combo);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_COMBO);
    }
}
