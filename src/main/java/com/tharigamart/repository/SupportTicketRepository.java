package com.tharigamart.repository;

import com.tharigamart.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserIdOrderByIdDesc(Long userId);

    List<SupportTicket> findAllByOrderByIdDesc();
}
