package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusHistoryTab<T> extends Component<T> {

    public FocusHistoryTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
