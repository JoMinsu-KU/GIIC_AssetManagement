# HTTP 환경에서의 Spring Security 설정 가이드

## 📋 개요

본 문서는 **내부망(인트라넷)에서 HTTP로 접속**하는 환경에서 Spring Security를 설정하는 방법을 안내합니다.

**결론: Spring Security는 HTTPS 없이도 완벽하게 작동합니다!** ✅

---

## 1. HTTPS vs HTTP 환경 비교

### 1.1 Spring Security 기능별 비교

| 기능 | HTTP | HTTPS | 비고 |
|------|------|-------|------|
| **인증 (Authentication)** | ✅ 가능 | ✅ 가능 | 동일하게 작동 |
| **권한 관리 (Authorization)** | ✅ 가능 | ✅ 가능 | 동일하게 작동 |
| **JWT 토큰** | ✅ 가능 | ✅ 권장 | HTTP에서도 작동, 단 평문 전송 |
| **세션 관리** | ✅ 가능 | ✅ 가능 | 동일하게 작동 |
| **CSRF 보호** | ✅ 가능 | ✅ 가능 | 동일하게 작동 |
| **비밀번호 암호화** | ✅ 가능 | ✅ 가능 | 서버 내부 처리로 동일 |

### 1.2 차이점

| 항목 | HTTP (내부망) | HTTPS (공개망) |
|------|---------------|----------------|
| **데이터 전송** | 평문 전송 | 암호화 전송 |
| **토큰 노출** | 네트워크 스니핑 시 노출 | 암호화로 보호 |
| **Secure Cookie** | 사용 불가 | 사용 가능 |
| **보안 수준** | 내부망 신뢰 필요 | 높음 |
| **구현 복잡도** | 낮음 | 중간 (SSL 인증서) |

---

## 2. 내부망 HTTP 환경 보안 전략

### 2.1 **권장: JWT 기반 인증** ✅

#### 이유
1. **Stateless**: 세션 저장소 불필요
2. **확장성**: 수평 확장 용이
3. **모바일 친화적**: Flutter 앱과 쉽게 통합
4. **내부망 안전성**: 물리적으로 격리된 네트워크

#### 보안 전제 조건
- ✅ 내부 네트워크는 외부 인터넷과 분리
- ✅ 방화벽으로 외부 접근 차단
- ✅ VPN 또는 사내망에서만 접속
- ✅ 신뢰할 수 있는 사용자만 접근

### 2.2 내부망에서의 JWT 전략

```
보안 계층:
1. 네트워크 보안 (방화벽)  ← 1차 방어
2. 인증/권한 (Spring Security + JWT)  ← 2차 방어
3. 비밀번호 암호화 (BCrypt)  ← 3차 방어
4. 입력값 검증 (Validation)  ← 4차 방어
```

---

## 3. Spring Security 설정 (HTTP 환경)

### 3.1 SecurityConfig.java

