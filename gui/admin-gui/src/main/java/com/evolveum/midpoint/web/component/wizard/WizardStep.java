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

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class WizardStep extends org.apache.wicket.extensions.wizard.WizardStep {

    public WizardStep() {
        setTitleModel(new StringResourceModel("WizardStep.title", this, null, "WizardStep.title"));
    }

    @Override
    public Component getHeader(String id, Component parent, IWizard wizard) {
        return new Label(id, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getTitle();
            }
        });
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    public StringResourceModel createStringResource(Enum e) {
        return createStringResource(e, null);
    }

    public StringResourceModel createStringResource(Enum e, String prefix) {
        return createStringResource(e, prefix, null);
    }

    public StringResourceModel createStringResource(Enum e, String prefix, String nullKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix).append('.');
        }

        if (e == null) {
            if (StringUtils.isNotEmpty(nullKey)) {
                sb.append(nullKey);
            } else {
                sb = new StringBuilder();
            }
        } else {
            sb.append(e.getDeclaringClass().getSimpleName()).append('.');
            sb.append(e.name());
        }

        return createStringResource(sb.toString());
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    protected List<QName> loadResourceObjectClassList(IModel<PrismObject<ResourceType>> model, Trace LOGGER, String message){
        List<QName> list = new ArrayList<>();

        try {
            ResourceSchema schema = RefinedResourceSchema.getResourceSchema(model.getObject(), getPageBase().getPrismContext());
            schema.getObjectClassDefinitions();

            for(Definition def: schema.getDefinitions()){
                list.add(def.getTypeName());
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, message, e);
            error(message + " " + e.getMessage());
        }

        return list;
    }

    protected IValidator<String> createObjectClassValidator(final IModel<List<QName>> model){
        return new IValidator<String>() {

            @Override
            public void validate(IValidatable<String> validated) {
                String value = validated.getValue();
                List<QName> list = model.getObject();
                List<String> stringList = new ArrayList<>();

                for(QName q: list){
                    stringList.add(q.getLocalPart());
                }

                if(!stringList.contains(value)){
                    error(createStringResource("SchemaHandlingStep.message.validationError", value).getString());
                    AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
                    target.add(getPageBase().getFeedbackPanel());
                }
            }
        };
    }
}
