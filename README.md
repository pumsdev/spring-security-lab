# Overview
![Overview image](./image/overview1.png)

Prerequisite:
- project ใน build.gradle เพิ่ม implement ของ spring security เข้าไป
```groovy
//...
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-security'
        implementation 'org.springframework.boot:spring-boot-starter-web'
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'org.springframework.security:spring-security-test'
        testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    }
//...
```


# Lab1 Spring security with user password
Initialize project จะเจอหน้า login สำหรับทุก endpoint เลย

![initial image](./image/initial.png)
- ตอน run จะมี log default password และตัว default user คือ user
- ถ้า login ถูกจะปล่อยให้เข้าไปแล้วเจอ whitelabel
- ถ้า login ผิดจะขึ้น Bad credentials!

เริ่มต้นด้วยการ ลองเปลี่ยน password เข้าไปใน in memory database
```java
@EnableWebSecurity
@Configuration
public class SecurityConfig {
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        String customPassword = "{noop}halow";
        return new InMemoryUserDetailsManager(
                User.withUsername("user")
                        .password(customPassword)
                        .roles("user")
                        .build()
        );
    }
}

```
- จำเป็นต้องใส่ {noop} ไว้หน้า password ด้วยเพราะว่าใช้ password ตรง ๆ ไม่ได้ encrypt

ลองเพิ่ม user เข้าไปอีกสัก 10 คนนึง ด้วยการปรับ code เพื่อสร้าง collection
```java
@EnableWebSecurity
@Configuration
public class SecurityConfig {
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        String customPassword = "{noop}halow";
        List<UserDetails> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            users.add(User.withUsername("user" + i)
                    .password(customPassword + i)
                    .roles("user")
                    .build());
        }

        return new InMemoryUserDetailsManager(users);
    }
}
```

ลองเปิด log trace เพื่อดูว่าจริง ๆ แล้วโดย default มันวิ่งไปที่ filter หรือ provider ตัวไหนบ้าง
```shell
logging.level.org.springframework.security=TRACE
```
![](./image/default.png)
- default filter: UsernamePasswordAuthenticationFilter
- default provider: DaoAuthenticationProvider
- - พอเป็น Dao มันก็จะวิ่งไปหา class UserDetailService โดยจาก code 
ด้านบนได้มีการ inject bean inMemoryUserDetailManager เพื่อแอบใส่ user 
เข้าไปใน inMemory แล้วก็จะเอา user ที่เข้ามาใน request มาเทียบกับของที่อยู่ใน inMemory เอง


# Lab2 custom filter
อยากลองใช้ filter chain มาเพื่อดัก request ก่อนที่จะวิ่งไปหา AuthenticationProvider

ก่อนจะเริ่มทำเรื่อง filter จะต้องทำเรื่อง SecurityConfig ก่อน ต้องมีการ Overide ค่า SecurityFilterChain
ก่อน เพื่อให้สามารถ register ตัว filter ของเราเข้าไปได้

โดยจะเริ่มจาก basic ให้มี form login เหมือนเดิม
```java
//...
public class SecurityConfig {
    //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            // authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .build();
    }
    //...
}
```
- จาก code ด้านบนจะมีการ comment anyRequest().authenticated(); ไว้อยู่เพื่อดู behavior
ว่าถ้าเราไม่เปิดจะเป็นยังไง

ผลลัพธ์ที่ได้คือ
![](./image/default-behavoir-not-config.png)
- ก็คือ โดน denied หมดเลย

แต่ถ้าเรา uncomment anyRequest().authenticated() ออกก็คือถ้า login ผ่านก็จะสามารถเข้าใช้งานได้
```java
//...
public class SecurityConfig {
    //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .build();
    }
    //...
}
```
เพื่อทดสอบการ login ให้เพิ่ม SimpleController สักตัวนึงให้สอดคล้องกับ config ด้านบน
```java

@RestController
public class SimpleController {
    private static final Logger logger = LoggerFactory.getLogger(SimpleController.class);
    @GetMapping("/")
    public String publicPage() {
        logger.debug("On public page");
        logger.debug("getPrincipal {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return "Public";
    }

    @GetMapping("/internal")
    public String internal(Authentication authentication) {
        logger.debug("Object after login");
        logger.debug("getPrincipal {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        logger.debug("getName {}", authentication.getName());
        return "Hallow internal";
    }
}
```
- การทดสอบเบื้องต้น เข้า localhost:8085/internal จะติดต้อง login
- หลังจาก login เสร็จจะเจอหน้าแสดง text "Hallow internal"
- ถ้าอยู่ที่ path / ไม่จำเป็นต้อง login แต่จะต้องเห็น text "Public"

