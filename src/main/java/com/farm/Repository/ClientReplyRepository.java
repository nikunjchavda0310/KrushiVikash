package com.farm.Repository;

import com.farm.Entity.ClientReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientReplyRepository extends JpaRepository<ClientReply,Long> {
}
