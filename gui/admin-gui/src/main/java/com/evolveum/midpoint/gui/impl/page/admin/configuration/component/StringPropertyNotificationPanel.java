/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author skublik
 */
public class StringPropertyNotificationPanel extends Panel {
        private static final long serialVersionUID = 1L;

        private static final String ID_HELP = "help";
        private static final String ID_LABEL = "label";
        private static final String ID_VALUE = "value";
        private static final String ID_LABEL_CONTAINER = "labelContainer";

        private IModel<Object> model;
        private IModel<String> name;
        private PageBase page;
        private Class<?> type;

        private static final Trace LOGGER = TraceManager.getTrace(NotificationConfigTabPanel.class);

        private boolean labelContainerVisible = true;

        public StringPropertyNotificationPanel(String id, final IModel<Object> model, final IModel<String> name, Class<?> type, Form form, PageBase page) {
            super(id, model);
            Validate.notNull(model, "no model");
            this.model = model;
            this.name = name;
            this.page = page;
            this.type = type;

            LOGGER.info("Creating property panel for {}", model.getObject());

            setOutputMarkupId(true);
            initLayout(model, form);
        }

        public IModel<Object> getModel() {
            return model;
        }

        private PageBase getPageBase() {
            return page;
        }

        private void initLayout(final IModel<Object> model, final Form form) {
            WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
            labelContainer.setOutputMarkupId(true);
            labelContainer.add(new VisibleEnableBehaviour() {
                private static final long serialVersionUID = 1L;

                @Override public boolean isVisible() {
                    return labelContainerVisible;
                }
            });
            add(labelContainer);

            final IModel<String> label = name;
            Label displayName = new Label(ID_LABEL, label);
            displayName.add(new AttributeModifier("style", new IModel<String>() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return "text-decoration: none;";
                }
            }));
            labelContainer.add(displayName);

            final IModel<String> helpText = getPageBase().createStringResource(name.getObject() + ".help", "");
            Label help = new Label(ID_HELP);
            help.add(AttributeModifier.replace("title", helpText));
            help.add(new InfoTooltipBehavior());
            help.add(new VisibleEnableBehaviour() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return StringUtils.isNotEmpty(helpText.getObject()) && !((name.getObject() + ".help").equals(helpText.getObject()));
                }
            });
            labelContainer.add(help);
            Component panel = null;
            if(type.equals(String.class)) {
                panel = new TextPanel(ID_VALUE, getModel(), String.class);
            } else if(type.equals(Boolean.class)) {
                panel = new TriStateComboPanel(ID_VALUE, (IModel)getModel());
            } else {
                throw new IllegalStateException("Unsupported type " + getModel().getObject().getClass().getName() + " for Model");
            }
            panel.add(new AttributeModifier("class", new IModel<String>() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return getInputCssClass();
                }
            }));
            add(panel);
        }

        protected String getInputCssClass() {
            return"col-xs-10";
        }

        protected String getValuesClass() {
            return "col-md-6";
        }

        protected String getValueCssClass() {
            return "row";
        }
    }
