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

import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ListMultipleChoicePanel<T> extends InputPanel {

    public ListMultipleChoicePanel(String id, IModel<List<T>> model, IModel<List<T>> choices) {
        super(id);
        ListMultipleChoice<T> multiple = new ListMultipleChoice<T>("input", model, choices);
        add(multiple);
    }

    @Override
    public FormComponent getComponent() {
        return (FormComponent) get("input");
    }
}
