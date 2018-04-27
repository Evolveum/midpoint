package com.evolveum.midpoint.schrodinger.page.task;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.SummaryBox;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;


/**
 * Created by matus on 3/21/2018.
 */
public class EditTaskPage extends BasicPage {



    public SummaryBox<EditTaskPage> summary (){

        SelenideElement summaryBox =$(By.cssSelector("div.info-box-content"));

        return new SummaryBox(this,summaryBox);
    }
}