ลองสร้าง SimpleFilter และ register เข้าไป

```java
public class SimpleFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SimpleFilter.class);
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("hola SimpleFilter");

        filterChain.doFilter(request, response);
    }
}
```
- การทำงานของ Filter คือจำเป็นต้องส่งต่อ filterChain ไปด้วย และของที่อยู่บนคำสั่ง filterChain.doFilter() จะหมายถึง
ขา request ส่วน หลัง doFilter จะเป็นขา response


```java
//...
public class SecurityConfig {
    //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .addFilterBefore(new SimpleFilter(), AuthorizationFilter.class)
                .build();
    }
}
```
- addFilterBefore เนื่องจาก อยากให้ filter ทำงานก่อนจะเริ่มการ Authentication

# Lab3 custom provider
ลองสร้าง custom provider เพื่อเพิ่ม role เข้าไปใน Authentication Object

```java
public class SimpleProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(SimpleProvider.class);
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("SimpleProvider yo!");
        var name = authentication.getName();
        if (Objects.equals(name, "user2")) {
            // add role to user2
            var user2 = User.withUsername("user2")
                    .password("SET_NEW_PASSWORD")
                    .roles("user", "devops")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(
                    user2,
                    null,
                    user2.getAuthorities()
            );
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

หลังจากสร้าง file provider แล้วก็ไป register ให้ spring security รู้จัก โดยเข้าไป add เพิ่มที่ Security Config
```java
//...
public class SecurityConfig {
    //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .addFilterBefore(new SimpleFilter(), AuthorizationFilter.class)
                .authenticationProvider(new SimpleProvider())
                .build();
    }
    //...
}
```
- code ที่เพิ่มขึ้นคือ authenticationProvider โดย register ตัว SimpleProvider เข้าไปด้วย
- การทำงานของ code จะเปลี่ยนไปเล็กน้อย คือ ทุกครั้งที่มีการ authentication จะวิ่งไปที่ SimpleProvider ก่อน
ถ้าไม่เข้า if ก็จะไปต่อที่ default provider ซึ่งก็คือ DaoAuthenticationProvider

case1: login with user1
![initial image](./image/simple-provider-default.png)
- จุดสังเกต คือมีการเรียกใช้ SimpleProvider และเรียก DaoAuthenticationProvider ต่อ
- จากนั้น ค่อย redirect ไปที่ /internal

case2: login with user2
![initial image](./image/simple-provider-reach.png)
- จุดสังเกต คือเรียกใช้ SimpleProvider แล้วเข้าไปทำงานด้านในคือ add role devops เข้าไป
จากนั้นก็ redirect กลับไปหา /internal

# Lab4 Change passwordEncoder function
ลองเปลี่ยนวิธีการ encoder password เพื่อเข้าใจวิธีการของ DelegatingPasswordEncoder
หลัก ๆ คือ `{}` จะเป็นตัวบอก function ที่ใช้ในการ encode password
ตัวอย่างอื่นดูได้จาก reference อันนี้
https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-dpe

```java
public class SecurityConfig {
    //...
    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager() {

        List<UserDetails> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Pbkdf2PasswordEncoder encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            String result = encoder.encode("myPassword");
            result = "{pbkdf2@SpringSecurity_v5_8}" + result;
            users.add(User.withUsername("user" + i)
                    .password(result)
                    .roles("user")
                    .build());
            System.out.println("user " + i + " password is " + users.get(i).getPassword());
        }

