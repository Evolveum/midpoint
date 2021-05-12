/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TARGET_DIR_PATH;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

class SchemaConfiguration {

    private static final String PROP = "schema";
    private static final String PROP_SINGLE_VALUED_PROPERTIES = PROP + ".single-valued-properties";
    private static final String PROP_MULTI_VALUED_PROPERTIES = PROP + ".multi-valued-properties";
    private static final String PROP_INDEXED_PERCENTAGE = PROP + ".indexed-percentage";

    private static final File SCHEMA_TEMPLATE_FILE = new File(TEST_DIR, "schema.vm.xsd");

    private static final String GENERATED_SCHEMA_DIR_PATH = TARGET_DIR_PATH + "/schema";
    private static final File GENERATED_SCHEMA_DIR = new File(GENERATED_SCHEMA_DIR_PATH);
    private static final File GENERATED_SCHEMA_FILE = new File(GENERATED_SCHEMA_DIR, "schema.xsd");

    private static final String P_SINGLE_PATTERN = "p-single-%04d";
    private static final String P_MULTI_PATTERN = "p-multi-%04d";

    private final int singleValuedProperties;
    private final int multiValuedProperties;
    private final int indexedPercentage;

    private SchemaConfiguration() {
        this.singleValuedProperties = Integer.parseInt(System.getProperty(PROP_SINGLE_VALUED_PROPERTIES, "100"));
        this.multiValuedProperties = Integer.parseInt(System.getProperty(PROP_MULTI_VALUED_PROPERTIES, "10"));
        this.indexedPercentage = Integer.parseInt(System.getProperty(PROP_INDEXED_PERCENTAGE, "0"));

        generateSchemaFile();
    }

    int getSingleValuedProperties() {
        return singleValuedProperties;
    }

    int getMultiValuedProperties() {
        return multiValuedProperties;
    }

    int getIndexedPercentage() {
        return indexedPercentage;
    }

    @Override
    public String toString() {
        return "SchemaConfiguration{" +
                "singleValuedProperties=" + singleValuedProperties +
                ", multiValuedProperties=" + multiValuedProperties +
                ", indexedPercentage=" + indexedPercentage +
                '}';
    }

    public static SchemaConfiguration setup() {
        System.setProperty("midpoint.global.extensionDir", GENERATED_SCHEMA_DIR_PATH);

        SchemaConfiguration configuration = new SchemaConfiguration();
        System.out.println("Schema: " + configuration);
        return configuration;
    }

    private void generateSchemaFile() {

        List<ItemDef> itemDefList = new ArrayList<>();
        IndexedSelector indexedSelector = new IndexedSelector();
        for (int i = 0; i < singleValuedProperties; i++) {
            String name = String.format(P_SINGLE_PATTERN, i);
            boolean indexed = indexedSelector.getNext();
            itemDefList.add(new ItemDef(name, "1", indexed));
        }
        indexedSelector.reset();
        for (int i = 0; i < multiValuedProperties; i++) {
            String name = String.format(P_MULTI_PATTERN, i);
            boolean indexed = indexedSelector.getNext();
            itemDefList.add(new ItemDef(name, "unbounded", indexed));
        }

        //noinspection ResultOfMethodCallIgnored
        GENERATED_SCHEMA_DIR.mkdirs();

        VelocityGenerator.generate(SCHEMA_TEMPLATE_FILE, GENERATED_SCHEMA_FILE,
                Map.of("itemList", itemDefList));
    }

    public static class ItemDef {
        private final String name;
        private final String maxOccurs;
        private final boolean indexed;

        private ItemDef(String name, String maxOccurs, boolean indexed) {
            this.name = name;
            this.maxOccurs = maxOccurs;
            this.indexed = indexed;
        }

        public String getName() {
            return name;
        }

        public String getMaxOccurs() {
            return maxOccurs;
        }

        public boolean isIndexed() {
            return indexed;
        }
    }

    private class IndexedSelector {
        private int counter;
        private boolean getNext() {
            counter += indexedPercentage;
            if (counter >= 100) {
                counter -= 100;
                return true;
            } else {
                return false;
            }
        }

        public void reset() {
            counter = 0;
        }
    }
}
