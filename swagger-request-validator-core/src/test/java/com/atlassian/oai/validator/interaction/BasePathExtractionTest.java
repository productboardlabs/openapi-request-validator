package com.atlassian.oai.validator.interaction;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.junit.Test;

import java.util.List;

import static com.atlassian.oai.validator.interaction.ApiOperationResolver.getBasePathFrom;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class BasePathExtractionTest {

    @Test
    public void returnsDefault_whenNullServers() {
        assertEquals("/", getBasePathFrom(null));
    }

    @Test
    public void returnsDefault_whenEmptyServers() {
        assertEquals("/", getBasePathFrom(emptyList()));
    }

    @Test
    public void returnsBasePathOfServer_whenSingleServer() {
        final List<Server> servers = ImmutableList.of(
                new Server().url("https://localhost:8080/base/path/1")
        );

        assertEquals("/base/path/1", getBasePathFrom(servers));
    }

    @Test
    public void returnsBasePathOfFirstServer_whenMultipleServers() {
        final List<Server> servers = ImmutableList.of(
                new Server().url("http://example.com/base/path/1"),
                new Server().url("https://localhost:8080/base/path/2"),
                new Server().url("http://example.com/base/path/3")
        );

        assertEquals("/base/path/1", getBasePathFrom(servers));
    }

    @Test
    public void substitutesPathVariables_whenPresent() {
        final List<Server> servers = ImmutableList.of(
                new Server()
                        .url("https://localhost:8080/api/{scope}/{version}")
                        .variables(
                                new ServerVariables()
                                        .addServerVariable("scope", new ServerVariable()._default("external"))
                                        .addServerVariable("version", new ServerVariable()._default("1.1")))
        );

        assertEquals("/api/external/1.1", getBasePathFrom(servers));
    }

    @Test
    public void returnsUrlOfServer_whenOnlyUrlPathIsDefined() {
        final List<Server> servers = ImmutableList.of(
                new Server().url("/base/path/1")
        );

        assertEquals("/base/path/1", getBasePathFrom(servers));
    }

    @Test
    public void returnsUrlOfServer_whenNotAValidUrl() {
        final List<Server> servers = ImmutableList.of(
                new Server().url("{notaurl}")
        );

        assertEquals("{notaurl}", getBasePathFrom(servers));
    }

    @Test
    public void returnsDefault_whenServerUrlNull() {
        final List<Server> servers = ImmutableList.of(
                new Server().url(null)
        );

        assertEquals("/", getBasePathFrom(servers));
    }

}
