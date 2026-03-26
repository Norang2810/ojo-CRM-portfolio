package org.backend.config.oauth;

import org.backend.domain.auth.oauth.props.GoogleOAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class OAuthConfig {
}