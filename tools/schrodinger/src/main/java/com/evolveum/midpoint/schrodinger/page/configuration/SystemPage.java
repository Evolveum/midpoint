package com.evolveum.midpoint.schrodinger.page.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.configuration.*;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SystemPage extends BasicPage {

    public SystemPage cancel() {
        //todo implement
        return this;
    }

    public SystemPage save() {
        //todo implement
        return this;
    }

    public SystemTab systemTab() {
        //todo implement
        SelenideElement element = null;
        return new SystemTab(this, element);
    }

    public NotificationsTab notificationsTab() {
        //todo implement
        SelenideElement element = null;
        return new NotificationsTab(this, element);
    }

    public LoggingTab loggingTab() {
        //todo implement
        SelenideElement element = null;
        return new LoggingTab(this, element);
    }

    public ProfilingTab profilingTab() {
        //todo implement
        SelenideElement element = null;
        return new ProfilingTab(this, element);
    }

    public AdminGuiTab adminGuiTab() {
        //todo implement
        SelenideElement element = null;
        return new AdminGuiTab(this, element);
    }
}
