# GIIC_Asset 테스팅 가이드

## 1. 테스트 전략 개요 (Testing Strategy Overview)

### 1.1 테스트 피라미드
본 프로젝트는 **테스트 피라미드** 전략을 따릅니다. 하위 레벨의 테스트를 풍부하게 작성하여 빠른 피드백을 확보하고, 상위 레벨의 테스트로 시스템의 통합성을 검증합니다.

| 레벨 | 테스트 유형 | 도구/기술 | 비중 | 목적 |
|---|---|---|---|---|
| **L3** | **E2E Tests** | Cypress / Selenium | 10% | 실제 사용자 시나리오 검증, 프론트엔드-백엔드 연동 확인 |
| **L2** | **Integration Tests** | SpringBootTest, TestContainers | 20% | 컴포넌트 간 상호작용, 데이터베이스/API 연동 검증 |
| **L1** | **Unit Tests** | JUnit 5, Mockito | 70% | 비즈니스 로직 검증, 개별 메서드/클래스 단위 테스트 |

### 1.2 테스트 커버리지 목표
- **Line Coverage**: 80% 이상
- **Branch Coverage**: 70% 이상
- **주요 비즈니스 로직**: 100% 테스트 필수

---

## 2. 단위 테스트 (Unit Tests)

단위 테스트는 외부 의존성(DB, Network 등)을 모두 Mocking하고, 순수한 비즈니스 로직만을 검증합니다.

### 2.1 기본 설정 (JUnit 5 + Mockito)
Spring Boot Starter Test에 기본 포함된 JUnit 5와 Mockito를 사용합니다.

```java
// build.gradle (기본 포함)
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

### 2.2 Service 레이어 테스트 예시
`AssetService`의 자산 등록 로직을 테스트하는 예시입니다.

```java
package org.example.giic_asset.service;

import org.example.giic_asset.domain.Asset;
import org.example.giic_asset.dto.AssetRequestDto;
import org.example.giic_asset.repository.AssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    @Test
    @DisplayName("자산 등록 성공 테스트")
    void createAsset_Success() {
        // Given
        AssetRequestDto request = new AssetRequestDto("A-001", "MacBook Pro", "NORMAL");
        Asset savedAsset = Asset.builder()
                .assetNo("A-001")
                .name("MacBook Pro")
                .status("NORMAL")
                .build();

        given(assetRepository.save(any(Asset.class))).willReturn(savedAsset);

        // When
        Asset result = assetService.registerAsset(request);

        // Then
        assertThat(result.getAssetNo()).isEqualTo("A-001");
        assertThat(result.getName()).isEqualTo("MacBook Pro");
    }
}
```

### 2.3 Repository 테스트 예시 (@DataJpaTest)
JPA 매핑 및 쿼리 메서드가 정상 동작하는지 검증합니다. `@DataJpaTest`는 기본적으로 인메모리 DB(H2)를 사용하지만, 실제 MySQL 동작을 검증하려면 TestContainers 사용을 권장합니다.

```java
package org.example.giic_asset.repository;

import org.example.giic_asset.domain.Asset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Test
    @DisplayName("자산번호로 자산 조회")
    void findByAssetNo_Success() {
        // Given
        Asset asset = new Asset("A-001", "MacBook Pro", "NORMAL");
        assetRepository.save(asset);

        // When
        Asset found = assetRepository.findByAssetNo("A-001").orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("MacBook Pro");
    }
}
```

### 2.4 테스트 네이밍 컨벤션
- `메서드명_테스트상태_기대동작` 패턴을 권장합니다.
- 예: `createAsset_WithDuplicateId_ThrowsException`
- `@DisplayName`을 사용하여 한글로 테스트 내용을 명확히 기술합니다.

---

## 3. 통합 테스트 (Integration Tests)

애플리케이션 컨텍스트를 로드하여 여러 컴포넌트 간의 상호작용을 검증합니다.

### 3.1 @SpringBootTest 설정
전체 애플리케이션 컨텍스트를 로드합니다.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional // 테스트 종료 후 데이터 롤백
class IntegrationTest { ... }
```

### 3.2 TestContainers로 MySQL 테스트
실제 운영 환경과 동일한 MySQL 환경에서 테스트하기 위해 TestContainers를 사용합니다.
*참고: `build.gradle`에 `org.testcontainers:mysql` 의존성 추가 필요.*

```groovy
// build.gradle dependencies
testImplementation "org.testcontainers:junit-jupiter:1.19.3"
testImplementation "org.testcontainers:mysql:1.19.3"
```

```java
@Testcontainers
@SpringBootTest
class AssetIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void testDatabaseConnection() {
        assertThat(mysql.isRunning()).isTrue();
    }
}
```

