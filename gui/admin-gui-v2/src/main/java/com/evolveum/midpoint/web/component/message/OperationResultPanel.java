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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class OperationResultPanel extends Panel {

    public OperationResultPanel(String id, IModel<OperationResult> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<OperationResult> model) {
        add(new Label("message"));

        ListView<Map.Entry<String, Object>> params = new ListView<Map.Entry<String, Object>>("params",
                createParamsModel(model)) {

            @Override
            protected void populateItem(ListItem<Map.Entry<String, Object>> item) {
                item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "key")));
                item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        add(params);

        WebMarkupContainer exception = new WebMarkupContainer("exception") {

            @Override
            public boolean isVisible() {
                OperationResult result = model.getObject();
                return result.getCause() != null;
            }
        };
        add(exception);
        exception.add(new Label("exceptionMessage", new PropertyModel<Object>(model, "cause.message")));
        exception.add(new Label("exceptionStack", new Model<Serializable>("aaaaaaaaaaaaaaaa")));

        ListView<OperationResult> subresults = new ListView<OperationResult>("subresults",
                createSubresultsModel(model)) {

            @Override
            protected void populateItem(ListItem<OperationResult> item) {
                item.add(new OperationResultPanel("subresult", item.getModel()));
            }
        };
        add(subresults);
    }

    private IModel<List<Map.Entry<String, Object>>> createParamsModel(final IModel<OperationResult> model) {
        return new LoadableModel<List<Map.Entry<String, Object>>>(false) {

            @Override
            protected List<Map.Entry<String, Object>> load() {
                List<Map.Entry<String, Object>> params = new ArrayList<Map.Entry<String, Object>>();

                OperationResult result = model.getObject();
                Map<String, Object> map = result.getParams();
                if (map != null && !map.isEmpty()) {
                    params.addAll(map.entrySet());
                }

                return params;
            }
        };
    }

    private IModel<List<OperationResult>> createSubresultsModel(final IModel<OperationResult> model) {
        return new LoadableModel<List<OperationResult>>(false) {

            @Override
            protected List<OperationResult> load() {
                OperationResult result = model.getObject();
                List<OperationResult> subresults = result.getSubresults();
                if (subresults == null) {
                    subresults = new ArrayList<OperationResult>();
                }

                return subresults;
            }
        };
    }
}
