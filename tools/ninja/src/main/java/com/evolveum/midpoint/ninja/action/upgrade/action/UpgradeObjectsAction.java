package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeObjectsConsumerWorker;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

// todo handle initial objects somehow
// compare vanilla previous with vanilla new ones and also vanilla previous with current in MP repository,
// apply only non conflicting delta items, report it to user
public class UpgradeObjectsAction extends AbstractRepositorySearchAction<UpgradeObjectsOptions, Void> {

    private Map<UUID, Set<String>> skipUpgradeForOids;

    @Override
    public Void execute() throws Exception {
        skipUpgradeForOids = loadVerificationFile();

        log.info("Upgrade will skip {} objects", skipUpgradeForOids.size());
        if (!options.getFiles().isEmpty()) {
            return upgradeObjectsInFiles();
        }

        return super.execute();
    }

    private Void upgradeObjectsInFiles() {
        for (File file : options.getFiles()) {
            if (!file.isDirectory()) {
                upgradeFile(file);
            } else {
                Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                for (File child : children) {
                    if (child.isDirectory()) {
                        continue;
                    }

                    upgradeFile(child);
                }
            }
        }
        // todo implement
        return null;
    }

    private void upgradeFile(File file) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        PrismSerializer<String> serializer = prismContext.xmlSerializer();
//        serializer.serializeAnyData()
//        try (Writer writer = new FileWriter(file)) {
//            List<PrismObject<?>> objects = parser.parseObjects();
//            for (PrismObject<?> object : objects) {
//                UpgradeObjectsHandler upgradeHandler = new UpgradeObjectsHandler();
//                UpgradeObjectResult result = upgradeHandler.handle(prismObject);
//
//                if (result.isChanged()) {
//                    ObjectDelta<?> delta = result.getDelta();
//                    if (delta != null && !delta.isEmpty()) {
//                        delta.applyTo(object);
//                    }
//                }
//
//            }
//        } catch (Exception ex) {
//            // todo handle error
//            ex.printStackTrace();
//        }
        // todo implement
    }

    @Override
    public String getOperationName() {
        return "upgrade objects";
    }

    private Map<UUID, Set<String>> loadVerificationFile() throws IOException {
        File verification = options.getVerification();
        if (verification == null || !verification.exists() || !verification.isFile()) {
            return Collections.emptyMap();
        }

        log.info("Loading verification file");

        Map<UUID, Set<String>> map = new HashMap<>();

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
                    String identifier = VerificationReporter.getIdentifierFromRecord(record);
                    if (uuid != null) {
                        Set<String> identifiers = map.get(uuid);
                        if (identifiers == null) {
                            identifiers = new HashSet<>();
                            map.put(uuid, identifiers);
                        }
                        identifiers.add(identifier);
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
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            new UpgradeObjectsConsumerWorker(skipUpgradeForOids, context, options, queue, operation).run();
            return null;
        };
    }
}