```java
package com.example.assetmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 설정 (내부망 환경)
            .csrf(csrf -> csrf.disable())  // JWT 사용 시 비활성화 가능
            
            // CORS 설정 (React, Flutter 앱 접근 허용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // HTTP 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                // Public 엔드포인트
                .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                
                // 모니터링 API (SUPER 전용)
                .requestMatchers("/monitoring/**").hasRole("SUPER")
                
                // 사용자 관리 API (ADMIN 이상)
                .requestMatchers("/api/users/requests", "/api/users/{id}/approve").hasAnyRole("ADMIN", "SUPER")
                
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            
            // 세션 정책 (JWT 사용 시 Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 예외 처리
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthEntryPoint)
            )
            
            // JWT 필터 추가
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 내부 IP 허용 (React, Flutter 개발 서버)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",           // React 개발 서버
            "http://192.168.1.100:3000",       // 내부 IP (예시)
            "http://10.0.0.50:8080"            // Ubuntu 서버 IP (예시)
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 암호화 (HTTP/HTTPS 무관하게 서버 내부에서 처리)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 3.2 JWT 필터 (JwtAuthenticationFilter.java)

```java
package com.example.assetmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Authorization 헤더에서 JWT 토큰 추출
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        final String employeeNo = jwtService.extractUsername(jwt);
        
        if (employeeNo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(employeeNo);
            
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 3.3 JWT 서비스 (JwtService.java)

```java
package com.example.assetmanagement.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;  // 15분 (900000ms)

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;  // 7일 (604800000ms)

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

## 4. application.yml 설정 (HTTP 환경)

### 4.1 개발 환경 (Windows - application-dev.yml)

```yaml
server:
  port: 8080
  # HTTP로 실행 (HTTPS 설정 없음)

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/asset_management
    username: root
    password: password
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret-key: ${JWT_SECRET_KEY:YourVeryLongSecretKeyAtLeast256BitsForHS256AlgorithmSecurity}
  expiration: 900000       # 15분 (밀리초)
  refresh-expiration: 604800000  # 7일 (밀리초)

# CORS 설정
cors:
  allowed-origins:
    - http://localhost:3000
    - http://192.168.1.100:3000

logging:
  level:
    org.springframework.security: DEBUG
```

### 4.2 운영 환경 (Ubuntu - application-prod.yml)

```yaml
server:
  port: 8080
  # HTTP로 실행 (내부망)
  address: 0.0.0.0  # 모든 네트워크 인터페이스에서 수신

spring:
  datasource:
    url: jdbc:mysql://db:3306/asset_management
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

jwt:
  secret-key: ${JWT_SECRET_KEY}  # 환경 변수로 주입
  expiration: 900000
  refresh-expiration: 604800000

cors:
  allowed-origins:
    - http://192.168.1.100:3000   # React 웹
    - http://10.0.0.50:8080       # Ubuntu 서버 IP

logging:
  level:
    org.springframework.security: INFO
```

---

## 5. 내부망 환경 보안 체크리스트

### 5.1 ✅ 필수 보안 설정

- [x] **네트워크 격리**: 외부 인터넷과 분리된 내부망
- [x] **방화벽 설정**: 8080 포트는 내부 IP만 접근 가능
- [x] **강력한 JWT Secret Key**: 256비트 이상 랜덤 문자열
- [x] **비밀번호 암호화**: BCrypt (Cost Factor 10 이상)
- [x] **JWT 만료 시간**: Access Token 15분, Refresh Token 7일
- [x] **입력값 검증**: @Valid, @NotNull 등 사용
- [x] **SQL Injection 방지**: JPA/PreparedStatement 사용
- [x] **XSS 방지**: React의 기본 Escaping 활용

### 5.2 ⚠️ HTTP 환경 주의사항

| 위험 요소 | 대응 방안 |
|----------|----------|
| **토큰 평문 전송** | 내부망 물리적 격리로 스니핑 차단 |
| **Man-in-the-Middle** | 신뢰 네트워크 내에서만 운영 |
| **세션 하이재킹** | JWT 만료 시간 짧게 설정 (15분) |
| **브라우저 저장소 노출** | Refresh Token은 HttpOnly 쿠키 사용 권장 |

### 5.3 🔒 추가 보안 강화 (선택)

```java
// IP 기반 접근 제어 (선택)
@Configuration
public class IpSecurityConfig {
    
    @Bean
    public SecurityFilterChain ipFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/**").hasIpAddress("192.168.1.0/24")  // 내부망만 허용
        );
        return http.build();
    }
}
```

---

## 6. React/Flutter 클라이언트 설정

### 6.1 React (Axios 설정)

```javascript
// src/api/axiosConfig.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://192.168.1.100:8080/api',  // Ubuntu 서버 IP
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// JWT 토큰 자동 추가
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 토큰 만료 시 자동 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refresh_token');
        const { data } = await axios.post(
          'http://192.168.1.100:8080/api/auth/refresh',
          { refreshToken }
        );
        
        localStorage.setItem('access_token', data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        
        return apiClient(originalRequest);
      } catch (err) {
        // 로그아웃 처리
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 6.2 Flutter (Dio 설정)

```dart
// lib/services/api_service.dart
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  static const String baseUrl = 'http://192.168.1.100:8080/api';
  late Dio _dio;

  ApiService() {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
    ));

    // JWT 토큰 자동 추가
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('access_token');
        
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        
        return handler.next(options);
      },
      onError: (error, handler) async {
        if (error.response?.statusCode == 401) {
          // 토큰 갱신 로직
          // ...
        }
        return handler.next(error);
      },
    ));
  }

  Dio get dio => _dio;
}
```

---

## 7. Docker Compose 설정 (HTTP 환경)

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"  # HTTP 포트만 노출
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./uploads:/app/uploads
      - ./gcp-key.json:/app/secrets/gcp-key.json:ro
    depends_on:
      - db
    restart: unless-stopped
    networks:
      - internal  # 내부 네트워크만 사용

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
      - MYSQL_DATABASE=asset_management
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - internal
    # 외부 포트 노출 안 함 (보안)

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"  # HTTP만 사용
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./uploads:/var/www/uploads:ro
    depends_on:
      - app
    networks:
      - internal

volumes:
  mysql-data:

networks:
  internal:
    driver: bridge
```

---

## 8. Nginx 설정 (HTTP Reverse Proxy)

```nginx
# nginx.conf
http {
    upstream backend {
        server app:8080;
    }

    server {
        listen 80;
        server_name 192.168.1.100;  # 내부 IP

        # API 프록시
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 정적 파일 (자산 이미지)
        location /uploads/ {
            alias /var/www/uploads/;
            autoindex off;
        }

        # React 빌드 파일 (선택)
        location / {
            root /var/www/html;
            try_files $uri $uri/ /index.html;
        }
    }
}
```

---

## 9. 보안 모범 사례

### 9.1 JWT Secret Key 생성 (안전한 방법)

```bash
# Linux/Mac/WSL
openssl rand -base64 64

# PowerShell (Windows)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 9.2 환경 변수 관리

```bash
# .env 파일 (Git에 커밋하지 않기)
JWT_SECRET_KEY=YourGeneratedSecretKeyHere
DB_USERNAME=root
DB_PASSWORD=YourStrongPasswordHere
```

```yaml
# docker-compose.yml
services:
  app:
    env_file:
      - .env  # 환경 변수 파일 참조
```

### 9.3 .gitignore 설정

```gitignore
# 보안 파일
.env
*.json  # GCP 키 파일
application-prod.yml

# 업로드 파일
uploads/
```

---

## 10. FAQ

### Q1. HTTP에서 JWT를 사용해도 안전한가요?
**A**: 내부망(물리적으로 격리된 네트워크)에서는 안전합니다. 단, 외부 인터넷과 연결된 환경이라면 HTTPS 필수입니다.

### Q2. CSRF 보호를 비활성화해도 되나요?
**A**: JWT를 사용하고 쿠키를 사용하지 않는다면 비활성화해도 됩니다. 하지만 Refresh Token을 쿠키로 사용한다면 CSRF 보호를 활성화하세요.

### Q3. 내부 IP로 접속 시 CORS 오류가 발생해요.
**A**: SecurityConfig의 `corsConfigurationSource()`에 허용할 IP를 추가하세요.

```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://192.168.1.100:3000",  // 여기에 IP 추가
    "http://localhost:3000"
));
```

### Q4. 향후 HTTPS로 전환하려면?
**A**: Let's Encrypt로 SSL 인증서를 발급받고 Nginx에 HTTPS 설정을 추가하면 됩니다. 코드 변경은 거의 불필요합니다.

### Q5. 모바일 앱에서도 HTTP로 접속 가능한가요?
**A**: 가능합니다. Flutter의 경우 `android/app/src/main/AndroidManifest.xml`에 `android:usesCleartextTraffic="true"` 설정을 추가하세요.

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

---

## 11. 결론

### ✅ Spring Security는 HTTP 환경에서 완벽하게 작동합니다!

**내부망 HTTP 환경에서의 권장 구성:**
```
1. Spring Security + JWT 인증 ✅
2. BCrypt 비밀번호 암호화 ✅
3. 네트워크 격리 (방화벽) ✅
4. 강력한 JWT Secret Key ✅
5. 짧은 토큰 만료 시간 ✅
```

**보안은 HTTPS뿐만 아니라 여러 계층의 방어로 이루어집니다.**  
내부망 환경에서는 물리적 격리가 1차 방어선이며, Spring Security가 2차 방어선 역할을 훌륭히 수행합니다.

---

**문서 버전**: 1.0  
**작성일**: 2026-01-30  
**대상**: 내부망(인트라넷) HTTP 환경  
**다음 검토일**: 2026-04-30
