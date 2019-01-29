package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceResultsHandlersTab<T> extends Component<T> {
    public ResourceResultsHandlersTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
