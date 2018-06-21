package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusProjectionsTab<T> extends Component<T> {

    public FocusProjectionsTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
