package com.hmdp.dto;

import lombok.Data;

@Data
public class VoucherOrderMessage {
    private Long id;
    private Long userId;
    private Long voucherId;
}
