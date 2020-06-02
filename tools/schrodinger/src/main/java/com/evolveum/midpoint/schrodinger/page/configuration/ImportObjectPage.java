/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionChecked;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportObjectPage extends BasicPage {

    public ImportObjectPage checkProtectedByEncryption() {
        setOptionChecked("importOptions:protectedByEncryption", true);
        return this;
    }

    public ImportObjectPage checkFetchResourceSchema() {
        setOptionChecked("importOptions:fetchResourceSchema", true);
        return this;
    }

    public ImportObjectPage checkKeepOid() {
        setOptionChecked("importOptions:keepOid", true);
        return this;
    }

    public ImportObjectPage checkOverwriteExistingObject() {
        setOptionChecked("importOptions:overwriteExistingObject", true);
        return this;
    }

    public ImportObjectPage checkReferentialIntegrity() {
        setOptionChecked("importOptions:referentialIntegrity", true);
        return this;
    }

    public ImportObjectPage checkSummarizeSuccesses() {
        setOptionChecked("importOptions:summarizeSuccesses", true);
        return this;
    }

    public ImportObjectPage checkValidateDynamicSchema() {
        setOptionChecked("importOptions:validateDynamicSchema", true);
        return this;
    }

    public ImportObjectPage checkValidateStaticSchema() {
        setOptionChecked("importOptions:validateStaticSchema", true);
        return this;
    }

    public ImportObjectPage checkSummarizeErrors() {
        setOptionChecked("importOptions:summarizeErrors", true);
        return this;
    }

    public ImportObjectPage uncheckProtectedByEncryption() {
        setOptionChecked("importOptions:protectedByEncryption", false);
        return this;
    }

    public ImportObjectPage uncheckFetchResourceSchema() {
        setOptionChecked("importOptions:fetchResourceSchema", false);
        return this;
    }

    public ImportObjectPage uncheckKeepOid() {
        setOptionChecked("importOptions:keepOid", false);
        return this;
    }

    public ImportObjectPage uncheckOverwriteExistingObject() {
        setOptionChecked("importOptions:overwriteExistingObject", false);
        return this;
    }

    public ImportObjectPage uncheckReferentialIntegrity() {
        setOptionChecked("importOptions:referentialIntegrity", false);
        return this;
    }

    public ImportObjectPage uncheckSummarizeSuccesses() {
        setOptionChecked("importOptions:summarizeSuccesses", false);
        return this;
    }

    public ImportObjectPage uncheckValidateDynamicSchema() {
        setOptionChecked("importOptions:validateDynamicSchema", false);
        return this;
    }

    public ImportObjectPage uncheckValidateStaticSchema() {
        setOptionChecked("importOptions:validateStaticSchema", false);
        return this;
    }

    public ImportObjectPage uncheckSummarizeErrors() {
        setOptionChecked("importOptions:summarizeErrors", false);
        return this;
    }

    public ImportObjectPage stopAfterErrorsExceed(Integer count) {
        String c = count == null ? "" : count.toString();
        $(By.name("importOptions:errors")).setValue(c);
        return this;
    }

    public ImportObjectPage getObjectsFromFile() {
        $(By.name("importRadioGroup")).selectRadio("radio0");
        return this;
    }

    public ImportObjectPage getObjectsFromEmbeddedEditor() {
        $(By.name("importRadioGroup")).selectRadio("radio1");
        return this;
    }

    public ImportObjectPage chooseFile(File file) {

        $(By.name("input:inputFile:fileInput")).uploadFile(file);
        //todo implement
        return this;
    }

    public ImportObjectPage setEditorXmlText(String text) {
        $(By.xpath("/html/body/div[2]/div/section/form/div[4]/div/div[1]/div/button[1]")).click();
        SelenideElement aceEditor = $(By.className("ace_text-input"));
        aceEditor.setValue(text);
        aceEditor.screenshot();

        //uncomment the following peace of code if the value is set to ace editor incorrectly
        // (e.g. multiple close tags present)
//           int lastIndex = 0;
//           while (lastIndex < text.length() && text.substring(lastIndex, text.length()-1).contains("<")) {
//            text = text.substring(lastIndex, text.length());
//            int actualIndex = text.indexOf("<");
//            if (actualIndex != 0) {
//                aceEditor.setValue(text.substring(0, actualIndex));
//            }
//            lastIndex = actualIndex;
//            text = text.substring(actualIndex);
//            if (text.startsWith("</")) {
//                lastIndex = text.indexOf(">") + 1;
//                aceEditor.setValue(text.substring(0, lastIndex));
//            } else if (text.startsWith("<?")) {
//                lastIndex = text.indexOf("?>") + 2;
//                aceEditor.setValue(text.substring(0, lastIndex));
//            } else if (text.startsWith("<!--")) {
//                lastIndex = text.indexOf("-->") + 3;
//                aceEditor.setValue(text.substring(0, lastIndex));
//            } else {
//
//                lastIndex = text.indexOf(">") + 1;
//                String tag = text.substring(0, lastIndex);
//                aceEditor.setValue(tag);
//                String endTag = "</" + tag.substring(1, (tag.contains(" ") ? (tag.indexOf(" ")) : (tag.length()-1))) + ">";
//                for (int i = 0; i < endTag.length(); i++) {
//                    aceEditor.sendKeys(Keys.DELETE);
//                }
//
//            }
//        }
//
//        obviously this peace of code was used as experimental, needs to be cleaned up
//
//        executeJavaScript(
//                "const ta = document.querySelector(\"textarea\"); " +
//                        "ta.value = \"asdf\";" +
//                        "ta.dispatchEvent(new Event(\"input\"));");
//
//        text = "asdf";
//        executeJavaScript(
//                "var ta = $(\"textarea[name='input:inputAce:aceEditor']\"); " +
//                        "ta.value('" + text + "'); " +
//                        "ta.dispatchEvent(new Event(\"input\"));");
//
//        $(".ace_content").shouldBe(Condition.visible);
//        $(".ace_content").click();
//        $(".ace_content").sendKeys("asdf");


        return this;
    }

    public ImportObjectPage clickImportFileButton() {
        $(".main-button-bar").$x("//a[@data-s-id='importFileButton']").click();
        return this;
    }

    public ImportObjectPage clickImportXmlButton() {
        $(".main-button-bar").$x("//a[@data-s-id='importXmlButton']").click();
        return this;
    }

}
