package com.evolveum.midpoint.ninja.action.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.upgrade.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.LocalizableMessage;

public class VerificationReporter {

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
            "Skip upgrade [yes/no]"
    );

    private final VerifyOptions options;

    private final PrismContext prismContext;

    private ObjectValidator validator;

    public static final CSVFormat CSV_FORMAT;

    static {
        CSV_FORMAT = createCsvFormat();
    }

    public VerificationReporter(@NotNull VerifyOptions options, @NotNull PrismContext prismContext) {
        this.options = options;
        this.prismContext = prismContext;

        init();
    }

    private void init() {
        validator = new ObjectValidator(prismContext);

        List<VerifyOptions.VerificationCategory> categories = options.getVerificationCategories();
        if (categories.isEmpty()) {
            validator.setAllWarnings();
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
                    default:
                        throw new IllegalArgumentException("Unknown category " + category);
                }
            }
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

    public void verify(Writer writer, PrismObject<?> object) throws IOException {
        ValidationResult result = validator.validate(object);

        UpgradeObjectsHandler handler = new UpgradeObjectsHandler();
        VerificationResult verificationResult = handler.verify(object, result);

        // todo handle merge result with upgrade result, doesn't look nice

        switch (options.getReportStyle()) {
            case PLAIN:
                verifyAsPlain(writer, object, verificationResult);
                break;
            case CSV:
                verifyAsCsv(writer, object, verificationResult);
                break;
            default:
                throw new IllegalArgumentException("Unknown report style " + options.getReportStyle());
        }
    }

    public static String getIdentifierFromRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return null;
        }

        return record.get(6);
    }

    public static UUID getUuidFromRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return null;
        }

        String uuid = record.get(0);
        return StringUtils.isNotEmpty(uuid) ? UUID.fromString(uuid) : null;
    }

    public static boolean skipUpgradeForRecord(CSVRecord record) {
        if (record == null || record.size() != REPORT_HEADER.size()) {
            return true;
        }

        String value = record.get(REPORT_HEADER.size() - 1);

        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("t")
                || value.equalsIgnoreCase("y");
    }

    private void verifyAsPlain(Writer writer, PrismObject<?> object, VerificationResult result) throws IOException {
        for (VerificationResultItem validationItem : result.getItems()) {
            writeValidationItem(writer, object, validationItem);
        }
    }

    private void verifyAsCsv(Writer writer, PrismObject<?> object, VerificationResult result) throws IOException {
        // this is not very nice/clean code, we're creating printer (Closeable), but not closing it, since that would close underlying writer
        CSVPrinter printer = CSV_FORMAT.print(writer);

        for (VerificationResultItem item : result.getItems()) {
            printer.printRecord(createReportRecord(item, object));
        }
    }

    private List<String> createReportRecord(VerificationResultItem item, PrismObject<?> object) {
        // todo populate
        String identifier = item.getIdentifier();
        UpgradePhase phase = item.getPhase();
        UpgradePriority priority = item.getPriority();
        UpgradeType type = item.getType();

        // this array has to match {@link VerifyConsumerWorker#REPORT_HEADER}
        return Arrays.asList(object.getOid(),
                object.getDefinition().getTypeName().getLocalPart(),
                object.getBusinessDisplayName(),
                Objects.toString(item.getStatus()),
                Objects.toString(item.getItemPath()),
                item.getMessage() != null ? item.getMessage().getFallbackMessage() : null,
                identifier,
                phase != null ? phase.name() : null,
                priority != null ? priority.name() : null,
                type != null ? type.name() : null,
                null    // todo last column should have YES (skip upgrade for all non-auto changes by default)
        );
    }

    private void writeValidationItem(Writer writer, PrismObject<?> object, VerificationResultItem validationItem) throws IOException {
        if (validationItem.getStatus() != null) {
            writer.append(validationItem.getStatus().toString());
            writer.append(" ");
        } else {
            writer.append("INFO ");
        }
        writer.append(object.toString());
        writer.append(" ");
        if (validationItem.getItemPath() != null) {
            writer.append(validationItem.getItemPath().toString());
            writer.append(" ");
        }
        writeMessage(writer, validationItem.getMessage());
        writer.append("\n");
    }

    private void writeMessage(Writer writer, LocalizableMessage message) throws IOException {
        if (message == null) {
            return;
        }
        // TODO: localization?
        writer.append(message.getFallbackMessage());
    }
}
