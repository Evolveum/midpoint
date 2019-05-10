/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class SimpleValueExpressionPanel extends BasePanel<ExpressionType>{
    private static final long serialVersionUID = 1L;

    private final static String ID_LITERAL_VALUE_INPUT = "literalValueInput";
    private static final String ID_FEEDBACK = "feedback";

    private static final Trace LOGGER = TraceManager.getTrace(SimpleValueExpressionPanel.class);
    private static final String DOT_CLASS = SimpleValueExpressionPanel.class.getName() + ".";

    public SimpleValueExpressionPanel(String id, IModel<ExpressionType> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

        MultiValueTextPanel<String> literalValueInput = new MultiValueTextPanel<String>(ID_LITERAL_VALUE_INPUT,
                new IModel<List<String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<String> getObject() {
                        return getLiteralValues();
                    }

                    @Override
                    public void setObject(List<String> strings) {
                        ExpressionUtil.updateLiteralExpressionValue(getModelObject(), strings, SimpleValueExpressionPanel.this.getPageBase().getPrismContext());
                    }

                    @Override
                    public void detach() {

                    }
                },
                new NonEmptyLoadableModel<Boolean>(false) {
                    @NotNull
                    @Override
                    protected Boolean load() {
                        return false;
                    }
                },
                false){

            private static final long serialVersionUID = 1L;

            @Override
            protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<String> modelObject) {
                ExpressionUtil.updateLiteralExpressionValue(SimpleValueExpressionPanel.this.getModelObject(),
                        modelObject, SimpleValueExpressionPanel.this.getPageBase().getPrismContext());
            }
        };
        literalValueInput.setOutputMarkupId(true);
        add(literalValueInput);
    }

    private List<String> getLiteralValues(){
        List<String> literalValueList = new ArrayList<>();
        try{
            return ExpressionUtil.getLiteralExpressionValues(getModelObject());
        } catch (SchemaException ex){
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
        }
        return literalValueList;
    }


}
