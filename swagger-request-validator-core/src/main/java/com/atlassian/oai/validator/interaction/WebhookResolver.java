package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.ApiPathImpl;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves OAS 3.1 top-level {@code webhooks} entries by name.
 *
 * <p>Unlike {@code paths}, webhooks are addressed by name (e.g. {@code newOrder})
 * rather than URL path. They define operations the server <em>sends</em> to a
 * client. This resolver maps each webhook name + HTTP method to its
 * {@link ApiOperation} for downstream validation by the existing
 * {@code RequestValidator}/{@code ResponseValidator} pipelines.
 *
 * <p>Webhook resolution is exact-match by name; no path templating, no base-path
 * prefix, no specificity scoring (those concepts don't apply to webhooks).
 */
public class WebhookResolver {

    private final Map<String, Map<PathItem.HttpMethod, Operation>> operationsByWebhook;
    private final Map<String, ApiPath> apiPathByWebhook;

    public WebhookResolver(@Nonnull final OpenAPI api) {
        final Map<String, PathItem> webhooks = api.getWebhooks() != null
                ? api.getWebhooks()
                : Collections.emptyMap();

        this.operationsByWebhook = new HashMap<>();
        this.apiPathByWebhook = new HashMap<>();

        for (final Map.Entry<String, PathItem> e : webhooks.entrySet()) {
            final String name = e.getKey();
            final PathItem pathItem = e.getValue();
            if (pathItem == null) {
                continue;
            }
            final Map<PathItem.HttpMethod, Operation> ops = new EnumMap<>(PathItem.HttpMethod.class);
            pathItem.readOperationsMap().forEach(ops::put);
            if (!ops.isEmpty()) {
                operationsByWebhook.put(name, ops);
                // We construct a synthetic ApiPath using the webhook name as
                // the path. Downstream validators only use this for context;
                // matching has already been done by name.
                apiPathByWebhook.put(name, new ApiPathImpl(name, "/", false));
            }
        }
    }

    /**
     * Returns true if the spec declares the named webhook.
     */
    public boolean hasWebhook(@Nonnull final String webhookName) {
        return operationsByWebhook.containsKey(webhookName);
    }

    /**
     * Resolves the {@link ApiOperation} for a given webhook name + HTTP method.
     * Returns {@code null} if the webhook is unknown or the method is not
     * declared on it.
     */
    @Nullable
    public ApiOperation findOperation(@Nonnull final String webhookName,
                                      @Nonnull final Request.Method method) {
        final Map<PathItem.HttpMethod, Operation> ops = operationsByWebhook.get(webhookName);
        if (ops == null) {
            return null;
        }
        final PathItem.HttpMethod httpMethod;
        try {
            httpMethod = PathItem.HttpMethod.valueOf(method.name());
        } catch (final IllegalArgumentException ex) {
            return null;
        }
        final Operation operation = ops.get(httpMethod);
        if (operation == null) {
            return null;
        }
        final ApiPath apiPath = apiPathByWebhook.get(webhookName);
        // Use the same ApiPath as the request path for context purposes.
        // The webhook name acts as the path identifier.
        final NormalisedPath requestPath = apiPath;
        return new ApiOperation(apiPath, requestPath, httpMethod, operation);
    }

    /**
     * Returns true if the spec declares any webhooks at all.
     */
    public boolean hasAnyWebhooks() {
        return !operationsByWebhook.isEmpty();
    }
}
