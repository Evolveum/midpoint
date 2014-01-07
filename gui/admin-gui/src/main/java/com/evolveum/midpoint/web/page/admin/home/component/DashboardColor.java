package com.evolveum.midpoint.web.page.admin.home.component;

/**
 * @author lazyman
 */
public enum DashboardColor {

    GRAY("panel-default"),
    BLUE("panel-info"),
    GREEN("panel-success"),
    YELLOW("panel-warning"),
    RED("panel-danger");

    private String cssClass;

    private DashboardColor(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
