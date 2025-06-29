package org.apereo.cas.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Utility class to assist with regex operations.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@UtilityClass
public class RegexUtils {

    /**
     * A pattern match that does not match anything.
     */
    public static final Pattern MATCH_NOTHING_PATTERN = Pattern.compile("a^");

    /**
     * Check to see if the specified pattern is a valid regular expression.
     *
     * @param pattern the pattern
     * @return whether this is a valid regex or not
     */
    public static boolean isValidRegex(final String pattern) {
        try {
            if (StringUtils.isNotBlank(pattern)) {
                Pattern.compile(pattern);
                return true;
            }
        } catch (final PatternSyntaxException exception) {
            LOGGER.debug("Pattern [{}] is not a valid regex.", pattern);
        }
        return false;
    }

    /**
     * Concatenate all elements in the given collection to form a regex pattern.
     *
     * @param requiredValues  the required values
     * @param caseInsensitive the case insensitive
     * @return the pattern
     */
    public static Pattern concatenate(final Collection<?> requiredValues, final boolean caseInsensitive) {
        val pattern = requiredValues
            .stream()
            .map(Object::toString)
            .collect(Collectors.joining("|", "(", ")"));
        return createPattern(pattern, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    /**
     * Creates the pattern. Matching is by default
     * case insensitive.
     *
     * @param pattern the pattern, may not be null.
     * @return the pattern or {@link RegexUtils#MATCH_NOTHING_PATTERN}
     * if pattern is null or invalid.
     */
    public static Pattern createPattern(final String pattern) {
        return createPattern(pattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Creates the pattern with the given flags.
     *
     * @param pattern the pattern, may be null.
     * @param flags   the flags
     * @return the compiled pattern or {@link RegexUtils#MATCH_NOTHING_PATTERN} if pattern is null or invalid.
     */
    public static Pattern createPattern(final String pattern, final int flags) {
        if (StringUtils.isBlank(pattern)) {
            LOGGER.warn("Pattern cannot be null/blank");
            return MATCH_NOTHING_PATTERN;
        }
        try {
            return Pattern.compile(pattern, flags);
        } catch (final PatternSyntaxException exception) {
            LOGGER.debug("Pattern [{}] is not a valid regex.", pattern);
            return MATCH_NOTHING_PATTERN;
        }
    }

    /**
     * Matches the entire region for the string.
     *
     * @param pattern the pattern
     * @param value   the string
     * @return true/false
     * @see Matcher#matches()
     */
    public static boolean matches(final Pattern pattern, final String value) {
        return pattern.matcher(value).matches();
    }

    /**
     * Matches boolean.
     *
     * @param pattern       the pattern
     * @param value         the value
     * @param completeMatch the complete match
     * @return true/false
     */
    public static boolean matches(final Pattern pattern, final String value, final boolean completeMatch) {
        val matcher = pattern.matcher(value);
        LOGGER.debug("Matching value [{}] against pattern [{}]", value, pattern.pattern());
        if (completeMatch) {
            return matcher.matches();
        }
        return matcher.find();
    }

    /**
     * Attempts to find the next sub-sequence of the input sequence that matches the pattern.
     *
     * @param pattern the pattern
     * @param value   the string
     * @return true/false
     * @see Matcher#find()
     */
    public static boolean find(final Pattern pattern, final String value) {
        return pattern.matcher(value).find();
    }

    /**
     * Attempts to find the next sub-sequence of the input sequence that matches the pattern.
     *
     * @param pattern the pattern
     * @param value   the string
     * @return true/false
     */
    public static boolean find(final String pattern, final String value) {
        return StringUtils.isNotBlank(value) && createPattern(pattern, Pattern.CASE_INSENSITIVE).matcher(value).find();
    }

    /**
     * Find first in the list of items provided.
     *
     * @param pattern  the regex pattern
     * @param elements the elements
     * @return the optional
     */
    public static Optional<String> findFirst(final String pattern, final Collection elements) {
        val compiledPattern = createPattern(pattern);
        return elements
            .stream()
            .filter(entry -> find(compiledPattern, entry.toString()))
            .findFirst();
    }

    /**
     * Find first matching pattern in the given collection of elements.
     *
     * @param patterns the patterns
     * @param elements the elements
     * @return the optional
     */
    public static Optional<String> findFirst(final Collection<String> patterns, final Collection elements) {
        return patterns
            .stream()
            .map(RegexUtils::createPattern)
            .flatMap(compiledPattern -> elements.stream().filter(r -> find(compiledPattern, r.toString())))
            .findFirst();
    }

    /**
     * Matches ip address.
     *
     * @param pattern    the pattern
     * @param remoteAddr the remote addr
     * @return true or false
     */
    public static boolean matchesIpAddress(final String pattern, final String remoteAddr) {
        try {
            val ipAddressMatcher = new IpAddressMatcher(pattern);
            LOGGER.trace("Attempting to match [{}] against [{}] as a IP or netmask", remoteAddr, pattern);
            return ipAddressMatcher.matches(remoteAddr);
        } catch (final Exception e) {
            LOGGER.trace("Falling back to regex match. Couldn't treat [{}] as an IP address or netmask: [{}]", pattern, e.getMessage());
            return find(pattern, remoteAddr);
        }
    }
}
