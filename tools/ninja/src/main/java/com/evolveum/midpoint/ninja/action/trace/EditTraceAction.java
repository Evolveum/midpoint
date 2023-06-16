/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.trace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.RepositoryAction;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.schema.traces.TraceParser;
import com.evolveum.midpoint.schema.traces.TraceWriter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

/**
 * TODO
 */
public class EditTraceAction extends RepositoryAction<EditTraceOptions, Void> {

    private static final String DEFAULT_OUTPUT = "output.zip";

    private int killed;

    @Override
    @NotNull
    public NinjaApplicationContextLevel getApplicationContextLevel() {
        return NinjaApplicationContextLevel.NO_REPOSITORY;
    }

    @Override
    public Void execute() throws Exception {
        TracingOutputType trace = parseInput();
        if (options.isPrintStat() || options.isPrintStatExtra()) {
            printStatistics(trace);
        }

        if (CollectionUtils.isNotEmpty(options.getKeep()) || CollectionUtils.isNotEmpty(options.getKill())) {
            applyKeep(trace);
            applyKill(trace);
            writeTrace(trace);
        }

        return null;
    }

    private TracingOutputType parseInput() throws IOException, SchemaException {
        String inputFile = options.getInput();
        log.info("Starting parsing input file: {}", inputFile);

        long start = System.currentTimeMillis();
        TraceParser parser = new TraceParser(context.getPrismContext());
        TracingOutputType trace = parser.parse(new File(inputFile), true);

        log.info("Parsing finished; in {} seconds", (System.currentTimeMillis() - start) / 1000);
        return trace;
    }

    private void printStatistics(TracingOutputType trace) {
        TraceStatistics statistics = options.isPrintStatExtra() ? TraceStatistics.extra(trace) : TraceStatistics.simple(trace);
        log.info("Trace statistics:\n{}", statistics.dump(TraceStatistics.SortBy.SIZE));
    }

    private void applyKeep(TracingOutputType trace) {
        List<Pattern> patterns = getPatterns(options.getKeep());
        if (!patterns.isEmpty()) {
            List<OperationResultType> matchingChildren = new ArrayList<>();
            applyKeep(trace.getResult(), matchingChildren, patterns);

            log.info("Keeping {} matching nodes", matchingChildren.size());
            trace.getResult().getPartialResults().clear();
            trace.getResult().getPartialResults().addAll(matchingChildren);
        }
    }

    private void applyKeep(OperationResultType result, List<OperationResultType> matchingChildren, List<Pattern> patterns) {
        for (OperationResultType child : result.getPartialResults()) {
            if (matches(child, patterns)) {
                matchingChildren.add(child);
            } else {
                applyKeep(child, matchingChildren, patterns);
            }
        }
    }

    private void applyKill(TracingOutputType trace) {
        List<Pattern> patterns = getPatterns(options.getKill());
        if (!patterns.isEmpty()) {
            applyKill(trace.getResult(), patterns);
            log.info("Killed {} nodes", killed);
        }
    }

    private void applyKill(OperationResultType result, List<Pattern> patterns) {
        Iterator<OperationResultType> iterator = result.getPartialResults().iterator();
        while (iterator.hasNext()) {
            OperationResultType child = iterator.next();
            if (matches(child, patterns)) {
                iterator.remove();
                killed++;
            } else {
                applyKill(child, patterns);
            }
        }
    }

    private boolean matches(OperationResultType result, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(result.getOperation()).matches()) {
                return true;
            }
        }
        return false;
    }

    private List<Pattern> getPatterns(List<String> templates) {
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String template : templates) {
            String regex = toRegex(template);
            compiledPatterns.add(Pattern.compile(regex));
        }
        return compiledPatterns;
    }

    private String toRegex(String template) {
        return template.replace(".", "\\.").replace("*", ".*");
    }

    private void writeTrace(TracingOutputType trace) throws SchemaException, IOException {
        String output = ObjectUtils.defaultIfNull(options.getOutput(), DEFAULT_OUTPUT);
        log.info("Starting writing trace to {}", output);
        new TraceWriter(context.getPrismContext()).writeTrace(trace, new File(output), true);
        log.info("Trace written.");
    }

}
