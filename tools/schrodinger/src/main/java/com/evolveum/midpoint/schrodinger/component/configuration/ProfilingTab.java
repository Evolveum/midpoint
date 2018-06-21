package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProfilingTab extends Component<SystemPage> {

    public ProfilingTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}

