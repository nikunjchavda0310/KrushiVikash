package com.farm.Repository;

import com.farm.Entity.Farmer;
import com.farm.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 1. For Farmer Dashboard: Get ONLY messages sent by ADMIN to this farmer
    List<Message> findByFarmerAndSenderTypeOrderByCreatedAtDesc(Farmer farmer, String senderType);

    // 2. For Farmer Badge: Count ONLY unread messages sent by ADMIN
    List<Message> findByFarmerAndSenderTypeAndIsReadFalse(Farmer farmer, String senderType);

    // 3. For Admin Dashboard: Get ALL unread replies sent by any FARMER
    List<Message> findBySenderTypeAndIsReadFalse(String senderType);

    // (Keep this if you use it for general history, but the ones above are more important now)
    List<Message> findByFarmerOrderByCreatedAtDesc(Farmer farmer);

    List<Message> findTop20BySenderTypeOrderByCreatedAtDesc(String farmer);

    List<Message> findByFarmerOrderByCreatedAtAsc(Farmer selectedFarmer);
}