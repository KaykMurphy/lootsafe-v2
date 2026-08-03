package com.lootsafe.repository;

import com.lootsafe.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByToken(String token);

    List<Announcement> findBySellerId(UUID sellerId);


}