        return new InMemoryUserDetailsManager(users);
    }
    //...
}
```
- หลังจากปรับวิธีการ endcode แล้ว login ที่หน้าเว็บอีกที user: user1, password: myPassword
- ต้องเข้าได้ปกติ เพราะว่าการ endcode จะมีผลกับ ของที่เก็บใน database และ วิธีการ compare

# Lab 5 ทดสอบเรื่องการเรียงของ Provider
เพิ่ม ZimpleProvider โดย copy มาจาก SimpleProvider แล้วก็เปลี่ยนเป็น user4 แทน
```java
public class ZimpleProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(ZimpleProvider.class);
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.debug("ZimpleProvider yo!");
        var name = authentication.getName();
        if (Objects.equals(name, "user4")) {
            // add role to user4
            logger.debug("add role devops to user4");
            var user4 = User.withUsername("user4")
                    .password("SET_NEW_PASSWORD")
                    .roles("user", "devops")
                    .build();
            return UsernamePasswordAuthenticationToken.authenticated(
                    user4,
                    null,
                    user4.getAuthorities()
            );
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

```

จากนั้นที่ securityConfig เพิ่มการ register provider ให้ ZimpleProvider ขึ้นก่อน
```java
public class SecurityConfig {
   //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .addFilterBefore(new SimpleFilter(), AuthorizationFilter.class)
                .authenticationProvider(new ZimpleProvider())
                .authenticationProvider(new SimpleProvider())
                .build();
    }
    //...
}
```
ผลลัพธ์ที่ได้ คือจะมี log ของ zimpleProvider ขึ้นก่อน simpleProvider ดังนั้น
การเรียงของ provider จะขึ้นอยู่กับลำดับในการ configuration


# Lab 6 HTTPBasic
มาลองใช้ http basic แบบ default ก่อน โดยการ ลบ config formLogin ออกและเพิ่ม httpBasic เข้ามาแทน
```java
public class SecurityConfig {
   //...
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        authorizeHttp -> {
                            authorizeHttp.requestMatchers("/").permitAll();
                            authorizeHttp.anyRequest().authenticated();
                        }
                )
//                .formLogin(l -> l.defaultSuccessUrl("/internal"))
                .logout(l -> l.logoutSuccessUrl("/"))
                .addFilterBefore(new SimpleFilter(), AuthorizationFilter.class)
                .authenticationProvider(new ZimpleProvider())
                .authenticationProvider(new SimpleProvider())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
    //...
}
```
จากนั้นลอง curl ไปที่ `/internal` ผลลัพธ์ควรโดนเตะ 401 แบบด้านล่าง
```sh
➜  ~ curl -v localhost:8085/internal
*   Trying 127.0.0.1:8085...
* Connected to localhost (127.0.0.1) port 8085 (#0)
> GET /internal HTTP/1.1
> Host: localhost:8085
> User-Agent: curl/8.1.2
> Accept: */*
>
< HTTP/1.1 401
< Set-Cookie: JSESSIONID=68863F0E6C40D165431EB81B73B8132D; Path=/; HttpOnly
< WWW-Authenticate: Basic realm="Realm"
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 0
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< Content-Length: 0
< Date: Fri, 08 Nov 2024 08:26:46 GMT
<
* Connection #0 to host localhost left intact
```
แต่ถ้าลอง curl แบบระบุ user เข้าไป ควรจะผ่าน
```shell
export basic_user=$(echo -n "user1:myPassword" | base64)
curl -v -H "Authorization: Basic $basic_user" localhost:8085/internal
```
ถ้าดูผลลัพธ์ใน log การทำงานจะเห็นว่ามีเพิ่ม BasicAuthFilter เข้ามาด้วย
![basic auth filter image](./image/basic-auth.png)


----
# Authorization Server

เพิ่ม dependency oauth2
```groovy
//...
    dependencies {
        //...
        implementation 'org.springframework.boot:spring-security-oauth2-authorization-server'
        //...
    }
//...
```
ต้อง comment config ใน SecurityConfig ทั้งหมดก่อน
เพิ่ม configuration ตาม link ด้านล่าง
https://docs.spring.io/spring-authorization-server/reference/getting-started.html#defining-required-components


ปรับแก้ ให้ใช้ grant type เป็น client credentials `AuthorizationGrantType.CLIENT_CREDENTIALS`
```java
//...
public class SecurityConfig {
    //...
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oidc-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8085/login/oauth2/code/oidc-client")
                .postLogoutRedirectUri("http://127.0.0.1:8085/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();

        return new InMemoryRegisteredClientRepository(oidcClient);
    }
    //...
}

```

curl ที่ใช้ในการทดสอบ
```shell
curl --location 'http://localhost:8085/oauth2/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Authorization: b2lkYy1jbGllbnQ6c2VjcmV0' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'scope=openid'
```
