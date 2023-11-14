package com.evolveum.midpoint.ninja.action.verify;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.VerifyResult;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.validator.*;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class VerificationReporter {

    public static final String DELTA_FILE_NAME_SUFFIX = ".delta.xml";

    public static final List<String> REPORT_HEADER = List.of(
            "Oid",
            "Type",
            "Name",
            "Status",
            "Item path",
            "Message",
            "Identifier",
            "Phase",
            "Priority",
            "Type",
            "Upgrade description",
            "Skip upgrade [yes/no] Default: no"
    );

    private static final int COLUMN_INDEX_OID = 0;
    private static final int COLUMN_INDEX_PATH = 4;
    private static final int COLUMN_INDEX_IDENTIFIER = 6;
    private static final int COLUMN_INDEX_SKIP_UPGRADE = REPORT_HEADER.size() - 1;

    public static final CSVFormat CSV_FORMAT;

    static {
        CSV_FORMAT = createCsvFormat();
    }

    private final VerifyOptions options;

    private final PrismContext prismContext;

    private final Charset charset;

    private final Log log;

    private ObjectUpgradeValidator validator;

    private final VerifyResult result = new VerifyResult();

    private boolean createDeltaFile;

    private Writer deltaWriter;

    public VerificationReporter(@NotNull VerifyOptions options, @NotNull PrismContext prismContext, @NotNull Charset charset, @NotNull Log log) {
        this.options = options;
        this.prismContext = prismContext;
        this.charset = charset;
        this.log = log;
    }

    public void setCreateDeltaFile(boolean createDeltaFile) {
        this.createDeltaFile = createDeltaFile;
    }

    public void destroy() {
        if (deltaWriter != null) {
            try {
                deltaWriter.write(NinjaUtils.XML_DELTAS_SUFFIX);
            } catch (IOException ex) {
                throw new NinjaException("Couldn't finish file for XML deltas", ex);
            }
            IOUtils.closeQuietly(deltaWriter);
        }
    }

    public void init() {
        result.setVerificationFile(options.getOutput());

        if (createDeltaFile) {
            initDeltaXmlFile();
        }

        validator = new ObjectUpgradeValidator(prismContext);

        validator.setWarnPlannedRemovalVersion(options.getPlannedRemovalVersion());

        List<VerifyOptions.VerificationCategory> categories = options.getVerificationCategories();
        if (categories.isEmpty()) {
            validator.showAllWarnings();
        } else {
            for (VerifyOptions.VerificationCategory category : categories) {
                switch (category) {
                    case DEPRECATED:
                        validator.setWarnDeprecated(true);
                        break;
                    case INCORRECT_OIDS:
                        validator.setWarnIncorrectOids(true);
                        break;
                    case PLANNED_REMOVAL:
                        validator.setWarnPlannedRemoval(true);
                        break;
                    case REMOVED:
                        validator.setWarnRemoved(true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown category " + category);
                }
            }
        }
    }

    private void initDeltaXmlFile() {
        if (options.getOutput() == null || !VerifyOptions.ReportStyle.CSV.equals(options.getReportStyle())) {
            return;
        }

        final File deltaFile = new File(options.getOutput() + DELTA_FILE_NAME_SUFFIX);

        try {
            if (deltaFile.exists()) {
                if (options.isOverwrite()) {
                    deltaFile.delete();
                } else {
                    throw new NinjaException("Export file for XML delta '" + deltaFile.getPath() + "' already exists");
                }
            }

            deltaFile.createNewFile();

            deltaWriter = new FileWriter(deltaFile, charset);
            deltaWriter.write(NinjaUtils.XML_DELTAS_PREFIX);
        } catch (IOException ex) {
            throw new NinjaException("Couldn't create file for XML deltas " + deltaFile.getPath(), ex);
        }
    }

    public String getProlog() {
        if (options.getReportStyle() == VerifyOptions.ReportStyle.CSV) {
            try {
                StringWriter writer = new StringWriter();
                CSVPrinter printer = CSV_FORMAT.print(writer);
                printer.printRecord(REPORT_HEADER);

                return writer.toString();
            } catch (IOException ex) {
                throw new IllegalStateException("Couldn't write CSV header", ex);
            }
        }

        return null;
    }

    public String getEpilog() {
        return null;
    }

    private static CSVFormat createCsvFormat() {
        return CSVFormat.Builder.create()
                .setDelimiter(';')
                .setEscape('\\')
                .setIgnoreHeaderCase(false)
                .setQuote('"')
                .setRecordSeparator('\n')
                .setQuoteMode(QuoteMode.ALL)
                .build();
    }

    public <T extends Objectable> UpgradeValidationResult verify(Writer writer, PrismObject<T> object) throws Exception {
        UpgradeValidationResult result = validator.validate((PrismObject) object);

        for (UpgradeValidationItem item : result.getItems()) {
            if (item.getPriority() == null) {
                this.result.incrementUnknownCount();
                continue;
            }

            this.result.incrementPriorityItemCount(item.getPriority());
        }

        switch (options.getReportStyle()) {
            case PLAIN:
                verifyAsPlain(writer, object, result);
                break;
            case CSV:
                verifyAsCsv(writer, object, result);
                break;
            default:
                throw new IllegalArgumentException("Unknown report style " + options.getReportStyle());
        }

        if (createDeltaFile) {
            writeDeltaXml(result);
        }

        return result;
    }

    private void writeDeltaXml(UpgradeValidationResult result) {
        for (UpgradeValidationItem item : result.getItems()) {
            if (item.getDelta() == null) {
                continue;
            }

            if (deltaWriter != null) {
                try {
                    ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(item.getDelta());
                    String xml = prismContext.xmlSerializer()
                            .serializeRealValue(deltaType, NinjaUtils.DELTA_LIST_DELTA);
                    deltaWriter.write(xml);
                } catch (SchemaException | IOException ex) {
                    log.error("Couldn't write object delta to XML file", ex);
                }
            }
        }
    }

    public static String getItemPathFromRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return "";
        }

        String path = record.get(COLUMN_INDEX_PATH);
        if (StringUtils.isBlank(path)) {
            return "";
        }

        return path.trim();
    }

    public static String getIdentifierFromRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return null;
        }

        return record.get(COLUMN_INDEX_IDENTIFIER);
    }

    public static UUID getUuidFromRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return null;
        }

        String oid = record.get(COLUMN_INDEX_OID);
        return StringUtils.isNotEmpty(oid) ? UUID.fromString(oid) : null;
    }

    public static boolean skipUpgradeForRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return true;
        }

        String value = record.get(COLUMN_INDEX_SKIP_UPGRADE);

        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("t")
                || value.equalsIgnoreCase("y");
    }

    private void verifyAsPlain(Writer writer, PrismObject<?> object, UpgradeValidationResult result) throws IOException {
        for (UpgradeValidationItem validationItem : result.getItems()) {
            writeValidationItem(writer, object, validationItem);
        }
    }

    private void verifyAsCsv(Writer writer, PrismObject<?> object, UpgradeValidationResult result) throws IOException {
        // this is not very nice/clean code, we're creating printer (Closeable), but not closing it, since that would close underlying writer
        CSVPrinter printer = CSV_FORMAT.print(writer);

        for (UpgradeValidationItem item : result.getItems()) {
            printer.printRecord(createReportRecord(item, object));
        }
    }

    private List<String> createReportRecord(UpgradeValidationItem item, PrismObject<?> object) {
        String identifier = item.getIdentifier();
        UpgradePhase phase = item.getPhase();
        UpgradePriority priority = item.getPriority();
        UpgradeType type = item.getType();
        String upgradeDescription = item.getDescription();

        // this array has to match {@link VerifyConsumerWorker#REPORT_HEADER}
        return Arrays.asList(object.getOid(),
                object.getDefinition().getTypeName().getLocalPart(),
                object.getBusinessDisplayName(),
                Objects.toString(item.getItem().getStatus()),
                Objects.toString(item.getItem().getItemPath()),
                item.getItem().getMessage() != null ? item.getItem().getMessage().getFallbackMessage() : null,
                identifier,
                phase != null ? phase.name() : null,
                priority != null ? priority.name() : null,
                type != null ? type.name() : null,
                upgradeDescription,
                null    // todo last column should have YES (skip upgrade for all non-auto changes by default)
        );
    }

    private void writeValidationItem(Writer writer, PrismObject<?> object, UpgradeValidationItem item) throws IOException {
        ValidationItem validationItem = item.getItem();

        List<Object> items = new ArrayList<>();

        if (validationItem.getStatus() != null) {
            items.add(validationItem.getStatus());
        } else {
            writer.append("INFO ");
        }

        UpgradePriority priority = item.getPriority();
        if (priority != null) {
            items.add(priority);
        }

        items.add(getObjectDisplayName(object));

        if (validationItem.getItemPath() != null) {
            items.add(validationItem.getItemPath());
        }

        String msg = writeMessage(validationItem.getMessage());
        if (msg != null) {
            items.add(msg);
        }

        writer.write(StringUtils.join(items, " "));
        writer.write("\n");
    }

    private String getObjectDisplayName(PrismObject<?> object) {
        StringBuilder sb = new StringBuilder();
        sb.append(object.getName());
        sb.append(" (");
        sb.append(object.getOid());
        sb.append(", ");
        sb.append(object.getCompileTimeClass().getSimpleName());
        sb.append(")");

        return sb.toString();
    }

    private String writeMessage(LocalizableMessage message) {
        if (message == null) {
            return null;
        }
        // TODO: localization?
        return message.getFallbackMessage();
    }

    public VerifyResult getResult() {
        return result;
    }
}
