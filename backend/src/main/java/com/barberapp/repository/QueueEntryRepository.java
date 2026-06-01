package com.barberapp.repository;

import com.barberapp.domain.QueueStatus;
import com.barberapp.entity.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    long countByStatus(QueueStatus status);

    List<QueueEntry> findByStatusInOrderByJoinedAtAscIdAsc(Collection<QueueStatus> statuses);

    Optional<QueueEntry> findFirstByStatusOrderByJoinedAtAscIdAsc(QueueStatus status);

    @Query("""
            select count(queueEntry)
            from QueueEntry queueEntry
            where queueEntry.status = :status
              and queueEntry.completedAt >= :completedAtStart
              and queueEntry.completedAt < :completedAtEnd
            """)
    long countByStatusAndCompletedAtBetween(
            @Param("status") QueueStatus status,
            @Param("completedAtStart") LocalDateTime completedAtStart,
            @Param("completedAtEnd") LocalDateTime completedAtEnd
    );

    long deleteByStatusInAndJoinedAtBefore(Collection<QueueStatus> statuses, LocalDateTime joinedAt);
}
