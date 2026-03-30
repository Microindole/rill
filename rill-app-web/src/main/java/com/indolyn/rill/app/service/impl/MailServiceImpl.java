package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.service.MailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    private final boolean mailEnabled;
    private final String from;
    private final JavaMailSender mailSender;

    public MailServiceImpl(
        @Value("${app.mail.enabled:false}") boolean mailEnabled,
        @Value("${app.mail.from:noreply@example.com}") String from,
        JavaMailSender mailSender) {
        this.mailEnabled = mailEnabled;
        this.from = from;
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String textBody) {
        if (!mailEnabled) {
            LOGGER.info("Mail disabled. To={}, Subject={}, Body={}", to, subject, textBody);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(textBody);
        mailSender.send(message);
    }
}
