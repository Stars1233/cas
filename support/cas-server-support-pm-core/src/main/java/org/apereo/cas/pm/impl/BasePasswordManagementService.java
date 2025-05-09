package org.apereo.cas.pm.impl;

import org.apereo.cas.audit.AuditActionResolvers;
import org.apereo.cas.audit.AuditResourceResolvers;
import org.apereo.cas.audit.AuditableActions;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.pm.PasswordChangeRequest;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apereo.inspektr.audit.annotation.Audit;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import java.io.Serializable;
import java.util.UUID;

/**
 * This is {@link BasePasswordManagementService}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public abstract class BasePasswordManagementService implements PasswordManagementService {

    protected final CasConfigurationProperties casProperties;

    protected final CipherExecutor<Serializable, String> cipherExecutor;

    protected final PasswordHistoryService passwordHistoryService;

    @Override
    public String parseToken(final String token) {
        try {
            val json = this.cipherExecutor.decode(token);
            val claims = JwtClaims.parse(json);
            val resetProperties = casProperties.getAuthn().getPm().getReset();
            val issuer = casProperties.getServer().getPrefix();
            
            if (!claims.getIssuer().equals(issuer)) {
                LOGGER.error("Token issuer does not match CAS");
                return null;
            }
            if (claims.getAudience().isEmpty() || !claims.getAudience().getFirst().equals(issuer)) {
                LOGGER.error("Token audience does not match CAS");
                return null;
            }
            if (StringUtils.isBlank(claims.getSubject())) {
                LOGGER.error("Token has no subject identifier");
                return null;
            }

            val holder = ClientInfoHolder.getClientInfo();
            if (resetProperties.isIncludeServerIpAddress() && !claims.getStringClaimValue("origin").equals(holder.getServerIpAddress())) {
                LOGGER.error("Token origin server IP address does not match CAS");
                return null;
            }
            if (resetProperties.isIncludeClientIpAddress() && !claims.getStringClaimValue("client").equals(holder.getClientIpAddress())) {
                LOGGER.error("Token client IP address does not match CAS");
                return null;
            }

            val expirationTime = claims.getExpirationTime();
            if (expirationTime.isBefore(NumericDate.now())) {
                LOGGER.error("Token has expired.");
                return null;
            }
            return claims.getSubject();
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return null;
    }

    @Override
    public String createToken(final PasswordManagementQuery query) {
        try {
            val issuer = casProperties.getServer().getPrefix();
            
            val token = UUID.randomUUID().toString();
            val claims = new JwtClaims();
            val resetProperties = casProperties.getAuthn().getPm().getReset();
            claims.setJwtId(token);
            claims.setIssuer(issuer);
            claims.setAudience(issuer);

            val minutes = Beans.newDuration(resetProperties.getExpiration()).toMinutes();
            claims.setExpirationTimeMinutesInTheFuture((float) minutes);
            claims.setIssuedAtToNow();

            val holder = ClientInfoHolder.getClientInfo();
            if (holder != null) {
                if (resetProperties.isIncludeServerIpAddress()) {
                    claims.setStringClaim("origin", holder.getServerIpAddress());
                }
                if (resetProperties.isIncludeClientIpAddress()) {
                    claims.setStringClaim("client", holder.getClientIpAddress());
                }
            }
            claims.setSubject(query.getUsername());
            LOGGER.debug("Creating password management token for [{}]", query.getUsername());
            val json = claims.toJson();

            LOGGER.debug("Encoding the generated JSON token...");
            return this.cipherExecutor.encode(json);
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return null;
    }

    @Audit(action = AuditableActions.CHANGE_PASSWORD,
        actionResolverName = AuditActionResolvers.CHANGE_PASSWORD_ACTION_RESOLVER,
        resourceResolverName = AuditResourceResolvers.CHANGE_PASSWORD_RESOURCE_RESOLVER)
    @Override
    public boolean change(final PasswordChangeRequest bean) throws Throwable {
        if (passwordHistoryService != null && passwordHistoryService.exists(bean)) {
            LOGGER.debug("Password history policy disallows reusing the password for [{}]", bean.getUsername());
            return false;
        }
        if (changeInternal(bean)) {
            if (passwordHistoryService != null) {
                LOGGER.debug("Password successfully changed; storing used password in history for [{}]...", bean.getUsername());
                return passwordHistoryService.store(bean);
            }
            return true;
        }
        return false;
    }

    protected abstract boolean changeInternal(PasswordChangeRequest bean) throws Throwable;
}
