package com.hyojinahn.shiptime.web;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;
import com.hyojinahn.shiptime.service.RateOption;
import com.hyojinahn.shiptime.service.RateQuote;
import com.hyojinahn.shiptime.service.ShipmentService;

/**
 * Step 3: create a shipment for the chosen rate and show the result.
 * Only the quote ID comes from the page. Carrier, service and the rate request
 * are taken from the quote stored in the session.
 */
@Controller
public class ShipmentController {

    static final String SESSION_SHIPMENT = "shipment";
    static final String SESSION_SHIPPED_OPTION = "shippedOption";

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
        Optional<RateOption> selected = quoteId == null ? Optional.empty() : quote.option(quoteId);
        if (selected.isEmpty()) {
            return "redirect:/rates";
        }
        RateOption option = selected.get();

        ShipmentResponse shipment;
        try {
            shipment = shipmentService.createShipment(quote.request(), option);
        } catch (ShipTimeApiException e) {
            model.addAttribute("quote", quote);
            model.addAttribute("errorMessage", userMessage(e));
            model.addAttribute("errorDetails", e.details());
            return "rates";
        }

        // A quote can be used once. Removing it stops a second shipment on refresh or back.
        session.removeAttribute(RateController.SESSION_QUOTE);
        session.setAttribute(SESSION_SHIPMENT, shipment);
        session.setAttribute(SESSION_SHIPPED_OPTION, option);
        return "redirect:/shipments/result";
    }

    @GetMapping("/shipments/result")
    public String showShipment(HttpSession session, Model model) {
        if (!(session.getAttribute(SESSION_SHIPMENT) instanceof ShipmentResponse shipment)) {
            return "redirect:/";
        }
        model.addAttribute("shipment", shipment);
        model.addAttribute("option", session.getAttribute(SESSION_SHIPPED_OPTION));
        return "shipment";
    }

    private static String userMessage(ShipTimeApiException e) {
        return switch (e.reason()) {
            case REJECTED -> "ShipTime could not create this shipment. The quote may have expired. Please get new rates.";
            case UNAVAILABLE -> "The shipping service is not available right now. Please try again later.";
        };
    }
}
