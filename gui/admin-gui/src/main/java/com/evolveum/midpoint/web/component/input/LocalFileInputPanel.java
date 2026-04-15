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
import org.apache.wicket.util.file.File;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.validation.FilePathValidator;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.SerializableFunction;

public class LocalFileInputPanel extends InputPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LocalFileInputPanel.class);

    private enum PathPrefix {

        ABSOLUTE("LocalFileInputPanel.absolute", s -> s),

        MIDPOINT_HOME_RESOURCES("LocalFileInputPanel.midpointHomeResources", s -> {

            String mpHome = System.getProperty("midpoint.home");
            if (StringUtils.isEmpty(mpHome)) {
                return s;
            }

            String resourcesPath = concatPaths(mpHome, "resources");

            return concatPaths(resourcesPath, s);
        });

        // todo to be discused later, whether we'll use this one or not
//        MIDPOINT_HOME("LocalFileInputPanel.midpointHome", s -> {
//
//            String mpHome = System.getProperty("midpoint.home");
//            if (StringUtils.isEmpty(mpHome)) {
//                return s;
//            }
//
//            return concatPaths(mpHome, s);
//        });

        public final String labelKey;

        public final SerializableFunction<String, String> prefixFunction;

        PathPrefix(String labelKey, SerializableFunction<String, String> prefixFunction) {
            this.labelKey = labelKey;
            this.prefixFunction = prefixFunction;
        }

        private static String concatPaths(String prefix, String suffix) {
            if (prefix == null) {
                return suffix;
            }

            if (StringUtils.isEmpty(suffix)) {
                return prefix;
            }

            String separator = File.separator;

            if (!prefix.endsWith(separator) && !suffix.startsWith(separator)) {
                prefix = prefix + separator;
            } else if (prefix.endsWith(separator) && suffix.startsWith(separator)) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }

            return prefix + suffix;
        }
    }

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

    private final IModel<PathPrefix> selectedPrefixModel = Model.of(PathPrefix.MIDPOINT_HOME_RESOURCES);

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

        populateModels();
        initLayout();
    }

    private void populateModels() {
        String currentValue = model.getObject();
        if (currentValue == null) {
            return;
        }

        for (PathPrefix pathPrefix : prefixesModel.getObject()) {
            if (pathPrefix == PathPrefix.ABSOLUTE) {
                continue; // skip absolute, it is default
            }

            String prefix = pathPrefix.prefixFunction.apply("");
            if (StringUtils.isEmpty(prefix)) {
                // if prefix is empty, it means that it cannot be used as prefix, so skip it
                continue;
            }

            if (!currentValue.startsWith(prefix)) {
                continue;
            }

            selectedPrefixModel.setObject(pathPrefix);

            String suffix = currentValue.substring(prefix.length());
            inputModel.setObject(suffix);

            LOGGER.trace("Populated models with prefix {} (real prefix {}) and input {}", pathPrefix, prefix, suffix);

            return;
        }

        // if no prefix matches, use absolute as default
        selectedPrefixModel.setObject(PathPrefix.ABSOLUTE);
        inputModel.setObject(currentValue);
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

        DropdownButtonDto dropdownDto = DropdownButtonDto.create(
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
                LocalFileInputPanel.this.onError(target);
            }
        });

        FilePathValidator validator = new FilePathValidator() {

            @Override
            protected String preProcessPath(String path) {
                PathPrefix prefix = selectedPrefixModel.getObject();

                return prefix != null ? prefix.prefixFunction.apply(path) : path;
            }
        };
        validator.checkExistence(true);

        text.add(validator);
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

    protected void onError(AjaxRequestTarget target) {
        // override this method in case you want to do something when validation error occurs
    }
}
