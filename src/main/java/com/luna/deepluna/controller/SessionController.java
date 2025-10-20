package com.luna.deepluna.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/capi/chat")
@RequiredArgsConstructor
@Tag(name = "ChatController")
public class SessionController {

}
