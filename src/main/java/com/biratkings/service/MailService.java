package com.biratkings.service;

import com.biratkings.model.Models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender sender;
    private final String admin;
    private final String from;

    public MailService(JavaMailSender sender,
                       @Value("${app.admin.email}") String admin,
                       @Value("${spring.mail.username:}") String from) {
        this.sender = sender; this.admin = admin; this.from = from;
    }

    /** @return true only if the SMTP server accepted the message. */
    public boolean notifyAdmin(String orderId, OrderRequest r) {
        if (from == null || from.isBlank()) {
            log.warn("MAIL_USERNAME/MAIL_PASSWORD not set — order {} NOT emailed. Set them to enable admin notifications.", orderId);
            return false;
        }
        StringBuilder b = new StringBuilder();
        b.append("New Biratnagar Kings store order ").append(orderId).append("\n\n")
         .append("Customer: ").append(clean(r.name())).append('\n')
         .append("Phone:    ").append(r.phone()).append('\n')
         .append("Address:  ").append(clean(r.address())).append("\n\nItems:\n");
        int units = 0;
        for (OrderItem i : r.items()) {
            units += i.quantity();
            b.append(" - ").append(i.productId()).append(" | size ").append(i.size()).append(" | qty ").append(i.quantity());
            if (i.customName() != null && !i.customName().isBlank()) b.append(" | name: ").append(i.customName().trim().toUpperCase());
            if (i.customNumber() != null) b.append(" | number: ").append(i.customNumber());
            b.append('\n');
        }
        b.append("\nTotal units: ").append(units);
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from); m.setTo(admin);
            m.setSubject("New order " + orderId + " — " + units + " item(s)");
            m.setText(b.toString());
            sender.send(m);
            return true;
        } catch (Exception e) {
            log.error("Failed to email order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private static String clean(String s) { return s == null ? "" : s.replaceAll("[\\r\\n]+", " ").trim(); }
}
