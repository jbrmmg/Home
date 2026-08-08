package com.jbrmmg.home.config;

import lombok.Data;

@Data
public class EmailProperties {
    private Integer port;
    private Integer ratePerMinute;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpSender;
    private String smtpPassword;
    private String key;
}
