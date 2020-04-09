/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Test that initial objects are parseable, correct, that they are not deprecated and so on.
 *
 * @author Radovan Semancik
 */
public class TestInitialObjects extends AbstractGuiUnitTest {

    private static final File DIR_INITIAL_OBJECTS = new File("src/main/resources/initial-objects");

    @Test
    public void testInitialObjects() throws Exception {
        ObjectValidator validator = new ObjectValidator(getPrismContext());
        validator.setAllWarnings();

        StringBuilder errorsSb = new StringBuilder();

        for (File file : DIR_INITIAL_OBJECTS.listFiles()) {
            if (file.isFile()) {
                try {
                    testInitialObject(validator, errorsSb, file);
                } catch (Throwable e) {
                    String msg = "Error processing file "+file.getName()+": "+e.getMessage();
                    logger.error(msg, e);
                    displayException(msg, e);
                    throw e;
                }
            }
        }

        if (errorsSb.length() != 0) {
            throw new SchemaException(errorsSb.toString());
        }
    }

    private <O extends ObjectType> void testInitialObject(ObjectValidator validator, StringBuilder errorsSb, File file) throws SchemaException, IOException {
        PrismObject<O> object = getPrismContext().parseObject(file);
        ValidationResult validationResult = validator.validate(object);
        if (validationResult.isEmpty()) {
            display("Checked "+object+": no warnings");
            return;
        }
        displayDumpable("Validation warnings for "+object, validationResult);
        for (ValidationItem valItem : validationResult.getItems()) {
            errorsSb.append(file.getName());
            errorsSb.append(" ");
            errorsSb.append(object);
            errorsSb.append(" ");
            valItem.shortDump(errorsSb);
            errorsSb.append("\n");
        }
    }
}
