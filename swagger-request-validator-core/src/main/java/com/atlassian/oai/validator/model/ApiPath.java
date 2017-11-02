package com.atlassian.oai.validator.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Representation of a path within an OAI/Swagger specification.
 * <p>
 * Has methods for extracting path params from path parts and comparing against a request path.
 */
public interface ApiPath extends NormalisedPath {

    /**
     * @return Whether the path part at the given index contains one or more path params (e.g. "/my/{param}/")
     *
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    boolean hasParams(int index);

    /**
     * @return The parameter name(s) in the path part at the given index, or an empty list if the given
     * part does not have a parameter. Parameter names are returned in order.
     *
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    List<String> paramNames(int index);

    /**
     * Extract the param values for each param in the indexed path part, extracted from the given request path part.
     *
     * @param index The index of the path part to extract templated params with
     * @param requestPathPart The request path part to extract param values from
     *
     * @return The (name, value) for each path param in the given part. If the param could not be found, will be empty.
     *
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    Map<String, Optional<String>> paramValues(int index, String requestPathPart);

}
