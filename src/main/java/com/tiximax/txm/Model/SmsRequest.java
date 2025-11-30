package com.tiximax.txm.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter

public class SmsRequest {
    private long amount;
    private String content;
}
