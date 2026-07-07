/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.result;

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Created by Viliam Repan (lazyman).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Toast implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(Toast.class);

    private String icon;

    private String title;

    private String subtitle;

    private Boolean close;

    private String body;

    private Boolean autohide;

    private Integer delay;

    @JsonProperty("className")
    private String cssClass;

    public String icon() {
        return icon;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public Boolean close() {
        return close;
    }

    public String body() {
        return body;
    }

    public String cssClass() {
        return cssClass;
    }

    public Boolean autohide() {
        return autohide;
    }

    public Integer delay() {
        return delay;
    }

    public Toast icon(String icon) {
        this.icon = icon;
        return this;
    }

    public Toast title(String title) {
        this.title = title;
        return this;
    }

    public Toast subtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public Toast close(Boolean close) {
        this.close = close;
        return this;
    }

    public Toast body(String body) {
        this.body = body;
        return this;
    }

    public Toast cssClass(String cssClass) {
        this.cssClass = cssClass;
        return this;
    }

    public Toast autohide(Boolean autohide) {
        this.autohide = autohide;
        return this;
    }

    public Toast delay(Integer delay) {
        this.delay = delay;
        return this;
    }

    public Toast info() {
        return cssClass("text-bg-info");
    }

    public Toast success() {
        return cssClass("text-bg-success");
    }

    public Toast error() {
        return cssClass("text-bg-danger");
    }

    public Toast warning() {
        return cssClass("text-bg-warning");
    }

    public void show(@NotNull AjaxRequestTarget target) {
        try {
            target.appendJavaScript("MidPointTheme.showToast(" + getVariables() + ");");
        } catch (Exception ex) {
            target.appendJavaScript("console.error('Couldn't create toast, reason: " + ex.getMessage() + "');");
            LOGGER.debug("Couldn't create toast", ex);
        }
    }

    public void show(@NotNull IHeaderResponse response) {
        try {
            response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.showToast(" + getVariables() + ");"));
        } catch (Exception ex) {
            response.render(OnDomReadyHeaderItem.forScript(
                    "console.error('Couldn't create toast, reason: " + ex.getMessage() + "');"));
            LOGGER.debug("Couldn't create toast", ex);
        }
    }

    private String getVariables() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
