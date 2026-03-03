/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.SerializableFunction;

public class LocalFileInputPanel extends InputPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LocalFileInputPanel.class);

    private static final String ID_DROPDOWN_CONTAINER = "dropdownContainer";
    private static final String ID_DROPDOWN = "dropdown";
    private static final String ID_INPUT = "input";

    private final IModel<String> model;

    private final IModel<List<PathPrefix>> prefixesModel = new LoadableModel<>(false) {

        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected List<PathPrefix> load() {
            return loadPathPrefixes();
        }
    };

    private final IModel<PathPrefix> selectedPrefixModel = Model.of(PathPrefix.MIDPOINT_HOME);

    private final IModel<String> inputModel = Model.of();

    public LocalFileInputPanel(String id, IModel<String> model) {
        super(id);

        this.model = model;
    }

    @Override
    public FormComponent<T> getBaseFormComponent() {
        // noinspection unchecked
        return (FormComponent<T>) get(ID_INPUT);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "input-group"));

        WebMarkupContainer dropdownContainer = new WebMarkupContainer(ID_DROPDOWN_CONTAINER);
        dropdownContainer.setOutputMarkupId(true);
        add(dropdownContainer);

        List<InlineMenuItem> items = new ArrayList<>();
        for (PathPrefix prefix : prefixesModel.getObject()) {
            items.add(new InlineMenuItem(createStringResource(prefix.labelKey)) {

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {

                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            selectPrefixPerformed(target, prefix);
                        }
                    };
                }
            });
        }

        DropdownButtonDto dropdownDto = new DropdownButtonDto(
                null, null, () -> LocalizationUtil.translate(selectedPrefixModel.getObject().labelKey), items);

        DropdownButtonPanel dropdown = new DropdownButtonPanel(ID_DROPDOWN, dropdownDto) {

            @Override
            protected String getSpecialButtonClass() {
                return "btn-default btn-sm";
            }
        };
        dropdown.setRenderBodyOnly(true);
        dropdownContainer.add(dropdown);

        final TextField<String> text = new TextField<>(ID_INPUT, inputModel);
        text.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateMainModel();
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                // todo implement
            }
        });
        add(text);
    }

    private Component getDropdownContainer() {
        return get(ID_DROPDOWN_CONTAINER);
    }

    private void selectPrefixPerformed(AjaxRequestTarget target, PathPrefix prefix) {
        selectedPrefixModel.setObject(prefix);
        target.add(getDropdownContainer());

        updateMainModel();
    }

    private void updateMainModel() {
        PathPrefix pathPrefix = selectedPrefixModel.getObject();
        String input = inputModel.getObject();

        String output = null;
        if (input != null) {
            output = pathPrefix.prefixFunction.apply(input);
        }

        model.setObject(output);

        LOGGER.trace("Selected path {}", output);
    }

    private List<PathPrefix> loadPathPrefixes() {
        return Arrays.asList(PathPrefix.values());
    }

    private enum PathPrefix {

        ABSOLUTE("LocalFileInputPanel.absolute", s -> s),

        MIDPOINT_HOME("LocalFileInputPanel.midpointHome", s -> {

            String mpHome = System.getProperty("midpoint.home");
            if (mpHome == null) {
                return s;
            }

            if (StringUtils.isEmpty(s)) {
                return s;
            }

            if (!mpHome.endsWith("/") && !s.startsWith("/")) {
                mpHome = mpHome + "/";
            } else if (mpHome.endsWith("/") && s.startsWith("/")) {
                mpHome = mpHome.substring(0, mpHome.length() - 1);
            }

            return mpHome + s;
        });

        public final String labelKey;

        public final SerializableFunction<String, String> prefixFunction;

        PathPrefix(String labelKey, SerializableFunction<String, String> prefixFunction) {
            this.labelKey = labelKey;
            this.prefixFunction = prefixFunction;
        }
    }
}
