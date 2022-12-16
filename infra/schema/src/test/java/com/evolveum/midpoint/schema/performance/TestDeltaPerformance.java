/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.performance;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class TestDeltaPerformance extends AbstractSchemaPerformanceTest {

    @Test
    public void test100ApplyNameDelta() throws Exception {
        PrismObject<UserType> jack = getJack();
        PrismObject<UserType> temp = jack.clone();
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_NAME).replace("jjj")
                .asObjectDelta(temp.getOid());
        measure("delta.applyTo.name",
                "Applies name change delta to jack",
                () -> { delta.applyTo(temp); return true; });

        PrismObject<UserType> jack2 = jack.clone();
        delta.applyTo(jack2);
        measure("jack2.diff.name",
                "Diffs jack and modified jack (name)",
                () -> jack2.diff(jack));
    }

    @Test
    public void test102ApplyPasswordValueDelta() throws Exception {
        PrismObject<UserType> jack = getJack();
        PrismObject<UserType> temp = jack.clone();
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE).replace("cleartext")
                .asObjectDelta(temp.getOid());
        measure("delta.applyTo.credentials",
                "Applies credentials change delta to jack",
                () -> { delta.applyTo(temp); return true; });
        PrismObject<UserType> jack2 = jack.clone();
        delta.applyTo(jack2);
        measure("jack2.diff(jack)",
                "Diffs jack and modified jack (credentials)",
                () -> jack2.diff(jack));
    }

    @Test
    public void test104ApplyExtensionItemDelta() throws Exception {
        PrismObject<UserType> jack = getJack();
        PrismObject<UserType> temp = jack.clone();
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "bar23"), def("bar23")).replace("bbb")
                .asObjectDelta(temp.getOid());
        measure("delta.applyTo(jack)", "", () -> { delta.applyTo(temp); return true; });
        PrismObject<UserType> jack2 = jack.clone();
        delta.applyTo(jack2);
        measure("jack2.diff(jack)", "", () -> jack2.diff(jack));
    }

    @Test
    public void test110ApplyDeltaWith5Modifications() throws Exception {
        PrismObject<UserType> jack = getJack();
        PrismObject<UserType> temp = jack.clone();
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_NAME).replace("jjj")
                .item(UserType.F_HONORIFIC_SUFFIX).replace("sss")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar23"), def("bar23")).replace("bbb")
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE).replace("cleartext")
                .item(UserType.F_ADMIN_GUI_CONFIGURATION).replace(new AdminGuiConfigurationType(getPrismContext()))
                .asObjectDelta(temp.getOid());
        measure("delta.applyTo.5mod",
                "Applies 5 modification delta to jack",
                () -> { delta.applyTo(temp); return true; });
        PrismObject<UserType> jack2 = jack.clone();
        delta.applyTo(jack2);
        measure("jack2.diff.5mod",
                "Diffs 5 modification delta from jack",
                () -> jack2.diff(jack));
    }

    private PrismPropertyDefinition<String> def(String name) {
        return getPrismContext().definitionFactory()
                .createPropertyDefinition(new QName(NS_FOO, name), DOMUtil.XSD_STRING);
    }

    @Test
    public void test120ApplyDeltaWith30Modifications() throws Exception {
        PrismObject<UserType> jack = getJack();
        PrismObject<UserType> temp = jack.clone();
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "bar0"), def("bar0")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar1"), def("bar1")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar2"), def("bar2")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar3"), def("bar3")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar4"), def("bar4")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar5"), def("bar5")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar6"), def("bar6")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar7"), def("bar7")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar8"), def("bar8")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar9"), def("bar9")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar10"), def("bar10")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar11"), def("bar11")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar12"), def("bar12")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar13"), def("bar13")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar14"), def("bar14")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar15"), def("bar15")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar16"), def("bar16")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar17"), def("bar17")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar18"), def("bar18")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar19"), def("bar19")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar20"), def("bar20")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar21"), def("bar21")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar22"), def("bar22")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar23"), def("bar23")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar24"), def("bar24")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar25"), def("bar25")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar26"), def("bar26")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar27"), def("bar27")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar28"), def("bar28")).replace("bbb")
                .item(ItemPath.create(UserType.F_EXTENSION, "bar29"), def("bar29")).replace("bbb")
                .asObjectDelta(temp.getOid());
        measure("delta.applyTo.30mod",
                "Applies delta with 30 modifications",
                () -> { delta.applyTo(temp); return true; });
        PrismObject<UserType> jack2 = jack.clone();
        delta.applyTo(jack2);
        measure("jack2.diff.30mod",
                "Diffs original with modified (30 items changed)",
                () -> jack2.diff(jack));
    }
}
