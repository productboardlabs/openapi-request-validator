package com.atlassian.oai.validator.schema.extension;

import com.github.fge.jsonschema.library.Library;

import java.util.function.Function;

public class SchemaExtensionOptions {
    private final Function<Library, Library> libraryExtender;
    private final String libraryUri;

    public SchemaExtensionOptions(
        final Function<Library, Library> libraryExtender,
        final String libraryUri) {
        this.libraryExtender = libraryExtender;
        this.libraryUri = libraryUri;
    }

    public Function<Library, Library> getLibraryExtender() {
        return libraryExtender;
    }

    public String getLibraryUri() {
        return libraryUri;
    }
}
