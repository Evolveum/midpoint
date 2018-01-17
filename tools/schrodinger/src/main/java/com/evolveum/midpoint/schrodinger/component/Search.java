package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Search<T> extends Component<T> {

    public Search(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
