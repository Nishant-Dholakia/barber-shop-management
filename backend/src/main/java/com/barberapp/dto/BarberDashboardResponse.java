package com.barberapp.dto;

import java.math.BigDecimal;

public record BarberDashboardResponse(
        BigDecimal todayRevenue,
        long customersServed,
        long queueLength
) {
}
