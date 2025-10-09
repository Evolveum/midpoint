package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Consumer that writes each exported object into its own XML file.
 */
public class ExportPerObjectWorker extends BaseWorker<ExportOptions, ObjectType> {

    private final PrismSerializer<String> serializer;
    private final Path outputDir;

    public ExportPerObjectWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);

        this.serializer = context.getPrismContext()
                .xmlSerializer()
                .options(SerializationOptions.createSerializeForExport().skipContainerIds(options.isSkipContainerIds()));

        this.outputDir = resolveAndPrepareOutputDir();

        if (options.isZip()) {
            context.getLog().warn("Ignoring --zip for split-files mode; writing plain XML files into {}", outputDir);
        }
    }

    @Override
    public void run() {
        try {
            while (!shouldConsumerStop()) {
                ObjectType obj = null;
                try {
                    obj = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (obj == null) {
                        continue;
                    }

                    Path target = buildTargetPath(obj);

                    if (Files.exists(target) && !options.isOverwrite()) {
                        context.getLog().warn("File {} exists and --overwrite not set; skipping", target);
                        operation.incrementError();
                        continue;
                    }

                    Files.createDirectories(target.getParent());
                    String xml = serializer.serialize(obj.asPrismObject());

                    try (BufferedWriter writer = Files.newBufferedWriter(
                            target, context.getCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        writer.write(xml);
                    }

                    operation.incrementTotal();
                } catch (Exception ex) {
                    Throwable cause = ExceptionUtil.findRootCause(ex);
                    context.getLog().error("Couldn't export object {}, reason: {}", ex, obj, cause != null ? cause.getMessage() : ex.getMessage());
                    operation.incrementError();
                }
            }
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }

    private Path resolveAndPrepareOutputDir() {
        if (options.getOutput() == null) {
            throw new NinjaException("Split-files export requires --output to specify a directory");
        }

        Path dir = options.getOutput().toPath();
        try {
            if (Files.exists(dir) && !Files.isDirectory(dir)) {
                throw new NinjaException("--output must be a directory when using --split-files: " + dir);
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new NinjaException("Couldn't prepare output directory '" + dir + "': " + e.getMessage(), e);
        }
        return dir;
    }

    private Path buildTargetPath(ObjectType obj) {
        String type = obj.asPrismObject().getCompileTimeClass().getSimpleName();
        String oidPart = obj.getOid() != null ? obj.getOid() : String.valueOf(System.currentTimeMillis());
        String name = obj.getName() != null && obj.getName().getOrig() != null ? obj.getName().getOrig() : "noname";

        String safeName = sanitizeForFilename(name);
        String filename = String.format(Locale.ROOT, "%s-%s.xml", oidPart, safeName);
        return outputDir.resolve(type).resolve(filename);
    }

    private String sanitizeForFilename(String input) {
        String s = input.trim();
        // Replace non-alphanumeric with underscore, collapse repeats, and trim length conservatively
        s = s.replaceAll("[^A-Za-z0-9._-]", "_");
        s = s.replaceAll("_+", "_");
        if (s.length() > 80) {
            s = s.substring(0, 80);
        }
        if (s.isEmpty()) {
            s = "noname";
        }
        return s;
    }
}
