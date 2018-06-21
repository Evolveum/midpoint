package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusDelegatedToMeTab<T> extends Component<T> {

    public FocusDelegatedToMeTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
