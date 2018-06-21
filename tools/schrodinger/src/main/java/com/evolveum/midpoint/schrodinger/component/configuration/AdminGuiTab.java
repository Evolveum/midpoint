package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AdminGuiTab extends Component<SystemPage> {

    public AdminGuiTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}