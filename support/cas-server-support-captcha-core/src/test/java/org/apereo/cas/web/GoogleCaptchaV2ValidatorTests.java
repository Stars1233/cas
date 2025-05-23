package org.apereo.cas.web;

import org.apereo.cas.configuration.model.support.captcha.GoogleRecaptchaProperties;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link GoogleCaptchaV2ValidatorTests}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@Tag("Simple")
class GoogleCaptchaV2ValidatorTests {
    @Test
    void verifyOperation() {
        val props = new GoogleRecaptchaProperties()
            .setScore(0.1)
            .setSecret(UUID.randomUUID().toString())
            .setVerifyUrl("http://localhost:8812");
        val validator = new GoogleCaptchaV2Validator(props);
        val request = new MockHttpServletRequest();
        request.addParameter(GoogleCaptchaV2Validator.REQUEST_PARAM_RECAPTCHA_RESPONSE, UUID.randomUUID().toString());
        assertNotNull(validator.getRecaptchaResponse(request));
    }
}
