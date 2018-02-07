package com.evolveum.midpoint.schrodinger.page.configuration;

import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import java.io.File;

import static com.codeborne.selenide.Selenide.$;
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
        $(By.name("input:inputFile:fileInput")).sendKeys(file.getAbsolutePath());
        //todo implement
        return this;
    }

    public ImportObjectPage setEditorText(String text) {
        // todo implement, nothing works yet
//        executeJavaScript(
//                "const ta = document.querySelector(\"textarea\"); " +
//                        "ta.value = \"asdf\";" +
//                        "ta.dispatchEvent(new Event(\"input\"));");

//        text = "asdf";
//        executeJavaScript(
//                "var ta = $(\"textarea[name='input:inputAce:aceEditor']\"); " +
//                        "ta.value('" + text + "'); " +
//                        "ta.dispatchEvent(new Event(\"input\"));");

//        $(".ace_content").shouldBe(Condition.visible);
//        $(".ace_content").click();
//        $(".ace_content").sendKeys("asdf");


        return this;
    }

    public ImportObjectPage clickImport() {
        $(".main-button-bar").$x("//a[@about='importFileButton']").click();
        return this;
    }
}
