package com.lootsafe.repository;

import com.lootsafe.entity.DisputeChat;
import com.lootsafe.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<DisputeChat, UUID> {

    boolean existsDisputeChatByTransaction(Transaction transaction);


}
