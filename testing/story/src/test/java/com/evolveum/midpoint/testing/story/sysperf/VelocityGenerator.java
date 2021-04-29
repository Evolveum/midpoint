/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.evolveum.midpoint.util.exception.SystemException;

public class VelocityGenerator {

    public static void generate(File templateFile, File outputFile, Map<String, Object> parameters) {

        VelocityContext ctx = new VelocityContext();
        parameters.forEach(ctx::put);
        try {
            try (FileWriter writer = new FileWriter(outputFile);
                    FileReader reader = new FileReader(templateFile)) {
                Velocity.evaluate(ctx, writer, "", reader);
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }
}
