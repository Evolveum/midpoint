/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.action.worker.VerifyConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class VerifyAction extends AbstractRepositorySearchAction<VerifyOptions, VerifyResult> {

    public VerifyAction() {
    }

    public VerifyAction(boolean partial) {
        super(partial);
    }

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
        VerifyResult result;
        if (options.getOutput() != null) {
            log.info("Verification report will be saved to '{}'", options.getOutput().getPath());
        } else if (context.isUserMode()) {
            log.warn("Consider using  '-o verify-output.csv' option for CSV output with upgradeability status of deprecated items.");
            log.warn("It is recommended to review this report and actions for proper upgrade procedure.");
        }
        if (!options.getFiles().isEmpty()) {
            result = verifyFiles();
        } else {
            result = super.execute();
        }

        log.info("");
        log.info(
                "Verification finished. {}, {}, {} and {} unknown issues found.",
                ConsoleFormat.formatMessageWithErrorParameters("{} critical", result.getCriticalCount()),
                ConsoleFormat.formatMessageWithWarningParameters("{} necessary", result.getNecessaryCount()),
                ConsoleFormat.formatMessageWithInfoParameters("{} optional", result.getOptionalCount()),
                result.getUnknownCount());

        if (options.getOutput() != null) {
            log.info("");
            log.info("Verification report saved to '{}'", options.getOutput().getPath());

            if (Objects.equals(VerifyOptions.ReportStyle.CSV, options.getReportStyle())) {
                log.info("XML dump with delta for each item saved to '{}'", options.getOutput().getPath() + VerificationReporter.DELTA_FILE_NAME_SUFFIX);
            }

            // todo this should not show when action is a part of complex action and next step is prepared automatically
            // FIXME: ADD links (do not display in batch mode)
            // FIXME: Could We could try to infer script name?
            if (context.isUserMode() && !partial) {
                log.info("");
                log.info("Please see documentation for use of verification report in upgrade process and modify it accordingly.");
                log.info("After you've reviewed verification report and marked changes to skip you can continue upgrade process "
                        + "with running 'ninja.sh upgrade-objects --verification-file \"{}\"'", options.getOutput().getPath());
            }
        }

        return result;
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
                    if (!verifyFile(file, reporter, writer)) {
                        break;
                    }
                } else {
                    Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                    for (File child : children) {
                        if (child.isDirectory()) {
                            continue;
                        }

                        if (!verifyFile(child, reporter, writer)) {
                            break;
                        }
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

    private boolean verifyFile(File file, VerificationReporter reporter, Writer writer) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        boolean shouldContinue = true;
        try {
            List<PrismObject<? extends Objectable>> objects = parser.parseObjects();
            for (PrismObject<? extends Objectable> object : objects) {
                UpgradeValidationResult result = reporter.verify(writer, object);
                if (options.isStopOnCriticalError() && result.hasCritical()) {
                    shouldContinue = false;
                }
            }
        } catch (Exception ex) {
            log.error("Couldn't verify file '{}'", ex, file.getPath());
        }

        return shouldContinue;
    }
}
