package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import javax.xml.namespace.QName;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PrismForm<T> extends Component<T> {

    public PrismForm(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<T> addAttributeValue(String name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(value);
        }
        // todo implement
        return this;
    }

    public PrismForm<T> removeAttributeValue(String name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> changeAttributeValue(String name, String oldValue, String newValue) {
        // todo implement
        return this;
    }

    public PrismForm<T> showEmptyAttributes(String containerName, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> addAttributeValue(QName name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(value);
        }
        // todo implement
        return this;
    }

    public PrismForm<T> setAttributeValue(QName name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> removeAttributeValue(QName name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> changeAttributeValue(QName name, String oldValue, String newValue) {
        // todo implement
        return this;
    }

    public PrismForm<T> showEmptyAttributes(QName containerName, String value) {
        // todo implement
        return this;
    }

    private SelenideElement findProperValueContainer() {
        return null;
    }

    private SelenideElement findProperty(String name) {
        return $(Schrodinger.byElementAttributeValue(null, "contains",
                Schrodinger.DATA_S_QNAME, "#" + name));
    }

    private SelenideElement findProperty(QName qname) {
        String name = Schrodinger.qnameToString(qname);
        return $(Schrodinger.byDataQName(name));
    }
}
