package com.evolveum.midpoint.testing.schrodinger.reports;

import com.codeborne.selenide.testng.TextReport;

public class SchrodingerTextReport extends TextReport {

    public SchrodingerTextReport() {
        super();
        this.report = new SchrodingerSimpleReport();
    }
}
