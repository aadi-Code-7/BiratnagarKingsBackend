package com.biratkings.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** When the React build is copied into src/main/resources/static, deep links must serve index.html. */
@Controller
public class SpaForwardController {
    @GetMapping({"/store", "/cart", "/players/{id}"})
    public String spa() { return "forward:/index.html"; }
}
