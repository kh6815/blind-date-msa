package com.project.blinddate.chat.controller;

import com.project.blinddate.chat.service.ChatImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/chats/image")
@RequiredArgsConstructor
public class ChatImageController {
    private final ChatImageService chatImageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = chatImageService.uploadImage(file);
        if (imageUrl == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(Map.of("url", imageUrl));
    }
}
