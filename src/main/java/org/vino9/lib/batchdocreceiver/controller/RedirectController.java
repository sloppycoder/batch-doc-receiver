package org.vino9.lib.batchdocreceiver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping("/")
    String redirectToH2Console() {
        return "redirect:/h2-console";
    }
}
