package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusBasicTab<T> extends Component<T> {

    public FocusBasicTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
