package org.example.goldenheartrestaurant.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {

    private boolean enabled = false;

    private String cloudName = "";

    private String apiKey = "";

    private String apiSecret = "";

    private String folder = "goldenheart/menu-items";

    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(cloudName)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiSecret);
    }
}
