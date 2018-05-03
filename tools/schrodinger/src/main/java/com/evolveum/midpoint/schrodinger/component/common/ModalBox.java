package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 4/26/2018.
 */
public class ModalBox<T> extends Component {
    public ModalBox(Object parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
