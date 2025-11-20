package com.tiximax.txm.Model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.tiximax.txm.Entity.Customer;
import com.tiximax.txm.Entity.Address;

@Data
public class CustomerResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String customerCode;
    private String source;
    private Long staffId;
    private Double totalWeight;
    private BigDecimal balance;
    private List<AddressDTO> addresses;

    // ✅ Mapper static method nằm trong chính DTO này
    public static CustomerResponseDTO fromEntity(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();

        dto.setId(customer.getAccountId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setCustomerCode(customer.getCustomerCode());
        dto.setSource(customer.getSource());
        dto.setStaffId(customer.getStaffId());
        dto.setTotalWeight(customer.getTotalWeight());
        dto.setBalance(customer.getBalance());

        // Map danh sách Address -> AddressDTO
        dto.setAddresses(
            customer.getAddresses().stream()
                .map(addr -> {
                    AddressDTO a = new AddressDTO();
                    a.setId(addr.getAddressId());
                    a.setAddressName(addr.getAddressName());
                    return a;
                })
                .collect(Collectors.toList())
        );

        return dto;
    }
}
