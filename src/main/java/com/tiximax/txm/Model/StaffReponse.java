package com.tiximax.txm.Model;

import com.tiximax.txm.Entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class StaffReponse extends Staff {
    String token;
}