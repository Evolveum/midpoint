package com.evolveum.midpoint.gui.impl.component;

import java.io.Serializable;

public class DetailsNavigationMainItem implements Serializable {

    public static final String F_LABEL_KEY = "labelKey";

    private String labelKey;

    public DetailsNavigationMainItem(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }
}
