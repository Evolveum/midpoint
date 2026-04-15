/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

public class FilePathValidator implements IValidator<String> {

    private static final long serialVersionUID = 1L;

    private boolean checkFileExistence;

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isEmpty(value)) {
            return;
        }

        try {
            String preProcessedPath = preProcessPath(value);

            Path path = Path.of(preProcessedPath);

            if (checkFileExistence) {
                File file = path.toFile();
                if (!file.exists()) {
                    validatable.error(new ValidationError(this).addKey("FilePathValidator.fileDoesNotExist"));
                } else if (!file.canRead()) {
                    validatable.error(new ValidationError(this).addKey("FilePathValidator.fileNotReadable"));
                }
            }
        } catch (Exception e) {
            validatable.error(new ValidationError(this).addKey("FilePathValidator.invalidPath"));
        }
    }

    /**
     * Override this method to pre-process the path before validation (e.g. resolve variables, apply prefixes, etc.)
     */
    protected String preProcessPath(String path) {
        return path;
    }

    public FilePathValidator checkExistence(boolean checkFileExistence) {
        this.checkFileExistence = checkFileExistence;
        return this;
    }
}
