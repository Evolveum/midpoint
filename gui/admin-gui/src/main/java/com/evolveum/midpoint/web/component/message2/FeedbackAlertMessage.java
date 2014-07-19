/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.message2;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class FeedbackAlertMessage extends SimplePanel<FeedbackMessage> {

    private static final String ID_MESSAGE = "message";
    private static final String ID_TYPE = "type";

    public FeedbackAlertMessage(String id, IModel<FeedbackMessage> model) {
        super(id, model);

        add(new AttributeModifier("class", createCssClass()));
    }

    private IModel<String> createCssClass() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();
                sb.append("alert ");

                FeedbackMessage message = getModelObject();
                switch (message.getLevel()) {
                    case FeedbackMessage.INFO:
                        sb.append("alert-info ");
                        break;
                    case FeedbackMessage.SUCCESS:
                        sb.append("alert-success ");
                        break;
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.FATAL:
                        sb.append("alert-danger ");
                        break;
                    case FeedbackMessage.UNDEFINED:
                    case FeedbackMessage.DEBUG:
                    case FeedbackMessage.WARNING:
                    default:
                        sb.append("alert-warn ");
                }

                sb.append("alert-dismissable");
                return sb.toString();
            }
        };
    }

    @Override
    protected void initLayout() {
        Label message = new Label(ID_MESSAGE, new PropertyModel<>(getModel(), "message"));
        message.setRenderBodyOnly(true);
        add(message);

        Label type = new Label(ID_TYPE, createTypeModel());
        add(type);

        //todo "show more" link only for operation result messages
    }

    private IModel<String> createTypeModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                String key;

                FeedbackMessage message = getModelObject();
                switch (message.getLevel()) {
                    case FeedbackMessage.INFO:
                        key = "FeedbackAlertMessage.info";
                        break;
                    case FeedbackMessage.SUCCESS:
                        key = "FeedbackAlertMessage.success";
                        break;
                    case FeedbackMessage.ERROR:
                    case FeedbackMessage.FATAL:
                        key = "FeedbackAlertMessage.error";
                        break;
                    case FeedbackMessage.UNDEFINED:
                    case FeedbackMessage.DEBUG:
                    case FeedbackMessage.WARNING:
                    default:
                        key = "FeedbackAlertMessage.warn";
                }

                return createStringResource(key).getString();
            }
        };
    }
}
