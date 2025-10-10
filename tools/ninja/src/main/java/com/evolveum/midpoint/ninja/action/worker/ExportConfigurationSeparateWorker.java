package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

public class ExportConfigurationSeparateWorker extends ExportConfigurationWorker {

    private final PrismSerializer<String> serializer;
    private final Path outputDir;

    public ExportConfigurationSeparateWorker(NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue, OperationStatus operation) {
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
    protected void init() {
        this.options = this.options.setOutput(null);
    }

    @Override
    protected String getProlog() {
        return null;
    }

    @Override
    protected String getEpilog() {
        return null;
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws SchemaException, IOException {
        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        if (shouldSkipObject(prismObject)) {
            return;
        }

        Path target = buildTargetPath(object);

        editObject(prismObject);

        @NotNull List<ItemPath> itemsPaths = getExcludeItemsPaths();
        if (!itemsPaths.isEmpty()) {
            prismObject.getValue().removePaths(itemsPaths);
        }

        if (Files.exists(target) && !options.isOverwrite()) {
            context.getLog().warn("File {} exists and --overwrite not set; skipping", target);
            operation.incrementError();
            return;
        }

        Files.createDirectories(target.getParent());
        String xml = serializer.serialize(prismObject);

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                target, context.getCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bufferedWriter.write(xml);
        }
    }

    private Path resolveAndPrepareOutputDir() {
        Path dir;

        if (options.getOutput() == null) {
            context.getLog().warn("Output directory not specified, using default: /tmp/export");
            dir = Path.of("/tmp/export-midpoint-config");
        } else {
            dir = options.getOutput().toPath();
        }

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
        String filename = String.format(Locale.ROOT, "%s-%s.xml", safeName, oidPart);
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
