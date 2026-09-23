package com.satyam.urlshortner.analytics;

import java.util.Locale;

// Deliberately dependency-free: a handful of substring checks cover the vast majority of
// real-world traffic without pulling in a full UA-parsing library.
public final class UserAgentParser {

    public record ParsedUserAgent(String deviceType, String browser, String os) {}

    private static final ParsedUserAgent UNKNOWN = new ParsedUserAgent("Unknown", "Unknown", "Unknown");

    private UserAgentParser() {
    }

    public static ParsedUserAgent parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return new ParsedUserAgent(deviceType(ua), browser(ua), os(ua));
    }

    private static String deviceType(String ua) {
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return "Mobile";
        }
        return "Desktop";
    }

    private static String os(String ua) {
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Other";
    }

    private static String browser(String ua) {
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/") && !ua.contains("edg/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("curl/") || ua.contains("postman") || ua.contains("python-requests")) {
            return "API client";
        }
        return "Other";
    }
}
