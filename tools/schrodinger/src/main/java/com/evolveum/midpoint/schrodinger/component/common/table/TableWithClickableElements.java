package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

/**
 * Created by matus on 5/2/2018.
 */
public interface TableWithClickableElements {


    public <E extends BasicPage> E clickByName(String name);

}
