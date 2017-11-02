package com.atlassian.oai.validator.model;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * A normalised representation of an API path.
 * <p>
 * Normalised paths are devoid of path prefixes and contain a normalised starting/ending
 * slash to make comparisons easier.
 */
public interface NormalisedPath {

    /**
     * @return The number of path parts from the normalised path
     */
    int numberOfParts();

    /**
     * @return The path part at the given index
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    @Nonnull
    String part(int index);

    /**
     * @return Whether the path part at the given index contains one or more path params (e.g. "/my/{param}/")
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    boolean hasParams(int index);

    /**
     * @return The parameter name(s) in the path part at the given index, or an empty list if the given
     * part does not have a parameter. Parameter names are returned in order.
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    List<String> paramNames(int index);

    /**
     * Extract the param values for each param in the indexed path part, extracted from the given request path part.
     *
     * @param index The index of the path part to extract templated params with
     * @param requestPathPart The request path part to extract param values from
     *
     * @return A list containing (in order) the value for each path param in the given part,
     * or empty if one could not be found.
     *
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    List<Optional<String>> paramValues(int index, String requestPathPart);

    /**
     * @return The original, un-normalised path string
     */
    @Nonnull
    String original();

    /**
     * @return The normalised path string, with prefixes removed and a standard treatment for leading/trailing slashes.
     */
    @Nonnull
    String normalised();
}
