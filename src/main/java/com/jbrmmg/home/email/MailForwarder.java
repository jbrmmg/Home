package com.jbrmmg.home.email;

public interface MailForwarder {
    void forward(byte[] rawMessage, String targetEmail);
}
