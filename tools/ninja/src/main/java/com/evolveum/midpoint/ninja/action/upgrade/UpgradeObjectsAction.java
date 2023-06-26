package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

// todo handle initial objects somehow
// compare vanilla previous with vanilla new ones and also vanilla previous with current in MP repository,
// apply only non conflicting delta items, report it to user
public class UpgradeObjectsAction extends AbstractRepositorySearchAction<UpgradeObjectsOptions, Void> {

    private Set<UUID> skipUpgradeForUUIDs;

    @Override
    public Void execute() throws Exception {
        skipUpgradeForUUIDs = loadVerificationFile();

        log.info("Upgrade will skip {} objects", skipUpgradeForUUIDs.size());

        return super.execute();
//
//
////        final VerifyResult verifyResult = context.getResult(VerifyResult.class);
////
////        final File output = verifyResult.getOutput();
//
//        // todo load CSV, only OIDs + state (whether to update)
//        // go through all oids that need to be updated
//        // if csv not available go through all
//
//        Set<Class<?>> classes = ClassPathUtil.listClasses(Main.class.getPackageName());
//        Set<Class<?>> processors = classes.stream()
//                .filter(UpgradeObjectProcessor.class::isAssignableFrom)
//                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
//                .collect(Collectors.toUnmodifiableSet());
//
//        context.out.println("Found " + processors.size() + " upgrade rules");
//
//        return null;
    }

    @Override
    public String getOperationName() {
        return "upgrade";
    }

    private Set<UUID> loadVerificationFile() throws IOException {
        File verification = options.getVerification();
        if (verification == null || !verification.exists() || !verification.isFile()) {
            return Collections.emptySet();
        }

        log.info("Loading verification file");

        Set<UUID> set = new HashSet<>();

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
                    if (uuid != null) {
                        set.add(uuid);
                    }
                }
            }
        }

        return Collections.unmodifiableSet(set);
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
    protected Runnable createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return new UpgradeObjectsConsumerWorker(skipUpgradeForUUIDs, context, options, queue, operation);
    }
}
