/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a form containing multiple progress bars, each visualizing the frequency of certain values.
 * The form displays a title based on the attribute name and the count of values.
 * It iterates through JSON information provided during initialization to create individual progress bars.
 * <p>
 * Example usage:
 * <pre>{@code
 * String jsonInformation = "{ \"attribute1\": [{\"value\": \"value1\", \"frequency\": 50}, {\"value\": \"value2\", \"frequency\": 30}] }";
 * ProgressBarForm progressBarForm = new ProgressBarForm("progressBarForm", jsonInformation);
 * add(progressBarForm);
 * }</pre>
 */
public class ProgressBarForm extends BasePanel<String> {
    private static final String ID_CONTAINER = "container";
    private static final String ID_FORM_TITLE = "progressFormTitle";
    private static final String ID_REPEATING_VIEW = "repeatingProgressBar";

    String jsonInformation;

    public ProgressBarForm(String id, String jsonInformation) {
        super(id);
        this.jsonInformation = jsonInformation;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        JSONObject jsonObject = new JSONObject(jsonInformation);
        String title = getPageBase().createStringResource("Title.unknown").getString();
        for (String key : jsonObject.keySet()) {
            title = key;
            break;
        }

        JSONArray jsonArray = jsonObject.getJSONArray(title);
        int valuesCount = jsonArray.length();

        String attribute = getPageBase().createStringResource("Attribute.item.name").getString();
        Label titleForm = new Label(ID_FORM_TITLE, attribute + ": " + title + " (" + valuesCount + ")");
        titleForm.setOutputMarkupId(true);
        container.add(titleForm);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW);
        repeatingProgressBar.setOutputMarkupId(true);
        container.add(repeatingProgressBar);

        initProgressBars(jsonArray, repeatingProgressBar);

    }

    private static void initProgressBars(@NotNull JSONArray jsonArray, @NotNull RepeatingView repeatingProgressBar) {
        jsonArray.forEach((item) -> {
            JSONObject jsonItem = (JSONObject) item;
            String jsonValue = jsonItem.getString("value");
            String jsonFrequency = jsonItem.getString("frequency");
            double frequency = Double.parseDouble(jsonFrequency);

            ProgressBar progressBar = new ProgressBar(repeatingProgressBar.newChildId()) {
                @Override
                public double getActualValue() {
                    return frequency;
                }

                @Override
                public String getBarTitle() {
                    return jsonValue;
                }
            };
            progressBar.setOutputMarkupId(true);
            repeatingProgressBar.add(progressBar);
        });
    }

}
