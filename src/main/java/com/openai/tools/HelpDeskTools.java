package com.openai.tools;

import com.openai.entity.HelpDeskTicket;
import com.openai.model.TicketRequest;
import com.openai.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpDeskTools {
    private static final Logger logger = LoggerFactory.getLogger(HelpDeskTools.class);
    private final HelpDeskTicketService helpDeskTicketService;

    @Tool(name = "createTicket", description = "Create the support ticket", returnDirect = true)
    String createTicket(@ToolParam(description = "Details to create a support ticket") TicketRequest ticketRequest, ToolContext toolContext) {
        String username = (String) toolContext.getContext().get("username");
        logger.info("Create support ticket for user: {} with details {}", username, ticketRequest);
        HelpDeskTicket savedTicket = helpDeskTicketService.createTicket(ticketRequest, username);
        logger.info("Ticket created successfully. Ticket ID: {}, Username: {}", savedTicket.getId(), savedTicket.getUsername());
        return "Ticket #" + savedTicket.getId() + " create successfully for user " + savedTicket.getUsername();
    }

    @Tool(description = "Fetch the status of the open tickets based on given username")
    List<HelpDeskTicket> getTicketStatus(ToolContext toolContext) {
        String username = (String) toolContext.getContext().get("username");
        logger.info("Fetch the status of the open tickets based on username: {}", username);
        List<HelpDeskTicket> tickets = helpDeskTicketService.getTicketsByUsername(username);
        logger.info("Found {} tickets for user {}", tickets.size(), username);
        return tickets;
    }
}
