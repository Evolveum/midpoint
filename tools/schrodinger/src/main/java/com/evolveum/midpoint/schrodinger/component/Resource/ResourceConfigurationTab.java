package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.resource.EditResourceConfigurationPage;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceConfigurationTab extends Component<EditResourceConfigurationPage> {
    public ResourceConfigurationTab(EditResourceConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ResourceConfigurationTab(EditResourceConfigurationPage parent) {
        super(parent);
    }

    public PrismForm<ResourceConfigurationTab> form() {

        SelenideElement element = null;

        return new PrismForm<>(this, element);
    }
}
