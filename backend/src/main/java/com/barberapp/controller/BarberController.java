package com.barberapp.controller;

import com.barberapp.dto.BarberDashboardResponse;
import com.barberapp.dto.BarberQueueItemResponse;
import com.barberapp.dto.CompleteCustomerRequest;
import com.barberapp.dto.MessageResponse;
import com.barberapp.dto.WalkInRequest;
import com.barberapp.service.BarberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/barber")
@Tag(name = "Barber", description = "Barber queue management and dashboard APIs")
public class BarberController {

    private final BarberService barberService;

    @GetMapping("/queue")
    @Operation(summary = "Get active barber queue")
    public List<BarberQueueItemResponse> getQueue() {
        return barberService.getQueue();
    }

    @PostMapping("/walkin")
    @Operation(summary = "Add a walk-in customer")
    public MessageResponse addWalkIn(@Valid @RequestBody WalkInRequest request) {
        return barberService.addWalkIn(request);
    }

    @PostMapping("/complete/{id}")
    @Operation(summary = "Complete a customer and record revenue")
    public MessageResponse completeCustomer(
            @Positive @PathVariable Long id,
            @Valid @RequestBody CompleteCustomerRequest request
    ) {
        return barberService.completeCustomer(id, request);
    }

    @PostMapping("/no-show/{id}")
    @Operation(summary = "Mark a customer as no-show")
    public MessageResponse markNoShow(@Positive @PathVariable Long id) {
        return barberService.markNoShow(id);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get barber dashboard summary")
    public BarberDashboardResponse getDashboard() {
        return barberService.getDashboard();
    }
}
