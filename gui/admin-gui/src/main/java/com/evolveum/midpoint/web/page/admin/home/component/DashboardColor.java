package com.evolveum.midpoint.web.page.admin.home.component;

/**
 * @author lazyman
 */
public enum DashboardColor {

    GRAY("box-default"),
    BLUE("box-info"),
    GREEN("box-success"),
    YELLOW("box-warning"),
    RED("box-danger");

    private String cssClass;

    private DashboardColor(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
