package com.tiximax.txm.Service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Model.EmailDetail;
import com.tiximax.txm.Repository.AuthenticationRepository;
@Service
public class OtpService {

    @Autowired
    private final AuthenticationRepository authenticationRepository; 

    private final Random random = new Random();

    @Autowired
    private EmailService emailService;

    public OtpService( AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    public String generateOtp(Account account) {
        String otp = String.format("%06d", random.nextInt(999999));
        account.setOtpCode(otp);
        account.setOtpExpiration(LocalDateTime.now().plusMinutes(5));
        authenticationRepository.save(account);
        return otp;
    }

    public boolean validateOtp(Account account, String otp) {
        if (account.getOtpCode() == null || account.getOtpExpiration() == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(account.getOtpExpiration())) {
            return false;
        }
        return account.getOtpCode().equals(otp);
    }
    public void sendOtpToEmail(String email) {
    Account account = authenticationRepository.findByEmail(email);
    if (account == null) {
        throw new IllegalArgumentException("User not found");
    }

    // Tạo OTP
    String otp = generateOtp(account);

    // Tạo email detail
    EmailDetail emailDetail = new EmailDetail();
    emailDetail.setRecipient(account.getEmail()); // hoặc username nếu là email
    emailDetail.setSubject("Your OTP Code");
    emailDetail.setMsgBody("Your OTP is: " + otp + "\nIt will expire in 5 minutes.");

    // Gửi email
    emailService.sendMailTemplate(emailDetail);
}
}
