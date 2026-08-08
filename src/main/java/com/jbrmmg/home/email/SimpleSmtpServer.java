package com.jbrmmg.home.email;

import com.jbrmmg.home.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SimpleSmtpServer {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleSmtpServer.class);

    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    private final EmailRateLimiter rateLimiter;
    private final ApplicationProperties applicationProperties;

    public SimpleSmtpServer(EmailRateLimiter rateLimiter, ApplicationProperties applicationProperties) {
        this.rateLimiter = rateLimiter;
        this.applicationProperties = applicationProperties;
        start();
    }

    public void start() {
        pool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(applicationProperties.getEmail().getPort())) {
                LOG.info("SMTP server listening on {}:{}", serverSocket.getInetAddress(), applicationProperties.getEmail().getPort());
                while (true) {
                    Socket client = serverSocket.accept();
                    pool.submit(() -> handleClient(client));
                }
            } catch (IOException e) {
                LOG.error("Failed to handle SMTP", e);
            }
        });
    }

    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {

            InetAddress addr = client.getInetAddress();
            if (!addr.isSiteLocalAddress()) {
                writer.write("550 Relay denied\r\n");
                writer.flush();
                client.close();
                LOG.warn("550 Relay denied");
                return;
            }

            writer.write("220 SimpleSMTP Ready\r\n");
            writer.flush();
            LOG.info("SMTP ready.");

            String line;
            boolean dataMode = false;
            StringBuilder emailData = new StringBuilder();
            String currentRecipient = null;

            while ((line = reader.readLine()) != null) {
                if (line.toUpperCase().startsWith("HELO") || line.toUpperCase().startsWith("EHLO")) {
                    writer.write("250 Hello\r\n");
                } else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                    writer.write("250 OK\r\n");
                } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                    String recipient = line.substring(8).trim()
                            .replaceAll("[<>]", ""); // clean <> brackets

                    if (applicationProperties.getAllowedRecipients().contains(recipient.toLowerCase())) {
                        currentRecipient = recipient;
                        writer.write("250 OK\r\n");
                    } else {
                        writer.write("550 Recipient not allowed\r\n");
                        writer.flush();
                        client.close();
                        return;
                    }
                } else if (line.toUpperCase().startsWith("DATA")) {
                    writer.write("354 End data with <CR><LF>.<CR><LF>\r\n");
                    dataMode = true;
                } else if (dataMode) {
                    if (line.equals(".")) {
                        // End of message
                        writer.write("250 OK\r\n");
                        writer.flush();
                        dataMode = false;

                        rateLimiter.submit(emailData.toString().getBytes(), currentRecipient);
                        emailData.setLength(0);
                    } else {
                        emailData.append(line).append("\r\n");
                    }
                } else if (line.toUpperCase().startsWith("QUIT")) {
                    writer.write("221 Bye\r\n");
                    writer.flush();
                    break;
                } else {
                    writer.write("250 OK\r\n");
                }
                writer.flush();
            }

            LOG.info("Email sent successfully.");
        } catch (IOException e) {
            LOG.error("Failed to handle SMTP", e);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
