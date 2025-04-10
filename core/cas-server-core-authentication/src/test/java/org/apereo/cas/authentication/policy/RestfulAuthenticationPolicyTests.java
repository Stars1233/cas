package org.apereo.cas.authentication.policy;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.exceptions.AccountDisabledException;
import org.apereo.cas.authentication.exceptions.AccountPasswordMustChangeException;
import org.apereo.cas.configuration.model.core.authentication.RestAuthenticationPolicyProperties;
import org.apereo.cas.util.MockWebServer;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;

import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.apereo.cas.util.junit.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link RestfulAuthenticationPolicyTests}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Tag("AuthenticationPolicy")
class RestfulAuthenticationPolicyTests {
    private static void assertPolicyFails(final int port, final HttpStatus status,
                                          final Class<? extends Throwable> exceptionClass) {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        try (val webServer = new MockWebServer(port,
            new ByteArrayResource(StringUtils.EMPTY.getBytes(StandardCharsets.UTF_8), "Output"), status)) {
            webServer.start();
            val props = new RestAuthenticationPolicyProperties();
            props.setUrl("http://localhost:" + port);
            val policy = new RestfulAuthenticationPolicy(props);
            assertThrowsWithRootCause(GeneralSecurityException.class, exceptionClass,
                () -> policy.isSatisfiedBy(CoreAuthenticationTestUtils.getAuthentication("casuser"), applicationContext));
        }
    }

    @Test
    void verifyAllowedOperation() throws Throwable {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        try (val webServer = new MockWebServer(
            new ByteArrayResource(StringUtils.EMPTY.getBytes(StandardCharsets.UTF_8), "Output"), HttpStatus.OK)) {
            webServer.start();
            val props = new RestAuthenticationPolicyProperties();
            props.setUrl("http://localhost:%s".formatted(webServer.getPort()));
            val policy = new RestfulAuthenticationPolicy(props);
            assertTrue(policy.isSatisfiedBy(CoreAuthenticationTestUtils.getAuthentication("casuser"), applicationContext).isSuccess());
        }
    }

    @Test
    void verifyStatusCodeUnAuthz() {
        assertPolicyFails(9201, HttpStatus.UNAUTHORIZED, FailedLoginException.class);
        assertPolicyFails(9202, HttpStatus.LOCKED, AccountLockedException.class);
        assertPolicyFails(9203, HttpStatus.METHOD_NOT_ALLOWED, AccountDisabledException.class);
        assertPolicyFails(9204, HttpStatus.FORBIDDEN, AccountDisabledException.class);
        assertPolicyFails(9205, HttpStatus.NOT_FOUND, AccountNotFoundException.class);
        assertPolicyFails(9206, HttpStatus.PRECONDITION_FAILED, AccountExpiredException.class);
        assertPolicyFails(9207, HttpStatus.PRECONDITION_REQUIRED, AccountPasswordMustChangeException.class);
        assertPolicyFails(9208, HttpStatus.INTERNAL_SERVER_ERROR, FailedLoginException.class);
    }
}
