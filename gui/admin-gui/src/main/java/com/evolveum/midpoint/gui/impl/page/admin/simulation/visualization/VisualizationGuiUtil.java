package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import com.evolveum.midpoint.prism.delta.ChangeType;

public class VisualizationGuiUtil {

    public static String createChangeTypeCssClassForOutlineCard(ChangeType change) {
        if (change == null) {
            return "card-outline-left-secondary";
        }

        switch (change) {
            case ADD:
                return "card-outline-left-success";
            case MODIFY:
                return "card-outline-left-info";
            case DELETE:
                return "card-outline-left-danger";
        }

        return "card-outline-left-secondary";
    }
}
