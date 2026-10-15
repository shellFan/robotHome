package com.robot.home.security;

import com.robot.home.auth.service.impl.AuthServiceImpl;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.AuthenticationException;
import com.robot.home.common.util.JwtUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.user.entity.User;
import com.robot.home.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtSecurityRegressionTest {

    private static final String SECRET = "unit-test-jwt-key-with-more-than-thirty-two-characters";

    private JwtUtils jwtUtils;
    private RedisUtils redisUtils;
    private UserService userService;
    private AuthServiceImpl authService;
    private Map<String, String> redisStore;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", 60_000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", 120_000L);

        redisStore = new HashMap<>();
        redisUtils = mock(RedisUtils.class);
        when(redisUtils.hasKey(anyString())).thenAnswer(invocation ->
                redisStore.containsKey(invocation.getArgument(0)));
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(redisUtils).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        userService = mock(UserService.class);
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus(1);
        when(userService.getById(7L)).thenReturn(user);

        authService = new AuthServiceImpl();
        ReflectionTestUtils.setField(authService, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(authService, "redisUtils", redisUtils);
        ReflectionTestUtils.setField(authService, "userService", userService);
        ReflectionTestUtils.setField(authService, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(authService, "jwtRefreshExpiration", 120_000L);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void refreshTokenCannotAuthenticateAsAccessToken() {
        String refreshToken = jwtUtils.generateRefreshToken(7L, "alice");
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "redisUtils", redisUtils);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);

        assertThrows(AuthenticationException.class,
                () -> filter.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNull(UserContext.getUser());
    }

    @Test
    void redisBlacklistFailureDeniesAuthentication() {
        String accessToken = jwtUtils.generateToken(7L, "alice", Constants.ROLE_USER);
        when(redisUtils.hasKey(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "redisUtils", redisUtils);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);

        assertThrows(AuthenticationException.class,
                () -> filter.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNull(UserContext.getUser());
    }

    @Test
    void accessTokenCannotBeUsedAtRefreshEndpoint() {
        String accessToken = jwtUtils.generateToken(7L, "alice", Constants.ROLE_USER);

        assertThrows(AuthenticationException.class, () -> authService.refresh(accessToken));
    }

    @Test
    void logoutRevokesRefreshTokenBeforeItCanMintNewTokens() {
        String refreshToken = jwtUtils.generateRefreshToken(7L, "alice");

        authService.logout(refreshToken);

        assertThrows(AuthenticationException.class, () -> authService.refresh(refreshToken));
    }

    @Test
    void logoutWithAccessTokenRevokesItsPairedRefreshToken() {
        Map<String, Object> pair = authService.refresh(jwtUtils.generateRefreshToken(7L, "alice"));
        String accessToken = (String) pair.get("token");
        String refreshToken = (String) pair.get("refreshToken");

        authService.logout(accessToken);

        assertThrows(AuthenticationException.class, () -> authService.refresh(refreshToken));
    }

    @Test
    void validRefreshTokenStillMintsTypedAccessAndRefreshTokens() {
        String refreshToken = jwtUtils.generateRefreshToken(7L, "alice");

        Map<String, Object> result = authService.refresh(refreshToken);

        assertEquals("user", jwtUtils.getType((String) result.get("token")));
        assertEquals("refresh", jwtUtils.getType((String) result.get("refreshToken")));
        assertEquals(jwtUtils.getSessionId(refreshToken),
                jwtUtils.getSessionId((String) result.get("token")));
        assertEquals(jwtUtils.getSessionId(refreshToken),
                jwtUtils.getSessionId((String) result.get("refreshToken")));
    }
}