### 3.3 API 통합 테스트 (@WebMvcTest vs @SpringBootTest)
API 엔드포인트 자체만 검증할 때는 `@WebMvcTest`를, 전체 흐름 검증 시에는 `@SpringBootTest` + `MockMvc`를 사용합니다.

```java
@SpringBootTest
@AutoConfigureMockMvc
class AssetApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockCustomUser // 커스텀 보안 어노테이션 사용 (아래 4번 참조)
    void createAsset_ApiTest() throws Exception {
        AssetRequestDto request = new AssetRequestDto("A-001", "MacBook", "NORMAL");

        mockMvc.perform(post("/api/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.asset_no").value("A-001"));
    }
}
```

---

## 4. 보안 테스트 (Security Testing)

Spring Security 설정과 인증/인가 로직을 테스트합니다.

### 4.1 @WithMockUser 사용법
간단한 역할(Role) 기반 테스트에 사용합니다.

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void deleteAsset_ByAdmin_Success() throws Exception {
    mockMvc.perform(delete("/api/assets/1"))
            .andExpect(status().isOk());
}

@Test
@WithMockUser(username = "user", roles = {"USER"})
void deleteAsset_ByUser_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/assets/1"))
            .andExpect(status().isForbidden());
}
```

### 4.2 커스텀 JWT 테스트 어노테이션
JWT 클레임(Claim) 정보나 UserDetails의 상세 정보를 모킹해야 할 때 커스텀 어노테이션을 생성합니다.

```java
// @WithMockCustomUser 어노테이션 정의
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String username() default "user";
    String role() default "USER";
}

// SecurityContextFactory 구현
public class WithMockCustomUserSecurityContextFactory 
    implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        UserDetails principal = User.withUsername(customUser.username())
                .password("password")
                .roles(customUser.role())
                .build();
                
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "password", principal.getAuthorities());
                
        context.setAuthentication(auth);
        return context;
    }
}
```

---

## 5. 프론트엔드 테스트 (Frontend Testing)

React 애플리케이션(`src/main/giic_frontend`)의 테스트 전략입니다.

### 5.1 React Testing Library 설정
`create-react-app`으로 생성된 프로젝트에는 이미 설정되어 있습니다.

```bash
cd src/main/giic_frontend
npm test
```

### 5.2 컴포넌트 렌더링 테스트
```javascript
// App.test.js
import { render, screen } from '@testing-library/react';
import App from './App';

test('renders login link', () => {
  render(<App />);
  const linkElement = screen.getByText(/로그인/i);
  expect(linkElement).toBeInTheDocument();
});
```

### 5.3 API 모킹 (MSW - Mock Service Worker)
백엔드 API가 준비되지 않았거나 독립적인 테스트를 위해 API를 모킹합니다.

```javascript
// src/mocks/handlers.js
import { rest } from 'msw';

export const handlers = [
  rest.get('/api/assets', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        data: [{ id: 1, name: 'MacBook Pro' }]
      })
    );
  }),
];
```

---

## 6. 테스트 데이터 관리 (Test Data Management)

### 6.1 @Sql로 테스트 데이터 로드
테스트 실행 전 특정 SQL을 실행하여 초기 데이터를 세팅합니다.

```java
@Test
@Sql(scripts = "/data/test-assets.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
void findAllAssets_Test() {
    // test-assets.sql에 정의된 데이터가 로드된 상태
}
```

### 6.2 Fixture 패턴 사용
테스트 객체 생성을 돕는 Factory 클래스를 만들어 사용합니다.

```java
public class AssetFixture {
    public static Asset createDefaultAsset() {
        return Asset.builder()
                .assetNo("A-DEFAULT")
                .name("Default Asset")
                .build();
    }
}
```

---

## 7. 테스트 실행 방법 (How to Run Tests)

### 7.1 Gradle 명령어
터미널에서 전체 테스트를 실행합니다.

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스 테스트 실행
./gradlew test --tests "org.example.giic_asset.service.AssetServiceTest"
```

### 7.2 IDE에서 실행
IntelliJ IDEA나 VSCode의 Test Runner 기능을 사용하여 개별 메서드나 클래스 단위로 실행 및 디버깅합니다.

### 7.3 커버리지 리포트 (JaCoCo)
`build.gradle`에 JaCoCo 플러그인을 추가하여 리포트를 생성할 수 있습니다.

```bash
# 리포트 생성
./gradlew jacocoTestReport
# 생성 경로: build/reports/jacoco/test/html/index.html
```

---

## 8. CI/CD 테스트 통합

GitHub Actions를 사용한 자동화된 테스트 워크플로우 예시입니다.

```yaml
# .github/workflows/test.yml
name: Java CI with Gradle

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run Tests
      run: ./gradlew test
      
    - name: Upload Test Results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: build/reports/tests/test/
```
