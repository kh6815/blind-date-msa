package com.project.blinddate.chat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalViewControllerAdvice {

    @Value("${external.user-server.url}")
    private String externalUserServerUrl;

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, RedirectAttributes redirectAttributes) {
        log.warn("View controller illegal argument exception: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", e.getMessage());
//        return "redirect:/chats"; // Redirect to chat list
        return "redirect:" + externalUserServerUrl + "/login";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        log.error("View controller internal server error: ", e);
        redirectAttributes.addFlashAttribute("error", "알 수 없는 오류가 발생했습니다.");
//        return "redirect:/chats";
        return "redirect:" + externalUserServerUrl + "/login";
    }
}
