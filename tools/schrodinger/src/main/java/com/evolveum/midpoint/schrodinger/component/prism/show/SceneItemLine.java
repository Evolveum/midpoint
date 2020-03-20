package com.evolveum.midpoint.schrodinger.component.prism.show;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.*;

public class SceneItemLine<T> extends Component<T> {

    public SceneItemLine(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

}
