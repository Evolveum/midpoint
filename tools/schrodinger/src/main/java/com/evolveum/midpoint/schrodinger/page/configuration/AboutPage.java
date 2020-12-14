/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.common.table.ReadOnlyTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AboutPage extends BasicPage {

    // public static Trace LOGGER = TraceManager.getTrace(AboutPage.class);

    public AboutPage repositorySelfTest() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.testRepository")).click();
        return this;
    }

    public AboutPage checkAndRepairOrgClosureConsistency() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.testRepositoryCheckOrgClosure")).click();
        return this;
    }

    public AboutPage reindexRepositoryObjects() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.reindexRepositoryObjects")).click();
        return this;
    }

    public AboutPage provisioningSelfTest() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.testProvisioning")).click();
        return this;
    }

    public AboutPage cleanupActivitiProcesses() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.checkWorkflowProcesses")).click();
        return this;
    }

    public AboutPage clearCssJsCache() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.clearCssJsCache")).click();
        return this;
    }

    public String version() {
        return $(Schrodinger.bySchrodingerDataId("wicket_message-1130625241")).parent().getText();
    }

    public String gitDescribe() {
        return $(Schrodinger.bySchrodingerDataResourceKey("midpoint.system.build")).parent().getText();
    }

    public String buildAt() {
        return $(Schrodinger.bySchrodingerDataId("build")).parent().getText();
    }


    // NOTE not sure if using xpath is the best way around this
    public String hibernateDialect() {
        SelenideElement additionalDetailsBox = $(By.cssSelector("div.box.box-danger"));

        return additionalDetailsBox.find(By.xpath("/html/body/div[2]/div/section/div[2]/div[1]/div[2]/div/div[2]/div[2]/table/tbody/tr[4]/td[2]")).getText();
    }

    public String connIdFrameworkVersion() {
        return $(Schrodinger.bySchrodingerDataId("provisioningDetailValue")).parent().getText();
    }

    public List<String> getJVMproperties() {
        SelenideElement jvmProperties = $(Schrodinger.byDataId("jvmProperties"));
        String jvmPropertiesText = jvmProperties.getText();

        List<String> listOfProperties = new ArrayList<>();
        if (jvmPropertiesText != null && !jvmPropertiesText.isEmpty()) {
            String[] properties = jvmPropertiesText.split("\\r?\\n");

            listOfProperties = Arrays.asList(properties);

        } else {
            // LOGGER.info("JVM properties not found";

        }

        return listOfProperties;
    }

    public String getJVMproperty(String property) {

        List<String> listOfProperties = getJVMproperties();

        if (property != null && !property.isEmpty()) {

            for (String keyPair : listOfProperties) {

                String[] pairs = keyPair.split("\\=");

                if (pairs != null && pairs.length > 1) {
                    if (pairs[0].equals(property)) {
                        return pairs[1];
                    }
                } else if (pairs.length == 1) {
                    if (pairs[0].contains(property)) {
                        return pairs[0];
                    }

                }
            }
        }

        return "";
    }


    public ConfirmationModal<FormLoginPage> clickSwitchToFactoryDefaults() {
        $(Schrodinger.byDataResourceKey("PageAbout.button.factoryDefault")).waitUntil(Condition.visible,MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ConfirmationModal<>(new FormLoginPage(), Utils.getModalWindowSelenideElement());
    }

    public String getSystemProperty(String propertyNameUserHome) {
        SelenideElement propertiesTable = $(Schrodinger.byElementValue("h3","System properties")).waitUntil(Condition.appear,MidPoint.TIMEOUT_DEFAULT_2_S).parent().$(By.cssSelector(".table.table-striped"));

        ReadOnlyTable readOnlyTable = new ReadOnlyTable(this,propertiesTable);
        return readOnlyTable.getParameterValue(propertyNameUserHome);
    }
}

