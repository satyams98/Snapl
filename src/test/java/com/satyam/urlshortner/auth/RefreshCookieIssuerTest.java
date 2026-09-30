package com.satyam.urlshortner.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshCookieIssuerTest {

    @Mock
    private ServerHttpResponse response;

    private final RefreshCookieIssuer issuer = new RefreshCookieIssuer();

    @Test
    void issueSetsCookieAndReturnsBody() {
        AuthResponse body = new AuthResponse("access-token", 900, 1L, "owner@acme.test", 10L, "Acme", "OWNER");
        AuthResult result = new AuthResult(body, "raw-refresh-token", Duration.ofDays(30));

        ResponseEntity<AuthResponse> entity = issuer.issue(result, response);

        ArgumentCaptor<ResponseCookie> captor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(response).addCookie(captor.capture());
        ResponseCookie cookie = captor.getValue();

        assertEquals(RefreshCookieIssuer.REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("raw-refresh-token", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofDays(30), cookie.getMaxAge());
        assertEquals(body, entity.getBody());
    }

    @Test
    void clearSetsEmptyExpiredCookie() {
        issuer.clear(response);

        ArgumentCaptor<ResponseCookie> captor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(response).addCookie(captor.capture());
        ResponseCookie cookie = captor.getValue();

        assertEquals(RefreshCookieIssuer.REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
    }
}
