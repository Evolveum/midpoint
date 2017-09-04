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

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.LocaleDescriptor;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Locale;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);

    private static final String ID_SELECT = "select";
    private static final String ID_OPTIONS = "options";

    public LocalePanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        final IModel<LocaleDescriptor> model = new Model(getSelectedLocaleDescriptor());
        Select<LocaleDescriptor> select = new Select<>(ID_SELECT, model);
        select.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                changeLocale(target, model.getObject());
            }
        });
        select.setOutputMarkupId(true);
        add(select);
        SelectOptions<LocaleDescriptor> options = new SelectOptions<LocaleDescriptor>(ID_OPTIONS,
                MidPointApplication.AVAILABLE_LOCALES,
                new IOptionRenderer<LocaleDescriptor>() {

                    @Override
                    public String getDisplayValue(LocaleDescriptor object) {
                        return object.getName();
                    }

                    @Override
                    public IModel<LocaleDescriptor> getModel(LocaleDescriptor value) {
                        return new Model<>(value);
                    }
                }) {



            @Override
            protected SelectOption<LocaleDescriptor> newOption(String text, IModel<LocaleDescriptor> model) {
                SelectOption option = super.newOption("&nbsp;" + text, model);
                option.add(new AttributeModifier("data-icon", "flag-" + model.getObject().getFlag()));

                return option;
            }
        };
        select.add(options);
    }

    private LocaleDescriptor getSelectedLocaleDescriptor() {
        Locale locale = getSession().getLocale();
        if (locale == null) {
            return null;
        }

        // The second condition is a fix attempt for issue MID-2075, where firefox
        // returns 'sk' as a locale from session, while other browsers return 'sk_SK'.
        // This is the reason, why in firefox selected locale is ignored (the commented
        // condition is not met) so we are adding second condition to overcome this issue.
        for (LocaleDescriptor desc : MidPointApplication.AVAILABLE_LOCALES) {
//            if (locale.equals(desc.getLocale())
            if (locale.equals(desc.getLocale()) || locale.getLanguage().equals(desc.getLocale().getLanguage())) {
                return desc;
            }
        }

        return null;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String selectId = get(ID_SELECT).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", new Object[]{descriptor.getLocale()});
        getSession().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
