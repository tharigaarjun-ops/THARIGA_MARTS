package com.tharigamart.service;

import com.tharigamart.dto.TicketDTO;
import com.tharigamart.dto.TicketRequest;
import com.tharigamart.entity.SupportTicket;
import com.tharigamart.entity.TicketStatus;
import com.tharigamart.entity.User;
import com.tharigamart.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Customer support tickets. */
@Service
public class TicketService {

    private final SupportTicketRepository ticketRepository;

    public TicketService(SupportTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketDTO create(User currentUser, TicketRequest request) {
        SupportTicket ticket = new SupportTicket(
                currentUser,
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getSubject().trim(),
                request.getMessage().trim());
        ticket.setStatus(TicketStatus.OPEN);
        return toDto(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketDTO> myTickets(User currentUser) {
        return ticketRepository.findByUserIdOrderByIdDesc(currentUser.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    /** All tickets (admin support view). */
    @Transactional(readOnly = true)
    public List<TicketDTO> listAll() {
        return ticketRepository.findAllByOrderByIdDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public TicketDTO toDto(SupportTicket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setName(ticket.getName());
        dto.setEmail(ticket.getEmail());
        dto.setSubject(ticket.getSubject());
        dto.setMessage(ticket.getMessage());
        dto.setStatus(ticket.getStatus() == null ? null : ticket.getStatus().name());
        dto.setAdminReply(ticket.getAdminReply());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }
}
