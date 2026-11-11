package com.hyojinahn.shiptime.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("message", "Hello World");
        return "index";
    }
}
