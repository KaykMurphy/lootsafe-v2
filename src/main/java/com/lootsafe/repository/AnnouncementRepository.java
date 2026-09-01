package com.lootsafe.repository;

import com.lootsafe.entity.Announcement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Announcement a WHERE a.token = :token")
    Optional<Announcement> findByTokenWithLock(@Param("token") String token);

    List<Announcement> findBySellerId(UUID sellerId);

    Optional<Announcement> findByIdAndSellerId(UUID id, UUID sellerId);

}
