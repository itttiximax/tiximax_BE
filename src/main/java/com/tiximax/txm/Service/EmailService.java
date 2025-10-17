package com.tiximax.txm.Service;

import com.tiximax.txm.Model.EmailDetail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendMailTemplate(EmailDetail emailDetail){
        try{
            Context context = new Context();

            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());

            String text = templateEngine.process("emailtemplate", context);
            

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            mimeMessageHelper.setFrom("global.trans@tiximax.net");
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text, true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());
            javaMailSender.send(mimeMessage);

        }catch (MessagingException messagingException){
            messagingException.printStackTrace();
        }
    }

    public void sendMailNotification(EmailDetail emailDetail, String template){
        try{
            Context context = new Context();

            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());
            context.setVariable("valuation", emailDetail.getValuation());
            context.setVariable("productName", emailDetail.getProductName());
            context.setVariable("date", emailDetail.getDate());
            context.setVariable("auctionId", emailDetail.getAuctionId());

            String text = templateEngine.process(template, context);

            // Creating a simple mail message
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Setting up necessary details
            mimeMessageHelper.setFrom("jeweljoust@gmail.com");
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text, true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());

            javaMailSender.send(mimeMessage);

        }catch (MessagingException messagingException){
            messagingException.printStackTrace();
        }
    }

    @Async
    public void sendMailNotificationSession(EmailDetail emailDetail, String template){
        try{
            Context context = new Context();

            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());
            context.setVariable("productName", emailDetail.getProductName());
            String text = templateEngine.process(template, context);

            // Creating a simple mail message
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Setting up necessary details
            mimeMessageHelper.setFrom("jeweljoust@gmail.com");
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text, true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());

            javaMailSender.send(mimeMessage);

        }catch (MessagingException messagingException){
            messagingException.printStackTrace();
        }
    }
    @Async
    public void sendOtp(EmailDetail emailDetail, String otp) {
    try {
        Context context = new Context();
        context.setVariable("name", emailDetail.getFullName());
        context.setVariable("otp", otp);
        context.setVariable("purpose", emailDetail.getSubject()); // Ví dụ: "Xác minh tài khoản"

        String text = templateEngine.process("otptemplate", context); // tên file: otp-template.html

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        mimeMessageHelper.setFrom("jeweljoust@gmail.com");
        mimeMessageHelper.setTo(emailDetail.getRecipient());
        mimeMessageHelper.setSubject("Your OTP Code - Tiximax");
        mimeMessageHelper.setText(text, true);

        javaMailSender.send(mimeMessage);

        System.out.println("OTP email sent successfully to " + emailDetail.getRecipient());
    } catch (MessagingException messagingException) {
        messagingException.printStackTrace();
        System.err.println("Failed to send OTP email: " + messagingException.getMessage());
    }
}
    //test
}

