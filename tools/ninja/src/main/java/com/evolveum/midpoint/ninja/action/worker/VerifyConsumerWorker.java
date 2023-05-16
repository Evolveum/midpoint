/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.VerifyOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.xml.serialize.LineSeparator;

/**
 * @author Radovan Semancik
 */
public class VerifyConsumerWorker extends AbstractWriterConsumerWorker<VerifyOptions, ObjectType> {

    private static final String[] REPORT_HEADER = {
            "Oid",
            "Type",
            "Name",
            "Status",
            "Item path",
            "Message",
            "Ignore during upgrade [yes/no]"
    };

    private ObjectValidator validator;

    private CSVPrinter reportWriter;

    public VerifyConsumerWorker(NinjaContext context, VerifyOptions options,
            BlockingQueue<ObjectType> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    protected void init() {
        validator = new ObjectValidator(context.getPrismContext());
        String warnOption = options.getWarn();
        if (warnOption == null) {
            validator.setAllWarnings();
        } else {
            String[] warnCategories = warnOption.split(",");
            for (String warnCategory : warnCategories) {
                switch (warnCategory) {
                    case "deprecated":
                        validator.setWarnDeprecated(true);
                        break;
                    case "plannedRemoval":
                        validator.setWarnPlannedRemoval(true);
                        break;
                    case "uuid":
                        validator.setWarnIncorrectOids(true);
                        break;
                    default:
                        System.err.println("Unknown warn option '" + warnCategory + "'");
                        break;
                }
            }
        }

        if (BooleanUtils.isTrue(options.isCreateReport())) {
            CSVFormat csv = createCsvFormat();

            try {
                File file = new File("./target/" + System.currentTimeMillis() + ".csv");
                file.createNewFile();
                Writer writer = new BufferedWriter(new FileWriter(file, context.getCharset()));
                reportWriter = csv.print(writer);

                reportWriter.printRecord(REPORT_HEADER);
            } catch (IOException ex) {
                ex.printStackTrace(); // todo handle exception
            }
        }
    }

    @Override
    public void destroy() {
        if (reportWriter != null) {
            IOUtils.closeQuietly(reportWriter);
        }
    }

    @Override
    protected String getProlog() {
        return null;
    }

    @Override
    protected void write(Writer writer, ObjectType object) throws IOException {
        PrismObject<?> prismObject = object.asPrismObject();
        ValidationResult validationResult = validator.validate(prismObject);
        for (ValidationItem validationItem : validationResult.getItems()) {
            writeValidationItem(writer, prismObject, validationItem);
        }

        if (BooleanUtils.isTrue(options.isCreateReport())) {
            for (ValidationItem item : validationResult.getItems()) {
                reportWriter.printRecord(createReportRecord(item, prismObject));
            }
        }
    }

    private String[] createReportRecord(ValidationItem item, PrismObject<?> object) {
        // this array has to match {@link VerifyConsumerWorker#REPORT_HEADER}
        return new String[] {
                object.getOid(),
                object.getDefinition().getTypeName().getLocalPart(),
                object.getName().getOrig(),
                Objects.toString(item.getStatus()),
                Objects.toString(item.getItemPath()),
                item.getMessage() != null ? item.getMessage().getFallbackMessage() : null,
                null
        };
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

    @Override
    protected String getEpilog() {
        return null;
    }

    private CSVFormat createCsvFormat() {
        return CSVFormat.newFormat(';')
                .withEscape('\\')
                .withIgnoreHeaderCase(false)
                .withQuote('"')
                .withRecordSeparator('\n')
                .withQuoteMode(QuoteMode.ALL);
    }
}
