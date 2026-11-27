/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/**
 * External dictionaries stored in files inside midpoint.home, configured by the policy
 * (dictionaryFile).
 * A file is read when it is first used and kept in memory until midPoint restarts.
 * A file that is missing or outside midpoint.home is a configuration error.
 */
@Component
public class FilePasswordDictionary implements PasswordDictionary {

    private final Path midpointHome;

    /** Words of the files read so far, by the real path of the file. */
    private final Map<Path, Set<String>> cache = new ConcurrentHashMap<>();

    @Autowired
    public FilePasswordDictionary(MidpointConfiguration configuration) {
        this(Paths.get(configuration.getMidpointHome()));
    }

    FilePasswordDictionary(@NotNull Path midpointHome) {
        this.midpointHome = midpointHome.toAbsolutePath().normalize();
    }

    @Override
    public boolean contains(@NotNull String word, @NotNull LimitationsType limitations) {
        return getWordSets(limitations).stream().anyMatch(words -> words.contains(word));
    }

    @Override
    public @NotNull Stream<String> words(@NotNull LimitationsType limitations) {
        return getWordSets(limitations).stream().flatMap(Set::stream);
    }

    /** All files are resolved first, so a configuration error is reported even if the word is found in another file. */
    private List<Set<String>> getWordSets(LimitationsType limitations) {
        List<Set<String>> wordSets = new ArrayList<>();
        for (String file : limitations.getDictionaryFile()) {
            wordSets.add(getWords(file));
        }
        return wordSets;
    }

    private Set<String> getWords(String file) {
        Path realPath = resolve(file);
        return cache.computeIfAbsent(realPath, this::read);
    }

    private Path resolve(String file) {
        Path relative = Paths.get(file);
        if (relative.isAbsolute()) {
            throw new SystemException("Password dictionary file '" + file + "' must be relative to midpoint.home");
        }
        Path path = midpointHome.resolve(relative).normalize();
        if (!path.startsWith(midpointHome)) {
            throw new SystemException("Password dictionary file '" + file + "' is outside midpoint.home");
        }
        try {
            Path realPath = path.toRealPath();
            if (!realPath.startsWith(midpointHome.toRealPath())) {
                throw new SystemException("Password dictionary file '" + file + "' is outside midpoint.home");
            }
            return realPath;
        } catch (NoSuchFileException e) {
            throw new SystemException("Password dictionary file '" + file + "' not found in midpoint.home", e);
        } catch (IOException e) {
            throw new SystemException("Cannot access password dictionary file '" + file + "': " + e.getMessage(), e);
        }
    }

    private Set<String> read(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            return PasswordDictionaryFormat.read(stream);
        } catch (IOException e) {
            throw new SystemException("Cannot read password dictionary file " + path + ": " + e.getMessage(), e);
        }
    }
}
