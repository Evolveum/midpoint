/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.expand.Expander;
import com.evolveum.midpoint.ninja.action.expand.ExpanderOptions;
import com.evolveum.midpoint.ninja.action.expand.KeepassProviderOptions;

public class ExpanderTest {

    @Test
    public void test100Simple() {
        ExpanderOptions opts = new ExpanderOptions();
        opts.setSecretsProvider(ExpanderOptions.SecretsProvider.KEEPASS);
        opts.setProperties(new File("src/test/resources/sample.properties"));

        KeepassProviderOptions keepassOpts = opts.getKeepassProviderOptions();
        keepassOpts.setFile(new File("src/test/resources/sample.kdbx"));
        keepassOpts.setPassword("qwe123");

        Expander expander = new Expander(opts);
        expander.init();
        String pwd = expander.expandKey("some.key");
        Assertions.assertThat(pwd).isEqualTo("qwe123");   // obtained from sample.kdbx

        String value = expander.expandKey("some.unencrypted.key");
        Assertions.assertThat(value).isEqualTo("Some unencrypted value");
    }
}
