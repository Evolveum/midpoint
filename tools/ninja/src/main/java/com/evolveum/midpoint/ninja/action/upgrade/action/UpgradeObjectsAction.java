package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.action.upgrade.*;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeObjectsAction extends AbstractRepositorySearchAction<UpgradeObjectsOptions, ActionResult<UpgradeObjectsItemsSummary>> {

    private Map<UUID, Set<SkipUpgradeItem>> skipUpgradeItems;

    @Override
    public LogTarget getLogTarget() {
        return LogTarget.SYSTEM_OUT;
    }

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        UpgradeObjectsOptions opts = NinjaUtils.getOptions(allOptions, UpgradeObjectsOptions.class);
        if (opts != null && !opts.getFiles().isEmpty()) {
            return NinjaApplicationContextLevel.NO_REPOSITORY;
        }

        return super.getApplicationContextLevel(allOptions);
    }

    @Override
    public ActionResult<UpgradeObjectsItemsSummary> execute() throws Exception {
        skipUpgradeItems = loadVerificationFile();

        log.info("Upgrade will skip {} objects", skipUpgradeItems.size());

        if (!options.getFiles().isEmpty()) {
            if (!options.isSkipUpgradeWarning()) {
                log.warn("File update will remove XML comments and change formatting. Do you wish to proceed? (Y/n)");
                String result = NinjaUtils.readInput(input -> StringUtils.isEmpty(input) || input.equalsIgnoreCase("y"));

                if (result.trim().equalsIgnoreCase("n")) {
                    log.info("Upgrade aborted");
                    return null;
                }
            }

            return upgradeObjectsInFiles();
        }

        return super.execute();
    }

    private ActionResult<UpgradeObjectsItemsSummary> upgradeObjectsInFiles() {
        UpgradeObjectsItemsSummary summary = new UpgradeObjectsItemsSummary();

        for (File file : options.getFiles()) {
            if (!file.isDirectory()) {
                upgradeFile(file, summary);
            } else {
                Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                for (File child : children) {
                    if (child.isDirectory()) {
                        continue;
                    }

                    upgradeFile(child, summary);
                }
            }
        }

        return new ActionResult<>(summary);
    }

    private void upgradeFile(File file, UpgradeObjectsItemsSummary summary) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        List<PrismObject<?>> objects;
        try {
            objects = parser.parseObjects();
        } catch (Exception ex) {
            log.error("Couldn't parse file '{}'", ex, file.getPath());
            return;
        }

        boolean changed = false;
        try {
            UpgradeObjectHandler executor = new UpgradeObjectHandler(options, context, skipUpgradeItems, summary);
            for (PrismObject object : objects) {
                UpgradeObjectResult result = executor.execute(object);
                if (result == UpgradeObjectResult.UPDATED) {
                    changed = true;
                }
            }
        } catch (Exception ex) {
            log.error("Couldn't update file '{}'", ex, file.getPath());
        }

        if (!changed) {
            return;
        }

        try (Writer writer = new FileWriter(file)) {
            PrismSerializer<String> serializer = prismContext.xmlSerializer();
            String xml;
            if (objects.size() > 1) {
                xml = serializer.serializeObjects(objects);
            } else {
                // will get cleaner xml without "objects" element
                xml = serializer.serialize(objects.get(0));
            }
            writer.write(xml);
        } catch (Exception ex) {
            log.error("Couldn't serialize objects to file '{}'", ex, file.getPath());
        }
    }

    @Override
    public String getOperationName() {
        return "upgrade objects";
    }

    private Map<UUID, Set<SkipUpgradeItem>> loadVerificationFile() throws IOException {
        File verification = options.getVerification();
        if (verification == null || !verification.exists() || !verification.isFile()) {
            // FIXME: Add log explanation, what is happening
            if (context.isUserMode()) {
                log.warn("Upgrade objects started without verification report, all neccessary non-manual changes will be accepted.");
            }
            return Collections.emptyMap();
        }

        log.info("Loading verification file");

        Map<UUID, Set<SkipUpgradeItem>> map = new HashMap<>();

        CSVFormat format = VerificationReporter.CSV_FORMAT;
        try (CSVParser parser = format.parse(new FileReader(verification, context.getCharset()))) {
            Iterator<CSVRecord> iterator = parser.iterator();
            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                if (record.getRecordNumber() == 1 || isRecordEmpty(record)) {
                    // csv header or empty record
                    continue;
                }

                if (VerificationReporter.skipUpgradeForRecord(record)) {
                    UUID uuid = VerificationReporter.getUuidFromRecord(record);
                    String path = VerificationReporter.getItemPathFromRecord(record);
                    String identifier = VerificationReporter.getIdentifierFromRecord(record);
                    if (uuid != null) {
                        Set<SkipUpgradeItem> identifiers = map.get(uuid);
                        if (identifiers == null) {
                            identifiers = new HashSet<>();
                            map.put(uuid, identifiers);
                        }
                        identifiers.add(new SkipUpgradeItem(path, identifier));
                    }
                }
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private boolean isRecordEmpty(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            String value = record.get(i);
            if (StringUtils.isNotBlank(value)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected Callable<ActionResult<UpgradeObjectsItemsSummary>> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            UpgradeObjectsConsumerWorker<?> worker = new UpgradeObjectsConsumerWorker<>(skipUpgradeItems, context, options, queue, operation);
            worker.run();

            UpgradeObjectsItemsSummary summary = worker.getItemsSummary();

            return new ActionResult<>(summary);
        };
    }

    protected void handleResultOnFinish(ActionResult<UpgradeObjectsItemsSummary> consumerResult, OperationStatus operation, String finishMessage) {
        super.handleResultOnFinish(consumerResult, operation, finishMessage);

        log.info("");
        log.info("Upgrade objects finished.");
        logSummary(consumerResult.result(), UpgradeObjectsItemsSummary.ItemStatus.PROCESSED);
        logSummary(consumerResult.result(), UpgradeObjectsItemsSummary.ItemStatus.SKIPPED);

        OperationResult result = operation.getResult();
        if (result.isAcceptable() && context.isUserMode()) {
            log.info("");
            log.info("If you want to continue to continue with upgrade, please run: ninja.sh upgrade-distribution.");
        }
    }

    private void logSummary(UpgradeObjectsItemsSummary summary, UpgradeObjectsItemsSummary.ItemStatus status) {
        int critical = summary.get(UpgradePriority.CRITICAL, status);
        int necessary = summary.get(UpgradePriority.NECESSARY, status);
        int optional = summary.get(UpgradePriority.OPTIONAL, status);
        int unknown = summary.get(null, status);

        log.info(
                "\t {}, {}, {} and {} unknown verification items {}, total {}",
                ConsoleFormat.formatMessageWithErrorParameters("{} critical", critical),
                ConsoleFormat.formatMessageWithWarningParameters("{} necessary", necessary),
                ConsoleFormat.formatMessageWithInfoParameters("{} optional", optional),
                unknown,
                status.label,
                critical + necessary + optional
        );
    }
}
