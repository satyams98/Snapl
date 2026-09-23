package com.satyam.urlshortner.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentParserTest {

    @Test
    void parsesChromeOnWindowsDesktop() {
        var parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        assertEquals("Desktop", parsed.deviceType());
        assertEquals("Chrome", parsed.browser());
        assertEquals("Windows", parsed.os());
    }

    @Test
    void parsesSafariOnIphoneAsMobile() {
        var parsed = UserAgentParser.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");

        assertEquals("Mobile", parsed.deviceType());
        assertEquals("Safari", parsed.browser());
        assertEquals("iOS", parsed.os());
    }

    @Test
    void parsesFirefoxOnLinux() {
        var parsed = UserAgentParser.parse("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0");

        assertEquals("Desktop", parsed.deviceType());
        assertEquals("Firefox", parsed.browser());
        assertEquals("Linux", parsed.os());
    }

    @Test
    void parsesEdgeSeparatelyFromChrome() {
        var parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0");

        assertEquals("Edge", parsed.browser());
    }

    @Test
    void parsesApiClientsAsNonBrowserTraffic() {
        var parsed = UserAgentParser.parse("curl/8.4.0");

        assertEquals("API client", parsed.browser());
        assertEquals("Other", parsed.os());
    }

    @Test
    void blankOrNullUserAgentReturnsUnknown() {
        assertEquals("Unknown", UserAgentParser.parse(null).browser());
        assertEquals("Unknown", UserAgentParser.parse("  ").deviceType());
    }
}
