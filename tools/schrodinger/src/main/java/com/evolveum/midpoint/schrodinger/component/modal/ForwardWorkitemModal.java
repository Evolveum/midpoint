package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class ForwardWorkitemModal<T> extends ModalBox<T> {

    public ForwardWorkitemModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ObjectBrowserModalTable<T, ForwardWorkitemModal<T>> table(){
        SelenideElement box = $(Schrodinger.byElementAttributeValue("div", "class","box boxed-table"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ObjectBrowserModalTable<T, ForwardWorkitemModal<T>>(this, box){
            public T clickByName(String name){
                getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

//                box.waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return getParent().getParent();
            }
        };
    }

}
