package com.evolveum.midpoint.testing.selenide.tests.resource;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 12.08.2015.
 */
@Component
public class ResourceUtil {
    private String testResourceName;
    private String testResourcePath;

    public String getTestResourceName() {
        return testResourceName;
    }

    public void setTestResourceName(String testResourceName) {
        this.testResourceName = testResourceName;
    }

    public String getTestResourcePath() {
        return testResourcePath;
    }

    public void setTestResourcePath(String testResourcePath) {
        this.testResourcePath = testResourcePath;
    }
}
