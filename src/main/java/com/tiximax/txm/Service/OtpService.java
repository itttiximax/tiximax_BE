package com.tiximax.txm.Service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Entity.Otp;
import com.tiximax.txm.Model.EmailDetail;
import com.tiximax.txm.Repository.AuthenticationRepository;
import com.tiximax.txm.Repository.OtpRepository;
@Service
public class OtpService {

    @Autowired
    private final AuthenticationRepository authenticationRepository; 

    private final Random random = new Random();
    @Autowired
    private final OtpRepository otpRepository;
    @Autowired
    private EmailService emailService;

    public OtpService( AuthenticationRepository authenticationRepository, OtpRepository otpRepository ,EmailService emailService) {
        this.authenticationRepository = authenticationRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

       public Otp generateOtp(Account account) {
        Otp otp = new Otp();
        otp.setAccount(account);
        otp.setCode(String.format("%06d", random.nextInt(999999)));
        otp.setExpiration(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        return otpRepository.save(otp);
    }

     public void sendOtpToEmail(String email) {
        Account account = authenticationRepository.findByEmail(email);
        if (account == null) {
            throw new IllegalArgumentException("User not found");
        }
        Otp otp = generateOtp(account);

        EmailDetail emailDetail = new EmailDetail();
        emailDetail.setRecipient(account.getEmail());
        emailDetail.setSubject("Your OTP Code");
        emailDetail.setMsgBody("Your OTP is: " + otp.getCode() + "\nIt will expire in 5 minutes.");

        emailService.sendMailTemplate(emailDetail);
    }

 public boolean validateOtp(String email, String code) throws Exception {
        var otpOptional = otpRepository.findTopByAccount_EmailOrderByExpirationDesc(
                email);

        if (otpOptional.isEmpty()) return false;
        Otp otp = otpOptional.get();

        if (otp.isUsed()){
            throw new Exception("mã OTP của bạn đã được sử dụng");
        }
        if ( otp.isExpired()){
            throw new Exception("mã OTP của bạn đã hết hạn");
        }
         
        if (!otp.getCode().equals(code)) {
            throw new Exception("mã OTP của bạn không đúng");
        }

       
        otp.setUsed(true);
        otpRepository.save(otp);
        return true;
    }
    
}
