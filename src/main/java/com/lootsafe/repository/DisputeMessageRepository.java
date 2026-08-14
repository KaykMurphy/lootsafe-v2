package com.lootsafe.repository;

import com.lootsafe.entity.DisputeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeMessageRepository extends JpaRepository<DisputeMessage, UUID> {

    List<DisputeMessage> findByDisputeChatIdOrderByCreatedAtAsc(UUID disputeChatId);

}
