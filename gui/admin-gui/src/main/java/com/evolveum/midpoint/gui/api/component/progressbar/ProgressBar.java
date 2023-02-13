package com.evolveum.midpoint.gui.api.component.progressbar;

import com.evolveum.midpoint.util.LocalizableMessage;

public class ProgressBar {

    public enum State {

        SUCCESS("bg-success"),
        INFO("bg-info"),
        DANGER("bg-danger"),
        WARNINIG("bg-warning"),
        SECONDARY("bg-secondary"),
        PRIMARY("bg-primary");

        private String cssClass;

        State(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private double value;

    private State state;

    private LocalizableMessage text;

    public ProgressBar(double value, State state) {
        this.value = value;
        this.state = state;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public LocalizableMessage getText() {
        return text;
    }

    public void setText(LocalizableMessage text) {
        this.text = text;
    }
}
