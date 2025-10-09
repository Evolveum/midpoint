/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

public class AccCertCampaignAsserter<RA> extends PrismObjectAsserter<AccessCertificationCampaignType,RA> {

    public AccCertCampaignAsserter(PrismObject<AccessCertificationCampaignType> focus) {
        super(focus);
    }

    public AccCertCampaignAsserter(PrismObject<AccessCertificationCampaignType> focus, String details) {
        super(focus, details);
    }

    public AccCertCampaignAsserter(PrismObject<AccessCertificationCampaignType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static AccCertCampaignAsserter<Void> forCampaign(PrismObject<AccessCertificationCampaignType> object) {
        return new AccCertCampaignAsserter<>(object);
    }

    public static AccCertCampaignAsserter<Void> forCampaign(PrismObject<AccessCertificationCampaignType> object, String details) {
        return new AccCertCampaignAsserter<>(object, details);
    }

    public AccCertCasesAsserter<AccCertCampaignAsserter<RA>> cases() {
        var asserter = new AccCertCasesAsserter<>(this, getCases(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private List<AccessCertificationCaseType> getCases() {
        return getObjectable().getCase();
    }

    @Override
    public AccCertCampaignAsserter<RA> display() {
        return (AccCertCampaignAsserter<RA>) super.display();
    }
}
