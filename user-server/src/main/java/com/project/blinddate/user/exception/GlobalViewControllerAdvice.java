package com.project.blinddate.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalViewControllerAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, RedirectAttributes redirectAttributes) {
        log.warn("View controller illegal argument exception: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/login"; // Default redirect to login or home
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        log.error("View controller internal server error: ", e);
        redirectAttributes.addFlashAttribute("error", "알 수 없는 오류가 발생했습니다.");
        return "redirect:/";
    }
}
