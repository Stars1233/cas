package org.apereo.cas.services;

import org.apereo.cas.authentication.principal.OidcPairwisePersistentIdGenerator;
import org.apereo.cas.authentication.principal.PersistentIdGenerator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.Serial;

/**
 * This is {@link PairwiseOidcRegisteredServiceUsernameAttributeProvider}.
 * This provides a different sub value to each Client, so as not to enable
 * Clients to correlate the End-User's activities without permission.
 * When pairwise Subject Identifiers are used, the OpenID Provider MUST calculate a unique sub
 * (subject) value for each Sector Identifier. The Subject Identifier value
 * MUST NOT be reversible by any party other than the OpenID Provider.
 * <p>
 * If the client has not provided a value for {@code sector_identifier_uri} in
 * dynamic client Registration, the sector identifier used for
 * pairwise identifier calculation is the host component of the registered {@code redirect_uri}.
 * If there are multiple host names in the registered {@code redirect_uri}s,
 * the Client MUST register a {@code sector_identifier_uri}.
 * When a {@code sector_identifier_uri} is provided, the host component of that URL is used as the sector
 * identifier for the pairwise identifier calculation. The value of the {@code sector_identifier_uri} MUST
 * be a URL using the https scheme that points to a JSON file containing an array of redirect_uri values.
 * The values of the registered {@code redirect_uri}s MUST be included in the elements of the array.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Setter
public class PairwiseOidcRegisteredServiceUsernameAttributeProvider extends BaseRegisteredServiceUsernameAttributeProvider {

    @Serial
    private static final long serialVersionUID = 469929103943101717L;

    private PersistentIdGenerator persistentIdGenerator = new OidcPairwisePersistentIdGenerator();

    @Override
    public String resolveUsernameInternal(final RegisteredServiceUsernameProviderContext context) {
        if (context.getRegisteredService() == null || !OidcRegisteredService.class.isAssignableFrom(context.getRegisteredService().getClass())) {
            LOGGER.warn("Service definition [{}] is undefined or it's not an OpenId Connect relying party", context.getRegisteredService());
            return context.getPrincipal().getId();
        }
        val oidcSvc = (OidcRegisteredService) context.getRegisteredService();
        if (StringUtils.isBlank(oidcSvc.getSubjectType())
            || Strings.CI.equals(OidcSubjectTypes.PUBLIC.getType(), oidcSvc.getSubjectType())) {
            LOGGER.warn("Service definition [{}] does not request a pairwise subject type", oidcSvc);
            return context.getPrincipal().getId();
        }
        val sectorIdentifier = getSectorIdentifier(oidcSvc);
        val id = this.persistentIdGenerator.generate(context.getPrincipal(), sectorIdentifier);
        LOGGER.debug("Resolved username [{}] for pairwise access", id);
        return id;
    }

    private static String getSectorIdentifier(final OidcRegisteredService client) {
        if (StringUtils.isNotBlank(client.getSectorIdentifierUri())) {
            val uri = UriComponentsBuilder.fromUriString(client.getSectorIdentifierUri()).build();
            return uri.getHost();
        }
        val uri = UriComponentsBuilder.fromUriString(client.getServiceId()).build();
        return uri.getHost();
    }

}
