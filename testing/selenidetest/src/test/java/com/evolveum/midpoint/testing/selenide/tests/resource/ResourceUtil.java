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

    /**
     * searches for resource in the resource list
     * with the specified name resourceName
     *
     * @param resourceName
     * @param resourceSearchResultPath
     * @return
     */
    public SelenideElement searchForOpendjResource(String resourceName, String resourceSearchResultPath) {
        //search for OpenDJ resource in resources list
        $(By.name("basicSearch:searchText")).shouldBe(visible).setValue(resourceName);
        $(By.linkText("Search")).click();
        //check if resource is found during the search
        return $(By.xpath(resourceSearchResultPath)).shouldHave(text(resourceName));
    }

    public String getTestResourceName() {
        return testResourceName;
    }

    public void setTestResourceName(String testResourceName) {
        this.testResourceName = testResourceName;
    }

}
