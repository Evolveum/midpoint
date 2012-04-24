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

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class FeedbackDetailsPanel extends Panel {

    public FeedbackDetailsPanel(String id, final IModel<OperationResult> model) {
        super(id);

        add(new AttributeAppender("class", new LoadableModel<String>() {

            @Override
            protected String load() {
                return createCss(model.getObject());
            }
        }, " "));

        initLayout(model);
    }

    private void initLayout(IModel<OperationResult> model) {

    }

    private String createCss(OperationResult result) {
        if (result == null || result.getStatus() == null) {
            return "messages-warn-content";
        }
        switch (result.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                return "messages-error-content";
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                return "messages-info-content";
            case SUCCESS:
                return "messages-succ-content";
            case UNKNOWN:
            case WARNING:
            default:
                return "messages-warn-content";
        }
    }
}
