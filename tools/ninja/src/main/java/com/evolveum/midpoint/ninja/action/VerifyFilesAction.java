package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;

public class VerifyFilesAction extends Action<VerifyFilesOptions, Void> {

    @Override
    public String getOperationName() {
        return "verify files";
    }

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        return NinjaApplicationContextLevel.NO_REPOSITORY;
    }

    @Override
    public LogTarget getLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    @Override
    public Void execute() throws Exception {
        VerificationReporter reporter = new VerificationReporter(options, context.getPrismContext());

        try (Writer writer = NinjaUtils.createWriter(
                options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out)) {

            String prolog = reporter.getProlog();
            if (prolog != null) {
                writer.write(prolog);
            }

            for (File file : options.getFiles()) {
                if (!file.isDirectory()) {
                    validateFile(file, reporter, writer);
                } else {
                    Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                    for (File child : children) {
                        if (child.isDirectory()) {
                            continue;
                        }

                        validateFile(child, reporter, writer);
                    }
                }
            }

            String epilog = reporter.getEpilog();
            if (epilog != null) {
                writer.write(epilog);
            }
        }

        return null;
    }

    private void validateFile(File file, VerificationReporter reporter, Writer writer) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        try {
            List<PrismObject<?>> objects = parser.parseObjects();
            for (PrismObject<?> object : objects) {
                reporter.verify(writer, object);
            }
        } catch (Exception ex) {
            // todo handle error
            ex.printStackTrace();
        }
    }
}
