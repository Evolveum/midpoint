package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceTimeoutsTab<T> extends Component<T> {
    public ResourceTimeoutsTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
