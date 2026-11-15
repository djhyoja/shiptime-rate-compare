package com.hyojinahn.shiptime.web;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.service.RateQuote;
import com.hyojinahn.shiptime.service.ShipmentResult;
import com.hyojinahn.shiptime.service.ShipmentService;

/**
 * Step 3: create a shipment for the chosen rate, show the result and serve its label.
 * Only the quote ID comes from the page; the service takes everything else from the stored quote.
 */
@Controller
public class ShipmentController {

    static final String SESSION_SHIPMENT = "shipment";
    static final String SESSION_SHIPMENT_IDS = "shipmentIds";

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/shipments")
    public String createShipment(@RequestParam(required = false) String quoteId,
                                 HttpSession session,
                                 Model model) {
        if (!(session.getAttribute(RateController.SESSION_QUOTE) instanceof RateQuote quote)) {
            return "redirect:/";
        }

        Optional<ShipmentResult> created;
        try {
            created = shipmentService.createShipment(quote, quoteId);
        } catch (ShipTimeApiException e) {
            model.addAttribute("quote", quote);
            model.addAttribute("errorMessage", userMessage(e));
            model.addAttribute("errorDetails", e.details());
            return "rates";
        }
        if (created.isEmpty()) {
            return "redirect:/rates";
        }
        ShipmentResult shipment = created.get();

        // A quote can be used once. Removing it stops a second shipment on refresh or back.
        session.removeAttribute(RateController.SESSION_QUOTE);
        session.setAttribute(SESSION_SHIPMENT, shipment);
        rememberShipment(session, shipment.shipId());
        return "redirect:/shipments/result";
    }

    @GetMapping("/shipments/result")
    public String showShipment(HttpSession session, Model model) {
        if (!(session.getAttribute(SESSION_SHIPMENT) instanceof ShipmentResult shipment)) {
            return "redirect:/";
        }
        model.addAttribute("shipment", shipment);
        return "shipment";
    }

    /**
     * The label needs ShipTime credentials, so the browser cannot open ShipTime's label URL.
     * This app downloads it and passes the PDF on, only for shipments created in this session.
     */
    @GetMapping("/shipments/{shipId}/label")
    public ResponseEntity<byte[]> downloadLabel(@PathVariable long shipId, HttpSession session) {
        if (!shipmentIds(session).contains(shipId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        byte[] pdf;
        try {
            pdf = shipmentService.getLabel(shipId);
        } catch (ShipTimeApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Label is not available", e);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("label-" + shipId + ".pdf").build().toString())
                .body(pdf);
    }

    @SuppressWarnings("unchecked")
    private static void rememberShipment(HttpSession session, long shipId) {
        Set<Long> ids = (Set<Long>) session.getAttribute(SESSION_SHIPMENT_IDS);
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(shipId);
        session.setAttribute(SESSION_SHIPMENT_IDS, ids);
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> shipmentIds(HttpSession session) {
        Object ids = session.getAttribute(SESSION_SHIPMENT_IDS);
        return ids instanceof Set<?> set ? (Set<Long>) set : Set.of();
    }

    private static String userMessage(ShipTimeApiException e) {
        return switch (e.reason()) {
            case REJECTED -> "ShipTime could not create this shipment. The quote may have expired. Please get new rates.";
            case UNAVAILABLE -> "The shipping service is not available right now. Please try again later.";
        };
    }
}
