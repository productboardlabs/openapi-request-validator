package com.atlassian.oai.validator.model;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * A container representing a single API operation.
 * <p>
 * This includes the path, method and operation components from the OAI spec object. Used as a
 * convenience to hold related information in one place.
 */
public class ApiOperation {
    private final NormalisedPath pathString;
    private final Path pathObject;
    private final HttpMethod method;
    private final Operation operation;

    public ApiOperation(@Nonnull final NormalisedPath pathString, @Nonnull final Path pathObject,
                        @Nonnull final HttpMethod method, @Nonnull final Operation operation) {

        this.pathString = requireNonNull(pathString, "A path string is required");
        this.pathObject = requireNonNull(pathObject, "A path object is required");
        this.method = requireNonNull(method, "A request method is required");
        this.operation = requireNonNull(operation, "A operation object is required");
    }

    /**
     * @return The path the operation is on
     */
    @Nonnull
    public NormalisedPath getPathString() {
        return pathString;
    }

    /**
     * @return The path object from the OAI specification
     */
    @Nonnull
    public Path getPathObject() {
        return pathObject;
    }

    /**
     * @return The method the operation is on
     */
    @Nonnull
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @return The operation object from the OAI specification
     */
    @Nonnull
    public Operation getOperation() {
        return operation;
    }
}
