package com.farm.Repository;

import com.farm.Entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface ContactRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findByClientIdOrderBySubmittedAtAsc(Long clientId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE contact_message SET reply = :reply, replied_at = :time, seen = 1 WHERE id = :id", nativeQuery = true)
    void updateAdminReply(@Param("id") Long id, @Param("reply") String reply, @Param("time") LocalDateTime time);

    long countByClientIdAndSeenFalse(Long id);


    List<ContactMessage> findByClientIdOrderByIdDesc(Long clientId);
}