package com.jbrmmg.home.email;

import com.jbrmmg.home.config.ApplicationProperties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Properties;

@Component
public class OutlookForwarder implements MailForwarder {
    private static final Logger LOG = LoggerFactory.getLogger(OutlookForwarder.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    public OutlookForwarder(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void forward(byte[] rawMessage, String targetEmail) {
        try {
            // Set up the properties
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", applicationProperties.getEmail().getSmtpHost());
            properties.put("mail.smtp.port", applicationProperties.getEmail().getSmtpPort());

            String key = applicationProperties.getEmail().getKey();
            String password = AESDecryptCBC.decrypt(applicationProperties.getEmail().getSmtpPassword(),key,key);

            // Create the session
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(applicationProperties.getEmail().getSmtpSender(),password);
                }
            });

            // Create the message
            MimeMessage message = new MimeMessage(
                    session,
                    new ByteArrayInputStream(rawMessage)
            );

            // Set the recipient.
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(targetEmail)
            );

            // Set the source email.
            message.setFrom(new InternetAddress(applicationProperties.getEmail().getSmtpSender()));

            // Send
            Transport.send(message);
        } catch (Exception e) {
            LOG.error("Failed to forward the email", e);
        }
    }
}
