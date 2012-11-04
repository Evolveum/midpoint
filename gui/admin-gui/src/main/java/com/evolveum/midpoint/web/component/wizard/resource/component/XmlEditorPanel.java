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

package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class XmlEditorPanel extends SimplePanel {

    private static final String ID_ACE_EDITOR = "aceEditor";

    public XmlEditorPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        AceEditor editor = new AceEditor(ID_ACE_EDITOR, getModel());
        add(editor);
    }
}
