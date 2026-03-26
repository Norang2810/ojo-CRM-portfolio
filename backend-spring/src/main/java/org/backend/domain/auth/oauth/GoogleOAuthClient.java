package org.backend.domain.auth.oauth;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.backend.domain.auth.oauth.dto.GoogleTokenResponse;
import org.backend.domain.auth.oauth.dto.GoogleUserInfo;
import org.backend.domain.auth.oauth.props.GoogleOAuthProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    //  기본값 fallback
    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String DEFAULT_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

    private final GoogleOAuthProperties props;

    private final RestClient restClient = RestClient.builder().build();

    // 앱 기동 시점에 설정값 검증 → 빈 값이면 바로 에러로 알려줌
    @PostConstruct
    void validateProps() {
        // clientId / clientSecret / redirectUri 는 "필수"
        requireText(props.clientId(), "app.google.client-id (GOOGLE_CLIENT_ID)");
        requireText(props.clientSecret(), "app.google.client-secret (GOOGLE_CLIENT_SECRET)");
        requireText(props.redirectUri(), "app.google.redirect-uri (GOOGLE_REDIRECT_URI)");

        // tokenUri / userInfoUri 는 "없으면 fallback" 하되, 경고 성격으로 예외 대신 통과
        // (원하면 여기서도 requireText로 강제해도 됨)
    }

    public GoogleUserInfo getUserInfoByCode(String code) {
        GoogleTokenResponse token = exchangeToken(code);
        return fetchUserInfo(token.accessToken());
    }

    private GoogleTokenResponse exchangeToken(String code) {
        String tokenUri = firstNonBlank(props.tokenUri(), DEFAULT_TOKEN_URI);

        return restClient.post()
                .uri(tokenUri) // props가 비어도 기본 URL로 보정
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("code=" + encode(code)
                        + "&client_id=" + encode(props.clientId())
                        + "&client_secret=" + encode(props.clientSecret())
                        + "&redirect_uri=" + encode(props.redirectUri())
                        + "&grant_type=authorization_code")
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) {
        String userInfoUri = firstNonBlank(props.userInfoUri(), DEFAULT_USERINFO_URI);

        return restClient.get()
                .uri(userInfoUri) // props가 비어도 기본 URL로 보정
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);
    }

    private String encode(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    //  유틸 메서드들
    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void requireText(String value, String keyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Google OAuth 설정 누락/빈 값: " + keyName);
        }
    }
}