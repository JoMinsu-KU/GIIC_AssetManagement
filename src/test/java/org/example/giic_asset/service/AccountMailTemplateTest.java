package org.example.giic_asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMailTemplateTest {

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersAllAccountMailTemplatesWithApplicantDataAndLinks() {
        Context context = new Context(Locale.KOREAN);
        context.setVariable("employeeNo", "2026001");
        context.setVariable("name", "테스트 사용자");
        context.setVariable("department", "경남지능화혁신사업단");
        context.setVariable("email", "user@example.com");
        context.setVariable("formattedDate", "2026. 07. 14. 14:30");
        context.setVariable("loginUrl", "http://203.253.164.48:3000/login");
        context.setVariable("reviewUrl", "http://203.253.164.48:3000/users?status=PENDING");

        assertTemplate("mail/signup-received", context, "정상적으로 접수했습니다.");
        assertTemplate("mail/signup-approved", context, "http://203.253.164.48:3000/login");
        assertTemplate("mail/signup-rejected", context, "승인되지 않았습니다.");
        assertTemplate("mail/super-signup-request", context, "users?status=PENDING");
    }

    private void assertTemplate(String template, Context context, String expected) {
        String html = templateEngine.process(template, context);
        assertThat(html)
                .contains("테스트 사용자")
                .contains("2026001")
                .contains(expected)
                .contains("style=\"margin:0;padding:0;background-color:#eef2f7")
                .contains("style=\"padding:42px 38px 36px;background-color:#ffffff")
                .contains("style=\"width:100%;margin-top:28px;border:1px solid #e2e8f1")
                .doesNotContain("th:text", "th:href", "th:replace");
    }
}
