package com.remstem.game.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/host/**", "/play/**", "/stats/**"})
    public String spa() {
        return "forward:/index.html";
    }
}
