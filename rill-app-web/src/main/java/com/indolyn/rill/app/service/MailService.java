package com.indolyn.rill.app.service;

public interface MailService {

    void sendEmail(String to, String subject, String textBody);
}
