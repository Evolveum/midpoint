package com.evolveum.midpoint.schrodinger.util;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Utils {

    public static <T> T createInstance(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setOptionChecked(String optionName, boolean checked) {
        $(By.name(optionName)).setSelected(checked);
    }
}
