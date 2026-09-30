package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.common.exception.BaseException;
import org.example.giic_asset.common.exception.ErrorCode;
import org.example.giic_asset.dto.user.UserResponse;
import org.example.giic_asset.dto.user.UserStatusUpdateRequest;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.repository.UserRepository;
import org.example.giic_asset.entity.enums.NotificationType;
import org.example.giic_asset.event.SignupApprovedEvent;
import org.example.giic_asset.event.SignupRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<UserResponse> getUserList(Pageable pageable, Role role, UserStatus status, String keyword,
            Role currentUserRole) {
        log.debug("UserService.getUserList() called. role: {}, status: {}, keyword: {}, currentUserRole: {}", role,
                status, keyword, currentUserRole);

        if (currentUserRole == Role.USER) {
            throw new BaseException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (currentUserRole == Role.ADMIN) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), Role.SUPER));
        }

        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("name"), "%" + keyword + "%"),
                    cb.like(root.get("employeeNo"), "%" + keyword + "%")));
        }

        return userRepository.findAll(spec, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveUser(String employeeNo, User requester) {
        User targetUser = findUserByEmployeeNo(employeeNo);
        validateManagerAction(targetUser, requester);
        targetUser.updateStatusAndRole(UserStatus.APPROVED, null);

        notificationService.createNotification(
                targetUser.getEmployeeNo(),
                NotificationType.SIGNUP_APPROVED,
                "가입 승인 완료",
                "회원 가입이 승인되었습니다.",
                "USER",
                null);
        eventPublisher.publishEvent(new SignupApprovedEvent(
                targetUser.getEmployeeNo(),
                targetUser.getName(),
                targetUser.getDepartment(),
                targetUser.getEmail(),
                LocalDateTime.now()));
    }

    @Transactional
    public void rejectUser(String employeeNo, User requester) {
        User targetUser = findUserByEmployeeNo(employeeNo);
        validateManagerAction(targetUser, requester);
        targetUser.updateStatusAndRole(UserStatus.REJECTED, null);

        notificationService.createNotification(
                targetUser.getEmployeeNo(),
                NotificationType.SIGNUP_REJECTED,
                "가입 요청 반려됨",
                "회원 가입 요청이 반려되었습니다.",
                "USER",
                null);
        eventPublisher.publishEvent(new SignupRejectedEvent(
                targetUser.getEmployeeNo(),
                targetUser.getName(),
                targetUser.getDepartment(),
                targetUser.getEmail(),
                LocalDateTime.now()));
    }

    @Transactional
    public void updateUserRole(String employeeNo, Role newRole, User requester) {
        User targetUser = findUserByEmployeeNo(employeeNo);
        validateManagerAction(targetUser, requester);
        targetUser.updateStatusAndRole(null, newRole);
    }

    @Transactional
    public void deactivateUser(String employeeNo, User requester) {
        User targetUser = findUserByEmployeeNo(employeeNo);
        validateManagerAction(targetUser, requester);
        targetUser.updateStatusAndRole(UserStatus.INACTIVE, null);
    }

    private User findUserByEmployeeNo(String employeeNo) {
        return userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void validateManagerAction(User targetUser, User requester) {
        if (requester.getRole() == Role.USER) {
            throw new BaseException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        // SUPER 관리자는 변경 불가 (다른 SUPER도 불가)
        if (targetUser.getRole() == Role.SUPER) {
            throw new BaseException(ErrorCode.FORBIDDEN, "SUPER 관리자는 변경할 수 없습니다.");
        }

        // ADMIN은 ADMIN/SUPER 변경 불가
        if (requester.getRole() == Role.ADMIN) {
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.SUPER) {
                throw new BaseException(ErrorCode.FORBIDDEN, "ADMIN은 ADMIN 이상의 사용자를 관리할 수 없습니다.");
            }
        }
    }

    @Transactional
    public void updateUserStatus(String employeeNo, UserStatusUpdateRequest request) {
        log.info("UserService.updateUserStatus() called. employeeNo: {}, request: {}", employeeNo, request);
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> {
                    log.error("User not found for status update. employeeNo: {}", employeeNo);
                    return new BaseException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        user.updateStatusAndRole(request.getStatus(), request.getRole());
        log.info("User status updated. employeeNo: {}, status: {}", employeeNo, request.getStatus());
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String employeeNo) {
        log.debug("UserService.getMyInfo() called. employeeNo: {}", employeeNo);
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> {
                    log.error("User not found for getMyInfo. employeeNo: {}", employeeNo);
                    return new BaseException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });
        return UserResponse.from(user);
    }
}
