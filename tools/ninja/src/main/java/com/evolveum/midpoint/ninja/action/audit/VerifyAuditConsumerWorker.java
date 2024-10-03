/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.action.worker.AbstractWriterConsumerWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * Consumer writing exported audit events to the writer (stdout or file).
 */
public class VerifyAuditConsumerWorker
        extends AbstractWriterConsumerWorker<VerifyAuditOptions, AuditEventRecordType> {

    private static final CSVFormat CSV_FORMAT = createCsvFormat();

    public static final List<String> REPORT_HEADER = List.of(
            "Repo Id",
            "Timestamp",
            "Status",
            "Item path",
            "Message"
    );
    private long recordsWithIssue = 0;
    private long unknownCount = 0;
    private long errorCount = 0;
    private long warningCount = 0;


    public VerifyAuditConsumerWorker(NinjaContext context,
            VerifyAuditOptions options, BlockingQueue<AuditEventRecordType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    AuditRecordValidator validator;

    @Override
    protected void init() {
        var objValidator = new ObjectValidator(PrismContext.get());
        objValidator.setWarnIncorrectOids(true);
        validator = new AuditRecordValidator(objValidator);
    }

    @Override
    protected void write(Writer writer, AuditEventRecordType object) throws SchemaException, IOException {
        ValidationResult result = validator.validate(object);
        if (result.isEmpty()) {
            return;
        }
        increaseStats(result);


        if (isCsv()) {
            writeCsvResult(writer, result, object);
        } else {
            writePlainResult(writer, result, object);
        }

    }

    private void increaseStats(ValidationResult result) {
        recordsWithIssue++;
        for (var item : result.getItems()) {
            var status = item.getStatus() != null ? item.getStatus() : OperationResultStatus.UNKNOWN;
            increaseCounter(status);
        }

    }

    private void increaseCounter(OperationResultStatus status) {
        switch (status) {
            case UNKNOWN:
                unknownCount++;
                break;
            case WARNING:
                warningCount++;
                break;
            case PARTIAL_ERROR:
            case FATAL_ERROR:
                errorCount++;
                break;
        }
    }

    private void writeCsvResult(Writer writer, ValidationResult result, AuditEventRecordType object) throws IOException {
        var printer = CSV_FORMAT.print(writer);
        for (var item : result.getItems()) {
            printer.printRecord(createCsvRecord(item, object));
        }
    }

    private List<Object> createCsvRecord(ValidationItem item, AuditEventRecordType record) {
        return List.of(
                record.getRepoId(),
                record.getTimestamp(),
                item.getStatus(),
                item.getItemPath(),
                getMessage(item.getMessage())
        );
    }

    private void writePlainResult(Writer writer, ValidationResult result, AuditEventRecordType audit) throws IOException {
        for (var item : result.getItems()) {
            plainWrite(writer, item.getStatus(),
                    audit.getTimestamp(),
                    audit.getRepoId(),
                    item.getItemPath(),
                    getMessage(item.getMessage())
            );
        }
    }

    private void plainWrite(Writer writer, Object... args) throws IOException {
        StringBuilder line = new StringBuilder();
        if (args != null && args.length > 0) {
            boolean first = true;
            for (var obj: args) {
                if (!first) {
                    line.append(' ');
                }
                line.append(obj);
                first = false;
            }
        }
        line.append('\n');
        writer.write(line.toString());
    }

    private String getMessage(LocalizableMessage message) {
        if (message == null) {
            return null;
        }
        return message.getFallbackMessage();
    }

    private boolean isCsv() {
        return options.getReportStyle() == VerifyAuditOptions.ReportStyle.CSV;
    }


    public String getProlog() {
        if (isCsv()) {
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

    @Override
    protected String getEpilog() {
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

    public long getRecordsWithIssueCount() {
        return recordsWithIssue;
    }

    public long getRecordsWithIssue() {
        return recordsWithIssue;
    }

    public long getUnknownCount() {
        return unknownCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getWarningCount() {
        return warningCount;
    }
}
