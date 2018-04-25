package com.evolveum.midpoint.schrodinger.component.Resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceConnectorPoolTab <T> extends Component <T> {
    public ResourceConnectorPoolTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
