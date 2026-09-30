package org.example.giic_asset.controller;

import org.example.giic_asset.common.exception.GlobalExceptionHandler;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.security.CustomUserDetails;
import org.example.giic_asset.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerActionTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private User requester;

    @BeforeEach
    void setUp() {
        requester = User.builder()
                .employeeNo("2025009")
                .password("encoded-password")
                .name("슈퍼관리자")
                .role(Role.SUPER)
                .status(UserStatus.APPROVED)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(requester);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType() == CustomUserDetails.class;
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsLegacyUserWhoseEmployeeNumberIsDot() throws Exception {
        mockMvc.perform(put("/api/users/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNo\":\".\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).rejectUser(".", requester);
    }
}
