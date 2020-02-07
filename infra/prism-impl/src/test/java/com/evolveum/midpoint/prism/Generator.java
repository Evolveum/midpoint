/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

public class Generator {

    @Test
    public void f() {
    }

    @Test
    public void test() throws Exception {
        File[] files = new File[] { new File(COMMON_DIR_PATH, "root-foo.xml") };
        File ff = new File(EXTRA_SCHEMA_DIR, "root.xsd");
        System.out.println("extra schema " + ff.getAbsolutePath());
        PrismContext prismContext = constructInitializedPrismContext(ff);
        for (File f : files){
            System.out.println("parsing file " + f.getName());
            PrismObject<?> o = prismContext.parseObject(f);

            String s = prismContext.serializeObjectToString(o, PrismContext.LANG_YAML);
            System.out.println("parsed: " + s);
            String fname = f.getName();
            fname = fname.replace(".xml", ".yaml");

            FileOutputStream bos = (new FileOutputStream(new File("src/test/resources/common/yaml/" + fname)));
            OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8);

            writer.write(s);
            writer.flush();
            writer.close();

            s = prismContext.serializeObjectToString(o, PrismContext.LANG_JSON);
            System.out.println("parsed: " + s);

            fname = fname.replace(".yaml", ".json");

            bos = (new FileOutputStream(new File("src/test/resources/common/json/" + fname)));
            writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8);

            writer.write(s);
            writer.flush();
            writer.close();

        }
    }

}
