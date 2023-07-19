/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.action.worker.VerifyConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class VerifyAction extends AbstractRepositorySearchAction<VerifyOptions, VerifyResult> {

    @Override
    public String getOperationName() {
        return "verify";
    }

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        VerifyOptions options = NinjaUtils.getOptions(allOptions, VerifyOptions.class);
        if (options != null && !options.getFiles().isEmpty()) {
            return NinjaApplicationContextLevel.NO_REPOSITORY;
        }

        return super.getApplicationContextLevel(allOptions);
    }

    @Override
    protected Callable<VerifyResult> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            VerifyConsumerWorker worker = new VerifyConsumerWorker(context, options, queue, operation);
            worker.run();

            return worker.getResult();
        };
    }

    @Override
    public VerifyResult execute() throws Exception {
        if (!options.getFiles().isEmpty()) {
            return verifyFiles();
        }

        return super.execute();
    }

    private VerifyResult verifyFiles() throws IOException {
        VerificationReporter reporter = new VerificationReporter(options, context.getPrismContext(), context.getCharset(), log);
        reporter.setCreateDeltaFile(true);

        try (Writer writer = NinjaUtils.createWriter(
                options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out)) {

            reporter.init();

            String prolog = reporter.getProlog();
            if (prolog != null) {
                writer.write(prolog);
            }

            for (File file : options.getFiles()) {
                if (!file.isDirectory()) {
                    verifyFile(file, reporter, writer);
                } else {
                    Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                    for (File child : children) {
                        if (child.isDirectory()) {
                            continue;
                        }

                        verifyFile(child, reporter, writer);
                    }
                }
            }

            String epilog = reporter.getEpilog();
            if (epilog != null) {
                writer.write(epilog);
            }
        } finally {
            reporter.destroy();
        }

        return reporter.getResult();
    }

    private void verifyFile(File file, VerificationReporter reporter, Writer writer) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        try {
            List<PrismObject<? extends Objectable>> objects = parser.parseObjects();
            for (PrismObject<? extends Objectable> object : objects) {
                reporter.verify(writer, object);
            }
        } catch (Exception ex) {
            log.error("Couldn't verify file '{}'", ex, file.getPath());
        }
    }
}
