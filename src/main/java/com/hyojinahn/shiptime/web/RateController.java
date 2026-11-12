package com.hyojinahn.shiptime.web;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.service.RateQuote;
import com.hyojinahn.shiptime.service.RateService;

/**
 * Step 1: enter addresses and package, then show the rates.
 * The quote is kept in the session and shown after a redirect, so refreshing
 * the result page does not send the form again.
 */
@Controller
public class RateController {

    static final String SESSION_FORM = "rateForm";
    static final String SESSION_QUOTE = "rateQuote";

    static final List<String> PROVINCES = List.of(
            "AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT");

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    /** Trims text input and turns blank input into null, so each empty field shows one message. */
    @InitBinder("rateForm")
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("provinces")
    public List<String> provinces() {
        return PROVINCES;
    }

    @GetMapping("/")
    public String form(HttpSession session, Model model) {
        Object previous = session.getAttribute(SESSION_FORM);
        model.addAttribute("rateForm", previous instanceof RateForm form ? form : new RateForm());
        return "index";
    }

    @PostMapping("/rates")
    public String getRates(@Valid @ModelAttribute("rateForm") RateForm form,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "index";
        }
        session.setAttribute(SESSION_FORM, form);

        RateQuote quote;
        try {
            quote = rateService.getRates(form);
        } catch (ShipTimeApiException e) {
            model.addAttribute("errorMessage", userMessage(e));
            model.addAttribute("errorDetails", e.details());
            return "index";
        }

        if (quote.isEmpty()) {
            model.addAttribute("infoMessage", "No rates found for this route. Please check the addresses and package.");
            return "index";
        }
        session.setAttribute(SESSION_QUOTE, quote);
        return "redirect:/rates";
    }

    @GetMapping("/rates")
    public String showRates(HttpSession session, Model model) {
        if (!(session.getAttribute(SESSION_QUOTE) instanceof RateQuote quote)) {
            return "redirect:/";
        }
        model.addAttribute("quote", quote);
        return "rates";
    }

    private static String userMessage(ShipTimeApiException e) {
        return switch (e.reason()) {
            case REJECTED -> "ShipTime could not accept these details. Please check the form.";
            case UNAVAILABLE -> "The shipping service is not available right now. Please try again later.";
        };
    }
}
