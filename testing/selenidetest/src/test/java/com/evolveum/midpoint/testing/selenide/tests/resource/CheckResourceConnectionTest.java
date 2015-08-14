package com.evolveum.midpoint.testing.selenide.tests.resource;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 09.08.2015.
 */
@Component
public class CheckResourceConnectionTest extends BaseTest {

    @Autowired
    ImportResourceTest importResourceTest;

    @Autowired
    LoginTest loginTest;

    @Autowired
    Util util;

    @Autowired
    ResourceUtil resourceUtil;

    @Test
    public void checkResourceConnectionTest(){
        //open Resources -> List Resources
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[4]/a")).shouldHave(text("Resources")).click();
        $(By.linkText("List resources")).click();

        //search for resource in resources list
        util.searchForElement(resourceUtil.getTestResourceName(), "/html/body/div[4]/div/form[1]/span/a");
        $(By.xpath("/html/body/div[4]/div/form[2]/div[2]/table/tbody/tr/td[2]/div/a/span")).click();
        //click on resource link
        $(By.linkText(resourceUtil.getTestResourceName())).click();

        //click Test connection button
        $(By.xpath("/html/body/div[4]/div/form/div[4]/a[1]")).should(appear).click();

        //check if all statuses are succeeded
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[1]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[2]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[3]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[4]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[5]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));

    }
}
