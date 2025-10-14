package com.tiximax.txm.API;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.tiximax.txm.Service.OtpService;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
       
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        
        otpService.sendOtpToEmail(email);
        return ResponseEntity.ok("OTP sent successfully");
    }


    }

