package com.barberapp.repository;

import com.barberapp.entity.RevenueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RevenueEntryRepository extends JpaRepository<RevenueEntry, Long> {

    @Query("""
            select coalesce(sum(revenueEntry.amount), 0)
            from RevenueEntry revenueEntry
            where revenueEntry.createdAt >= :createdAtStart
              and revenueEntry.createdAt < :createdAtEnd
            """)
    BigDecimal sumAmountByCreatedAtBetween(
            @Param("createdAtStart") LocalDateTime createdAtStart,
            @Param("createdAtEnd") LocalDateTime createdAtEnd
    );
}
