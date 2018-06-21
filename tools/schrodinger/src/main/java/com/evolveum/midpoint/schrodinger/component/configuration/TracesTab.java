package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TracesTab extends Component<InternalsConfigurationPage> {

    public TracesTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}

