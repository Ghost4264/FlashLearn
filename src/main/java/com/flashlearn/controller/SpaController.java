package com.flashlearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Клиентские маршруты React: отдаёт index.html из classpath:/static.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/login",
            "/register",
            "/faq",
            "/study",
            "/profile",
            "/admin",
            "/decks",
            "/decks/{id:[0-9]+}"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
