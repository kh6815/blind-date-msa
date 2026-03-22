package com.project.blinddate.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 Thymeleaf 템플릿에서 사용할 수 있는 전역 모델 속성 설정.
 */
@ControllerAdvice
public class GlobalModelAttributeConfig {

    @Value("${app.display-name}")
    private String appDisplayName;

    /**
     * 모든 컨트롤러의 모델에 앱 이름을 자동으로 추가.
     *
     * @return 앱 표시 이름
     */
    @ModelAttribute("appName")
    public String appName() {
        return appDisplayName;
    }
}