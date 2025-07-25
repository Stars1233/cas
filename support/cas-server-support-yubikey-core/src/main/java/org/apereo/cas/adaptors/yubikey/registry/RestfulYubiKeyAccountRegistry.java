package org.apereo.cas.adaptors.yubikey.registry;

import org.apereo.cas.adaptors.yubikey.YubiKeyAccount;
import org.apereo.cas.adaptors.yubikey.YubiKeyAccountValidator;
import org.apereo.cas.adaptors.yubikey.YubiKeyDeviceRegistrationRequest;
import org.apereo.cas.adaptors.yubikey.YubiKeyRegisteredDevice;
import org.apereo.cas.configuration.model.support.mfa.yubikey.YubiKeyRestfulMultifactorProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.http.HttpExecutionRequest;
import org.apereo.cas.util.http.HttpUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is {@link RestfulYubiKeyAccountRegistry}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class RestfulYubiKeyAccountRegistry extends BaseYubiKeyAccountRegistry {

    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(true).build().toObjectMapper();

    private final YubiKeyRestfulMultifactorProperties restProperties;

    public RestfulYubiKeyAccountRegistry(final YubiKeyRestfulMultifactorProperties restProperties,
                                         final YubiKeyAccountValidator validator) {
        super(validator);
        this.restProperties = restProperties;
    }

    @Override
    public YubiKeyAccount save(final YubiKeyDeviceRegistrationRequest request, final YubiKeyRegisteredDevice... device) {
        val account = YubiKeyAccount.builder()
            .username(request.getUsername())
            .devices(CollectionUtils.wrapList(device))
            .build();
        return save(account);
    }

    @Override
    public YubiKeyAccount save(final YubiKeyAccount account) {
        update(account);
        return account;
    }

    @Override
    public boolean update(final YubiKeyAccount account) {
        HttpResponse response = null;
        try {
            val headers = CollectionUtils.<String, String>wrap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            headers.putAll(restProperties.getHeaders());
            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.POST)
                .url(restProperties.getUrl())
                .headers(headers)
                .entity(MAPPER.writeValueAsString(account))
                .build();
            response = HttpUtils.execute(exec);
            return response != null && HttpStatus.valueOf(response.getCode()).is2xxSuccessful();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            HttpUtils.close(response);
        }
        return false;
    }

    @Override
    public void delete(final String username, final long deviceId) {
        HttpResponse response = null;
        try {
            val url = Strings.CI.appendIfMissing(restProperties.getUrl(), "/")
                .concat(username).concat("/").concat(String.valueOf(deviceId));

            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.DELETE)
                .headers(restProperties.getHeaders())
                .url(url)
                .build();

            response = HttpUtils.execute(exec);
        } finally {
            HttpUtils.close(response);
        }
    }

    @Override
    public void delete(final String username) {
        HttpResponse response = null;
        try {
            val url = Strings.CI.appendIfMissing(restProperties.getUrl(), "/").concat(username);
            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.DELETE)
                .headers(restProperties.getHeaders())
                .url(url)
                .build();
            response = HttpUtils.execute(exec);
        } finally {
            HttpUtils.close(response);
        }
    }

    @Override
    public void deleteAll() {
        HttpResponse response = null;
        try {
            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.DELETE)
                .url(restProperties.getUrl())
                .headers(restProperties.getHeaders())
                .build();
            response = HttpUtils.execute(exec);
        } finally {
            HttpUtils.close(response);
        }
    }

    @Override
    protected YubiKeyAccount getAccountInternal(final String username) {
        HttpResponse response = null;
        try {
            val url = Strings.CI.appendIfMissing(restProperties.getUrl(), "/").concat(username);
            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.GET)
                .url(url)
                .headers(restProperties.getHeaders())
                .build();
            response = HttpUtils.execute(exec);
            if (response != null) {
                val status = HttpStatus.valueOf(response.getCode());
                if (status.is2xxSuccessful()) {
                    try (val contis = ((HttpEntityContainer) response).getEntity().getContent()) {
                        val content = IOUtils.toString(contis, StandardCharsets.UTF_8);
                        return MAPPER.readValue(content, YubiKeyAccount.class);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            HttpUtils.close(response);
        }
        return null;
    }

    @Override
    protected Collection<? extends YubiKeyAccount> getAccountsInternal() {
        HttpResponse response = null;
        try {
            val exec = HttpExecutionRequest.builder()
                .basicAuthPassword(restProperties.getBasicAuthPassword())
                .basicAuthUsername(restProperties.getBasicAuthUsername())
                .method(HttpMethod.GET)
                .url(restProperties.getUrl())
                .headers(restProperties.getHeaders())
                .build();
            response = HttpUtils.execute(exec);
            if (response != null) {
                val status = HttpStatus.valueOf(response.getCode());
                if (status.is2xxSuccessful()) {
                    try (val contis = ((HttpEntityContainer) response).getEntity().getContent()) {
                        val content = IOUtils.toString(contis, StandardCharsets.UTF_8);
                        return MAPPER.readValue(content, new TypeReference<List<YubiKeyAccount>>() {
                        });
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            HttpUtils.close(response);
        }
        return new ArrayList<>();
    }
}
