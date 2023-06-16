package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.LocalizableMessage;

// todo finish
public class VerifyFilesAction extends Action<VerifyFilesOptions, Void> {

    @Override
    public Void execute() throws Exception {
        for (File file : options.getFiles()) {
            if (!file.isDirectory()) {
                validateFile(file);
            } else {
                Collection<File> children = FileUtils.listFiles(file, new String[] { "xml" }, true);
                for (File child : children) {
                    if (child.isDirectory()) {
                        continue;
                    }

                    validateFile(child);
                }
            }
        }

        return null;
    }

    private void validateFile(File file) {
        PrismContext prismContext = context.getPrismContext();
        ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
        PrismParser parser = prismContext.parserFor(file).language(PrismContext.LANG_XML).context(parsingContext);

        ObjectValidator validator = new ObjectValidator(context.getPrismContext());
        validator.setAllWarnings();

        try (Writer writer = new PrintWriter(System.out)) {
            List<PrismObject<?>> objects = parser.parseObjects();

            for (PrismObject<?> object : objects) {
                ValidationResult validationResult = validator.validate(object);

                for (ValidationItem validationItem : validationResult.getItems()) {
                    writeValidationItem(writer, object, validationItem);
                }
            }
        } catch (Exception ex) {
            // todo handle error
            ex.printStackTrace();
        }
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
