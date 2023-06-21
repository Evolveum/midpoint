package com.evolveum.midpoint.ninja.action.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.Main;
import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeObjectProcessor;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradePhase;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradePriority;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.LocalizableMessage;

public class VerificationReporter {

    private static final List<String> REPORT_HEADER = List.of(
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
            "Perform upgrade [yes/no]"
    );

    private final VerifyOptions options;

    private final PrismContext prismContext;

    private ObjectValidator validator;

    private static final CSVFormat CSV_FORMAT;

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

        enhanceValidationResult(object, result);

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
    }

    private void enhanceValidationResult(PrismObject<?> object, ValidationResult result) {
        Set<Class<?>> processors = ClassPathUtil.listClasses(Main.class.getPackageName())
                .stream()
                .filter(UpgradeObjectProcessor.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect(Collectors.toUnmodifiableSet());

        Set<UpgradeObjectProcessor<?>> instances = processors.stream()
                .map(c -> {
                    try {
                        return (UpgradeObjectProcessor<?>) c.getConstructor().newInstance();
                    } catch (Exception ex) {
                        // todo
                        ex.printStackTrace();

                        return null;
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toUnmodifiableSet());

        for (ValidationItem validationItem : result.getItems()) {
            for (UpgradeObjectProcessor<?> processor : instances) {
                if (processor.isApplicable(object, validationItem.getItemPath())) {
                    // todo finish
                }
            }
        }
    }

    private void verifyAsPlain(Writer writer, PrismObject<?> object, ValidationResult result) throws IOException {
        for (ValidationItem validationItem : result.getItems()) {
            writeValidationItem(writer, object, validationItem);
        }
    }

    private void verifyAsCsv(Writer writer, PrismObject<?> object, ValidationResult result) throws IOException {
        // this is not very nice/clean code, we're creating printer (Closeable), but not closing it, since that would close underlying writer
        CSVPrinter printer = CSV_FORMAT.print(writer);

        for (ValidationItem item : result.getItems()) {
            printer.printRecord(createReportRecord(item, object));
        }
    }

    private List<String> createReportRecord(ValidationItem item, PrismObject<?> object) {
        // todo populate
        String identifier = null;
        UpgradePhase phase = null;
        UpgradePriority priority = null;
        UpgradeType type = null;

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
                null
        );
    }

    private void writeValidationItem(Writer writer, PrismObject<?> object, ValidationItem validationItem) throws IOException {
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
