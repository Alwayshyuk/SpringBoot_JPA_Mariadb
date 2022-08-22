# API 서비스 만들기             

최근의 프론트 엔트 개발은 좀 더 복잡해지고 광범위하다. JSP나 Thymeleaf와 같이 서버에서 모든 데이터를 만들어서 브라우저에 전송하는            
SSR(Server Side Rendering) 방식이 아닌 CSR(Client Side Rendering) 방식이 점차 사용되고 있으며,            
하나의 단독적인 애플리케이션으로 동작하는 SPA(Single Page Application)의 형태로 변화하고 있다.          

최근의 서버는 XML이나 JSON 데이터를 전송하는 역할이 점점 커지고 있다. 이처럼 클라이언트가 원하는 데이터를 제공하는 서버를 흔히 API 서버라고 하는데           
여기에서는 JSON을 이용하는 API 서버를 어떻게 만들어야 하는지 간단한 프로젝트로 알아보겠다.           
API 서버를 구성할 때 가장 주의해야 하는 부분이 바로 보안과 인증에 대한 문제이므로, 이번 프로젝트에서는 스프링 시큐리티를 사용해서 이를 처리한다.           
인증처리는 JWT(JSON Web Token)를 사용한다.              

## API 서버를 위한 구성               

API 서버는 쉽게 말해서 순수하게 원하는 데이터만을 제공하는 서버라고 생각할 수 있다. 데이터만 제공하기 때문에 클라이언트의 기술이 어떻게              
이루어 지는지는 중요하지 않고, 서버의 데이터를 여러 형태로 사용하는 것이 가능하다.          

여기서는 간단한 Note를 작성하고 이용하는 프로젝트를 구성할 것이다. 이전 프로젝트에서 다룬 적이 있는 JSON 데이터를 위주로 처리하고         
GET/POST/PUT/DELETE 등을 사용해서 등록/수정/삭제/조회 등을 처리한다.        
이를 위해 가장 먼저 해야 하는 일은 단순히 JSON 처리를 하는 API 기능을 구성한다. 프로젝트에 entity 패키지 내에 Note 엔티티 클래스를 추가한다.             

```java
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Note extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long num;
    private String title, content;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClubMember writer;

    public void changeTitle(String title){
        this.title = title;
    }
    public void changeContent(String content){
        this.content = content;
    }
}
```

Note 클래스는 ClubMember와 @ManyToOne의 관계로 구성하고, 나중에 로그인한 사용자와 비교하기 위해 사용한다.             
Note에서 수정이 가능한 부분은 제목과 내용이므로 이에 대한 수정이 가능하도록 changeTitle(), changeContent()를 구성한다.        
Note에 대한 JPA 처리는 NoteRepository를 구성한다.                 

```java
public interface NoteRepository extends JpaRepository<Note, Long> {
    @EntityGraph(attributePaths = "writer", type=EntityGraph.EntityGraphType.LOAD)
    @Query("select n from Note n where n.num = :num")
    Optional<Note> getWithWriter(Long num);

    @EntityGraph(attributePaths = {"writer"}, type=EntityGraph.EntityGraphType.LOAD)
    @Query("select n from Note n where n.writer.email=:email")
    List<Note> getList(String email);
}
```

NoteRepository에는 작성자에 대한 처리를 @EntityGraph를 이용해서 처리한다.           

#### DTO, 서비스 계층            

Note 엔티티를 다루기 위해서 중간에 NoteDTO를 구성하고, 이를 이용하는 서비스 계층을 설계한다. 프로젝트 내에 dto 패키지를 구성하고 NoteDTO를 구성한다.             

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoteDTO {
    private Long num;
    private String title, content, writerEmail;
    private LocalDateTime regDate, modDate;
}
```

프로젝트에 service 패키지를 구성하고, NoteService와 NoteServiceImpl 클래스를 구성한다.          

```java
public interface Noteservice {
    Long register(NoteDTO noteDTO);
    NoteDTO get(Long num);
    void modify(NoteDTO noteDTO);
    void remove(Long num);
    List<NoteDTO> getAllWithWriter(String writerEmail);
    default Note dtoToEntity(NoteDTO noteDTO){
        Note note = Note.builder()
                .num(noteDTO.getNum())
                .title(noteDTO.getTitle())
                .content(noteDTO.getContent())
                .writer(ClubMember.builder().email(noteDTO.getWriterEmail()).build())
                .build();
        return note;
    }
    default NoteDTO entityToDTO(Note note){
        NoteDTO noteDTO = NoteDTO.builder()
                .num(note.getNum())
                .title(note.getTitle())
                .writerEmail(note.getWriter().getEmail())
                .regDate(note.getRegDate())
                .modDate(note.getModDate())
                .build();
        return noteDTO;
    }
}
```

NoteService 인터페이스는 CRUD 기능과 entityToDto(), dtoToEntity() 기능을 구현한다.              

```java
@Service
@Log4j2
@RequiredArgsConstructor
public class NoteServiceImpl implements Noteservice{
    private final NoteRepository noteRepository;

    @Override
    public Long register(NoteDTO noteDTO) {
        Note note = dtoToEntity(noteDTO);
        log.info("==========================");
        log.info(note);
        noteRepository.save(note);
        return note.getNum();
    }

    @Override
    public NoteDTO get(Long num) {
        Optional<Note> result = noteRepository.getWithWriter(num);
        if(result.isPresent()){
            return entityToDTO(result.get());
        }
        return null;
    }

    @Override
    public void modify(NoteDTO noteDTO) {
        Long num = noteDTO.getNum();

        Optional<Note> result = noteRepository.findById(num);

        if(result.isPresent()){
            Note note = result.get();
            note.changeTitle(noteDTO.getTitle());
            note.changeContent(noteDTO.getContent());
            noteRepository.save(note);
        }
    }

    @Override
    public void remove(Long num) {
        noteRepository.deleteById(num);
    }

    @Override
    public List<NoteDTO> getAllWithWriter(String writerEmail) {
        List<Note> noteList = noteRepository.getList(writerEmail);
        return noteList.stream().map(note -> entityToDTO(note)).collect(Collectors.toList());
    }
}
```

#### 컨트롤러 계층            
JSON으로 데이터를 처리하는 NoteController는 @RestController를 사용해서 구현한다.         
구현 시에는 화면이 없는 상태이므로 REST 방식을 테스트할 수 있는 크롬 확장 프로그램을 사용한다. controller 패키지에 NoteController를 작성한다.             

```java
@RestController
@Log4j2
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {
    private final Noteservice noteservice;
    
    @PostMapping(value = "")
    public ResponseEntity<Long> register(@RequestBody NoteDTO noteDTO){
        log.info("-------------register---------------");
        log.info(noteDTO);
        
        Long num = noteservice.register(noteDTO);
        
        return new ResponseEntity<>(num, HttpStatus.OK);
    }
}
```

NoteController에는 POST 방식으로 새로운 Note를 등록할 수 있는 기능을 작성한다.          
register()에는 @RequestBody를 이용해서 JSON 데이터를 받아서 NoteDTO로 변환할 수 있도록 처리한다.           

##### 크롬 확장 프로그램을 이용한 POST 방식 테스트             

Thymeleaf를 이용하는 화면이 없는 상황이므로 브라우저에서 사용 가능한 REST 방식의 테스트 도구를 이용한다.           
크롬 브라우저의 확장 프로그램 중에서 Yet Another REST Client를 이용했지만 PostMan이나 기타 다른 프로그램을 사용해도 무방하다.           

프로젝트를 실행한 후에 REST 테스트를 진행한다. POST 방식을 선택하고, JSON 타입의 데이터를 작성한다.            
작성할 때 주의할 점은 writerEmail은 반드시 데이터베이스에 있는 ClubMember의 메일 계정을 사용해야 한다.            
서버를 호출할 때는 반드시 전송하는 데이터의 타입을 application/json 타입으로 지정해야 한다.            

##### 특정 번호의 Note 확인하기            

NoteController에는 GET 방식으로 특정한 번호의 Note를 확인할 수 있는 기능을 추가한다.        

```java
NoteController 클래스 일부

    @GetMapping(value = "/{num}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NoteDTO> read(@PathVariable("num") Long num){
        log.info("------------read----------------");
        log.info(num);
        return new ResponseEntity<>(noteservice.get(num), HttpStatus.OK);
    }
```

read()는 @PathVariable을 사용해서 경로에 있는 Note의 num을 얻어서 해당 Note 정보를 반환한다.           
GET 방식은 브라우저에서도 확인할 수 있지만 REST 테스트 도구를 이용할 수도 있다.         
현재 존재하는 Note의 번호를 /notes/2와 같이 @PathVariable로 지정할 수 있다.             

##### 특정 회원의 모든 Note 확인하기               

NoteController에는 특정 이메일을 가진 회원이 작성한 모든 Note를 조회할 수 있는 기능을 아래와 같이 구현한다.           

```java
NoteController 클래스 일부

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NoteDTO>> getList(String email){
        log.info("--------------getList-----------------");
        log.info(email);

        return new ResponseEntity<>(noteservice.getAllWithWriter(email), HttpStatus.OK);
    }
```

getList()는 파라미터로 전달되는 이메일 주소를 통해서 해당 회원이 작성한 모든 Note에 대한 정보를 JSON으로 반환한다.          
여러 개의 Note를 추가한 상태에서 GET 방식으로 확인하면 JSON의 배열을 확인할 수 있다.          

##### Note의 삭제 테스트           

Note의 삭제는 DELETE 방식으로 처리한다. 응답에 사용하는 포맷은 단순한 문자열로 지정한다.                

```java
NoteController 클래스 일부

    @DeleteMapping(value = "/{num}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> remove(@PathVariable("num") Long num){
        log.info("--------------remove-------------------");
        log.info(num);
        
        noteservice.remove(num);
        return new ResponseEntity<>("removed", HttpStatus.OK);
    }
```

REST 테스트를 할 때는 반드시 현재 데이터베이스에 존재하는 Note의 번호를 이용해서 테스트를 진행한다.          

##### Note의 수정 테스트            

Note를 수정할 때는 등록과 달리 JSON 데이터에 num 속성을 포함해서 전송한다.           

```java
NoteController 클래스 일부

    @PutMapping(value = "/{num}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> modify(@RequestBody NoteDTO noteDTO){
        log.info("------------modify----------------");
        log.info(noteDTO);

        noteservice.modify(noteDTO);

        return new ResponseEntity<>("modified", HttpStatus.OK);
    }
```

테스트할 때는 수정이 필요한 Note의 번호와 제목, 내용을 같이 전송한다.            

## API 서버를 위한 필터           

지금까지 작성한 /notes라는 경로는 외부에서 데이터를 주고받기 위한 경로이다.              
이 경로를 외부에서 아무런 제약없이 호출하는 것은 서버에 상당한 부담을 주기 때문에 인증을 거친 사용자에 한해서 서비스를 제공한다.              

기존 웹 애플리케이션의 쿠키나 세션을 사용하는 경우 동일한 사이트에서만 동작하기 때문에 API 서버처럼 외부에서 자유롭게 데이터를 인증받을 때는         
유용하지 못하다. 외부에서 API를 호출할 때는 주로 인증 정보나 인증 키를 같이 전송해서 처리한다.                

예를 들어 앞에서 소셜 로그인을 하기 위해서 구글에서 키를 발급받아 사용했듯이 API를 이용할 때 자신의 고유한 키를 같이 전송하고            
이를 이용해서 해당 요청이 정상적인 사용자임을 알아내는 방식을 사용한다. 이러한 키를 토큰이라는 용어로 사용하기도 하는데        
여기서는 JWT(JSON Web Token)라는 것을 이용할 것이다.              

외부에서는 특정한 API를 호출할 때 반드시 인증에 사용할 토큰을 같이 전송하고, 서버에서는 이를 검증한다.                
이 과정에서 필요한 것은 특정한 URL을 호출할 때 전달된 토큰을 검사하는 필터이다. 스프링 시큐리티는 원하는 필터를 사용자가 작성하고,              
이를 설정에서 시큐리티 동작의 일부로 추가할 수 있다. 필터를 추가할 때는 기존에 있는 여러 필터 사이에 위치시킬 수 있다.            

#### OncePerRequestFilter 사용해 보기              

가장 먼저 살펴볼 필터는 org.springframework.web.filter.OncePerRequestFilter 이다. OncePerRequestFilter는 추상 클래스로 제공되는           
필터로 가장 일반적이며, 매번 동작하는 기본적인 필터라고 생각하면 된다. OncePerRequestFilter는 추상 클래스이므로 이를 사용하기 위해서는        
상속으로 구현해서 사용한다. 프로젝트 security 패키지 내에 filter 패키지를 추가하고, ApiCheckFilter라는 클래스를 추가한다.             

```java
@Log4j2
public class ApiCheckFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("ApiCheckFilter.......................");
        log.info("ApiCheckFilter.......................");
        log.info("ApiCheckFilter.......................");
        
        filterChain.doFilter(request, response);
    }
}
```
doFilterInternal()에서 마지막의 filterChain.doFilter()는 다음 필터의 단계로 넘어가는 역할을 위해서 필요하다.           
작성된 필터는 SecurityConfig를 통해서 스프링의 빈으로 설정한다.          

```java
SecurityConfig 클래스 일부

    @Bean
    public ApiCheckFilter apiCheckFilter(){
        return new ApiCheckFilter();
    }
```

SecurityConfig에 ApiCheckFilter를 적용한 후에 /sample/all과 같이 permitAll()을 적용한 URL이나            
/notes/2와 같이 기존에 GET 방식으로 접근할 수 있는 URL을 호출하면 position 15 of 15... 메시지가 출력된 이후에            
ApiCheckFilter...메시지가 출력되는 것을 볼 수 있다. 이 사실은 현재 ApiCheckFilter가 스프링 시큐리티의 여러 필터 중에 맨 마지막 필터로 동작한다는 것을 의미한다.                

##### 필터의 위치 조절과 AntPathMatcher 적용하기            

ApiCheckFilter의 동작 순서를 조절하고 싶다면 기존에 있는 특정한 필터의 이전이나 다음에 동작하도록 지정할 수 있다.               
예를 들어 여러 필터 중에서 UsernamePasswordAuthenticationFilter는 사용자의 아이디와 패스워드를 기반으로 동작하는 필터이다.           
조금 전에 작성한 ApiCheckFilter를 UsernamePasswordAuthenticationFilter 이전에 동작하도록 지정하려면 다음과 같이 작성할 수 있다.            

```java
SecurityConfig 클래스 일부

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin();
        http.csrf().disable();
        http.logout();
        http.oauth2Login().successHandler(successHandler());
        http.rememberMe().tokenValiditySeconds(60*60*24*7).userDetailsService(userDetailsService);
        
        http.addFilterBefore(apiCheckFilter(), UsernamePasswordAuthenticationFilter.class);
    }
```

아래쪽에 addFilterBefore()를 이용해서 설정을 변경하려면 필터의 동작 순서가 달라진 것을 확인할 수 있다.             

필터의 순서를 조정하기는 했지만 ApiCheckFilter는 오직 /notes/..로 시작하는 경우에만 동작하는게 바람직 할 것이다.                   
이를 처리하는 방법으로는 AntPathMatcher라는 것을 사용한다. AntPathMatcher는 엔트 패턴에 맞는지를 검사하는 유틸리티라고 보면 된다.        
ApiCheckFilter에 문자열로 된 패턴을 넣어서 패턴에 맞는 경우에는 다른 동작을 하도록 변경한다.                

```java
@Log4j2
public class ApiCheckFilter extends OncePerRequestFilter {

    private AntPathMatcher antPathMatcher;
    private String pattern;

    public ApiCheckFilter(String pattern){
        this.antPathMatcher = new AntPathMatcher();
        this.pattern = pattern;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("REQUESTURI:"+request.getRequestURI());
        log.info(antPathMatcher.match(pattern, request.getRequestURI()));
        if(antPathMatcher.match(pattern, request.getRequestURI())){
            log.info("ApiCheckFilter.......................");
            log.info("ApiCheckFilter.......................");
            log.info("ApiCheckFilter.......................");

            return;
        }
        filterChain.doFilter(request, response);
    }
}

```

변경된 ApiCheckFilter 클래스는 문자열로 패턴을 입력받는 생성자가 추가되었으므로 SecurityConfig를 아래와 같이 수정한다.                  

```java
SecurityConfig 클래스 일부

    @Bean
    public ApiCheckFilter apiCheckFilter(){
        return new ApiCheckFilter("/notes/**/*");
    }
```

위와 같이 변경된 후에 프로젝트를 실행하면 /notes/로 사작하는 경로에만 로그가 출력되는 것을 볼 수 있다.

## API를 위한 인증처리          

API를 이용한다면 일반적인 로그인의 URL이 아닌 별도의 URL로 로그인을 처리하는 것이 일반적이다.           
폼 방식의 로그인과 달리 API는 URL이 변경되면 API를 이용하는 입장에서는 호출하는 URL을 변경해야만 하기 떄문에 위험한 일이다.              

여기서는 ApiLoginFilter를 작성할 예정이다. ApiLoginFilter는 특정한 URL로 외부에서 로그인이 가능하도록 하고,          
로그인이 성공하면 클라이언트가 Authorization 헤더의 값으로 이용할 데이터를 전송할 것이다.            
/api/login 이라는 URL을 이용해서 외부의 클라이언트가 자신의 아이디와 패스워드로 로그인한다고 가정한다.          
API를 사용하기 위해서 별도의 메뉴로 승인을 받는 것이 일반적이지만 여기서는 일반 로그인과 동일한 계정으로 로그인하면 일정 기간 동안 API를 호출할 수 있도록 구성한다.              

작성하려는 코드는 ApiLoginFilter라는 이름으로 생성한다. 다만 이전의 ApiCheckFilter와 달리              
org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter라는 스프링 시큐리티에서 제공하는 필터를 이용한다.            
filter 패키지에 ApiLoginFilter 클래스를 추가한다.         

```java
@Log4j2
public class ApiLoginFilter extends AbstractAuthenticationProcessingFilter {
    public ApiLoginFilter(String defaultFilterProcessesUrl){
        super(defaultFilterProcessesUrl);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        log.info("----------ApiLoginFilter----------------");
        log.info("attemptAuthentication");

        String email = request.getParameter("email");
        String pw = "1111";

        if(email==null){
            throw new BadCredentialsException("email cannot be null");
        }
        return null;
    }
}
```

AbstractAuthenticationProcessingFilter는 이름에서 알 수 있듯이 추상 클래스로 설계되어 있고,                
attemptAuthentication()라는 추상 메서드와 문자열로 패턴을 받는 생성자가 반드시 필요하다.              
attemptAuthentication()에서는 간단히 email이라는 파라미터가 있어야만 동작이 가능했다.            
우선은 간단히 동작 여부를 확인하기 위한 용도이므로 메서드 내부의 내용은 많은 수정을 거칠 것이다.         
SecurityConfig에 ApiLoginFilter를 추가한다. AbstractAuthenticationProcessingFilter는 반드시          
AuthenticationManager가 필요하므로 authenticationManager()를 이용해서 추가해 주어야 한다.           

```java
@Configuration
@Log4j2
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ClubUserDetailsService userDetailsService;
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
//                .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
        http.csrf().disable();
        http.logout();
        http.oauth2Login().successHandler(successHandler());
        http.rememberMe().tokenValiditySeconds(60*60*24*7).userDetailsService(userDetailsService);

        http.addFilterBefore(apiCheckFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(apiLoginFilter(), UsernamePasswordAuthenticationFilter.class);
        
    }
    @Bean
    public ClubLoginSuccessHandler successHandler(){
        return new ClubLoginSuccessHandler(passwordEncoder());
    }

//    @Override 더이상 사용하지 않는다.
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.inMemoryAuthentication().withUser("user1")
//                //1111 패스워드 인코딩 결과
//                .password("$2a$10$odia6IyGmiLeWVWzsK81M.FRQW02N4xN5l2fEMTxgYAs1sPTiElCq")
//                .roles("USER");
//    }
    @Bean
    public ApiCheckFilter apiCheckFilter(){
        return new ApiCheckFilter("/notes/**/*");
    }
    @Bean
    public ApiLoginFilter apiLoginFilter() throws Exception{
        ApiLoginFilter apiLoginFilter = new ApiLoginFilter("/api/login");
        apiLoginFilter.setAuthenticationManager(authenticationManager());
        
        return apiLoginFilter;
    }
}
```

추가한 apiLoginFilter()는 /api/login 이라는 경로로 접근할 때 동작하도록 지정하고, usernamePasswordAuthenticationFilter 전에 동작하도록 한다.         
프로젝트를 실행하고 /api/login을 email 파라미터 없이 전송하면 401 에러가 발생하는 것을 볼 수 있다.            

#### Authorization 헤더 처리           

특정한 API를 호출하는 클라이언트에서는 다른 서버나 Application으로 실행되기 때문에 쿠키나 세션을 활용할 수 없다.           
이러한 제약 때문에 API를 호출하는 경우에는 Request를 전송할 때 Http 헤더 메시지에 특별한 값을 지정해서 전송한다.          

Authorization 헤더는 이러한 용도로 사용한다. 클라이언트에서 전송한 Request에 포함된 Authorization 헤더의 값을 파악해서            
사용자가 정상적인 요청인지를 알아내는 것이다. 예를 들어 ApiCheckFilter에서 Authorization 헤더를 추출하고 헤더의 값이 12345678인 경우에는           
인증을 못한다고 가정을 한다. 만일 헤더의 값이 정확하다면 다음 단계를 진행하고, 그렇지 못하다면 다른 메시지를 전송해야 한다.         
기존의 코드에 checkAuthHeader() 메서드를 추가해서 이러한 기능을 구현하면 아래와 같다.         

```java
@Log4j2
public class ApiCheckFilter extends OncePerRequestFilter {

    private AntPathMatcher antPathMatcher;
    private String pattern;

    public ApiCheckFilter(String pattern){
        this.antPathMatcher = new AntPathMatcher();
        this.pattern = pattern;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("REQUESTURI:"+request.getRequestURI());
        log.info(antPathMatcher.match(pattern, request.getRequestURI()));
        if(antPathMatcher.match(pattern, request.getRequestURI())){
            log.info("ApiCheckFilter.......................");
            log.info("ApiCheckFilter.......................");
            log.info("ApiCheckFilter.......................");

            boolean checkHeader = checkAuthHeader(request);

            if(checkHeader){
                filterChain.doFilter(request, response);
                return;
            }
            return;
        }
        filterChain.doFilter(request, response);
    }
    private boolean checkAuthHeader(HttpServletRequest request){
        boolean checkResult = false;
        String authHeader = request.getHeader("Authorization");

        if(StringUtils.hasText(authHeader)){
            log.info("Authorization exist:"+authHeader);
            if(authHeader.equals("12345678")){
                checkResult=true;
            }
        }
        return checkResult;
    }
}
```

checkAuthHeader()는 authorization이라는 헤더의 값을 확인하고 boolean 타입의 결과를 반환한다.          
이를 이용해서 doFilterInternal()에서 다음 필터로 진행할 것인지를 결정한다.         

##### 헤더 검증 실패 처리         

위의 코드는 Authorization 헤더의 값이 정상일 때도 동작하지만, Authorization 헤더가 없는 경우에도 정상이라는 메시지가 전송된다.           
이것은 ApiCheckFilter가 스프링 시큐리티가 사용하는 쿠키나 세션을 이용하지 않기 때문에 발생하는 문제이다.         
이를 해결하기 위해서는 정상적인 인증을 처리하도록 AuthenticationManager를 이용하는 방식을 사용하거나                
ApiCheckFilter에서 간단하게 JSON 포맷의 에러 메시지를 전송하는 방법을 사용할 수 있다. 여기서는 간단히 JSON 데이터를 전송한다.         

```java
ApiCheckFilter 클래스 일부
        
            if(checkHeader){
                filterChain.doFilter(request, response);
                return;
            }else{
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=utf-8");
                JSONObject json = new JSONObject();
                String message = "FAIL CHECK API TOKEN";
                json.put("code", "403");
                json.put("message", message);

                PrintWriter out = response.getWriter();
                out.print(json);
                return;
            }
```

변경된 내용은 checkHeader()가 false를 반환하는 경우에 net.minidev.json.JSONObject.JSONObject를 이용해서             
간단한 JSON 데이터와 403 에러 메시지를 만들어서 전송한다.               

Authorization 헤더에 따라서 다르게 동작하는 코드가 완성되었다면 다음과 같은 작업을 처리해야 한다.          
- 외부에서 인증할 수 있는 인증처리            
- ApiCheckFilter가 사용할 Authorization 헤더의 값을 발행하기           

#### AuthenticationManager를 이용해서 인증처리 하기           

ApiLoginFilter가 정상적으로 동작하기 위해서는 내부적으로 AuthenticationManager를 사용해서 동작하도록 수정해야 한다.           
AuthenticationManager는 authenticate() 메서드를 가지고 있는데 특이하게도 파라미터와 리턴 타입이 동일하게 Authentication 타입이다.          

파라미터로 전송하는 Authentication 타입의 객체로는 xxxToken이라는 것을 사용한다. 예를 들어 UsernamePasswordAuthenticationFilter 클래스는            
org.springframework.security.authentication.UsernamePasswordAuthenticationToken이라는 객체를 사용한다.          
직접 Authentication 타입의 객체를 만들어서 파라미터로 전달할 수도 있지만, 여기서는 최대한 간단히 사용할 수 있는          
UsernamePasswordAuthenticationToken을 사용해서 인증을 진행한다.           

```java
ApiLoginFilter 클래스 일부

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        log.info("----------ApiLoginFilter----------------");
        log.info("attemptAuthentication");

        String email = request.getParameter("email");
        String pw = request.getParameter("pw");

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, pw);

        return getAuthenticationManager().authenticate(authToken);
    }
```

변경된 내용은 email, pw를 파라미터로 받아서 실제 인증을 처리하는 것이다.         
브라우저를 이용해서 /api/login?email=xxx&pw=xxx 와 같이 실제 사용자 계정으로 로그인을 시도하면 /로 이동하지만 정상적으로 로그인이 가능하다.               

##### 인증 실패 처리           

ApiLoginFilter로 직접 인증처리를 했다면 남은 작업은 인증 후 성공 혹은 실패에 따른 처리이다.          
이러한 처리는 메서드를 override해서 처리하거나 별도의 클래스를 지정할 수 있으므로 여기서는 2가지 방식 모두를 소개한다.          

ApiLoginFilter에서 인증에 실패하는 경우 API 서버는 일반 화면이 아니라 JSON 결과가 전송되도록 수정해야 하고, 성공하는 경우에는 클라이언트가         
보관할 인증 토큰이 전송되어야 한다. AbstractAuthenticationProcessingFilter에는 setAuthenticationFailureHandler()를 이용해서         
인증에 실패했을 경우 처리를 지정할 수 있다. security 패키지의 handler 패키지 내 ApiLoginFailHandler 클래스를 추가한다.              

```java
@Log4j2
public class ApiLoginFailHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.info("login fail handler...........");
        log.info(exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");
        JSONObject json = new JSONObject();
        String message = exception.getMessage();
        json.put("code", "401");
        json.put("message", message);

        PrintWriter out = response.getWriter();
        out.print(json);
    }
}
```

ApiLoginFailHandler는 AuthenticationFailureHandler 인터페이스를 구현하는 클래스로 오직 인증에 실패하는 경우에 처리를 전담하도록 구성한다.           
인증에 실패하면 401 상태 코드를 반환하도록 한다. SecurityConfig 클래스에는 ApiLoginFilter의 setAuthenticationFailureHandler()를 적용해 주어야 한다.         

```java
SecurityConfig 클래스 일부

    @Bean
    public ApiLoginFilter apiLoginFilter() throws Exception{
        ApiLoginFilter apiLoginFilter = new ApiLoginFilter("/api/login");
        apiLoginFilter.setAuthenticationManager(authenticationManager());

        apiLoginFilter.setAuthenticationFailureHandler(new ApiLoginFailHandler());
        return apiLoginFilter;
    }
```

프로젝트를 재시작하고, 인증이 불가능한 정보를 전송하면 401 상태 코드와 함께 에러 메시지가 전송된다.          

##### 인증 성공 처리          

인증의 실패와 마찬가지로 인증의 성공 또한 별도의 클래스로 작성해서 처리할 수 있긴 하지만, AbstractAuthenticationProcessingFilter 클래스에는        
successfulAuthentication()라는 메서드를 override해서 구현이 가능하다.             

```java
ApiLoginFilter 클래스 일부

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("---------------ApiLoginFilter--------------");
        log.info("successfulAuthentication:"+authResult);

        log.info(authResult.getPrincipal());
    }
```

successfulAuthentication()의 마지막 파라미터는 성공한 사용자의 인증 정보를 가지고 있는 Authentication 객체이다.        
이를 통해서 인증에 성공한 사용자의 정보를 로그에서 확인할 수 있다. 브라우저에서 /api/login?email=user90@zerock.org&pw=1111과 같이        
정상적인 계정 정보를 이용하면 작성한 로그가 기록되는 것을 볼 수 있다.          

#### JWT 토큰 생성/검증             

인증에 성공한 후에는 사용자가 /notes/xx와 같은 API를 호출할 때 사용할 적절한 데이터를 만들어서 전송해 주어야 한다.        
여기서 JWT(JSON Web Token)를 이용할 것이다. 인증이 성공한 사용자에게는 특수한 문자열JWT을 전송해 주고, API를 호출할 때는          
이 문자열을 읽어서 해당 Request가 정상적인 요청인지를 확인하는 용도로 사용할 것이다.           

JWT 문자열은 아래와 같은 정보로 구성된다.          
- Header: 토큰 타입과 알고리즘을 의미한다. HS256 혹은 RSA를 주로 사용            
- Payload: 이름과 값의 쌍을 Claim 이라고 하고, claims를 모아둔 객체를 의미             
- Signature 헤더의 인코딩 값과 정보의 인코딩 값을 합쳐 비밀 키로 해시 함수로 처리된 결과               

JWT는 Header와 Payload를 단순히 Base64로 인코딩 한다. 이렇게 되면 누군가 디코딩하면 내용물을 알 수 있기 때문에 문제가 발생할 것이다.              
그렇기 때문에 마지막에 Signature를 이용해서 암호화된 값을 같이 사용한다. 암호화할 때 비밀 키를 모르면 올바르게 검증할 수가 없기 때문에        
이를 활용해서 검증하게 된다. JWT와 관련한 가장 확실한 정보는 https://jwt.io 사이트에서 알아볼 수 있다.             
JWT를 이용하기 위해서는 직접 코드를 구현하거나, Spring Security OAuth쪽에서 제공하는 클래스가 있기는 하지만           
가장 사용하기 쉬운 라이브러리로 io.jsonwebtoken(https://github.com/jwtk/jjwt)를 이용해서 개발한다.           

프로젝트에 적용하기 위해 build.gradle에 라이브러리를 추가한다.          

```
build.gradle

	implementation 'io.jsonwebtoken:jjwt:0.9.1'
```

JWT를 현재 프로젝트에서는 1) 인증에 성공했을 때 JWT 문자열을 만들어서 클라이언트에게 전송하는 것과 2) 클라이언트가 보낸 토큰의 값을         
검증하는 경우에 사용된다. 프로젝트 내에는 security 패키지 내에 util이라는 패키지를 구성하고 JWTUtil이라는 클래스를 추가한다.         

```java
@Log4j2
public class JWTUtil {
    private String secretKey = "zerock12345678";
    private long expire = 60*24*30;

    public String generateToken(String content) throws Exception{
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(expire).toInstant()))
                .claim("sub", content)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes("UTF-8"))
                .compact();
    }
    public String validateAndExtract(String tokenStr) throws Exception{
        String contentValue = null;

        try {
            DefaultJws defaultJws= (DefaultJws) Jwts.parser()
                    .setSigningKey(secretKey.getBytes("UTF-8"))
                    .parseClaimsJws(tokenStr);
            log.info(defaultJws);
            log.info(defaultJws.getBody().getClass());
            DefaultClaims claims = (DefaultClaims) defaultJws.getBody();
            log.info("----------------------");
            contentValue = claims.getSubject();
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            contentValue = null;
        }
        return contentValue;
    }
}
```

JWTUtil은 스프링 환경이 아닌 곳에서 사용할 수 있도록 간단한 유틸리티 클래스로 설계하였다.            
generateToken()은 JWT 토큰을 생성하는 역할을 하고, validateAndExtract()는 인코딩된 문자열에서 원하는 값을 추출하는 용도로 작성하였다.            
generateToken()의 경우는 JWT 문자열 자체를 알면 누구든 API를 사용할 수 있다는 문제가 생기므로 만료기간(expire) 값을 설정하고             
zerock12345678이라는 키를 이용해서 Signature를 생성한다. 여기서는 30일이라는 기간을 지정해 주었다.         
sub라는 이름을 가지는 Claim에는 사용자의 이메일 주소를 입력해 주어서 나중에 사용할 수 있도록 구성한다.         

validateAndExtract()는 JWT 문자열을 검증하는 역할을 한다. 예를 들어 JWT가 이미 만료 기간이 지난 것이라면 이 과정에서 Exception이 발생하게 된다.            
또한 sub 이름으로 보관된 이메일 역시 반환하게 된다. JWTUtil은 올바르게 생성되고, 검증이 가능한지 테스트를 진행하는 것이 좋다.          
test 폴더 내에 security 패키지에 JWTTests 클래스를 작성한다.          

```java
public class JWTTests {
    private JWTUtil jwtUtil;

    @BeforeEach
    public void testBefore(){
        System.out.println("testBefore...............");
        jwtUtil = new JWTUtil();
    }

    @Test
    public void testEncode() throws Exception{
        String email = "user95@zerock.org";
        String str = jwtUtil.generateToken(email);

        System.out.println(str);
    }
}
```

기존의 테스트 코드와는 달리 JWTTests 클래스는 스프링을 이용하는 테스트가 아니므로 내부에서 직접 JWTUtil 객체를 만들어서 사용할 필요가 있다.         
testEncode()를 이용해서 만들어지는 JWT 문자열을 확인할 수 있다.       
```
> Task :test
testBefore...............
eyJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE2NjExNTk1OTksImV4cCI6MTY2Mzc1MTU5OSwic3ViIjoidXNlcjk1QHplcm9jay5vcmcifQ.OUeh7Z3-w2BKveThOG2kCPLNNqGDDL3fPVvPTCWSdW4
```

화면에 출력된 문자열이 정상적인 JWT 문자열인지에 대한 검증은 https://jwt.io 사이트에서 먼저 확인한다.            
사이트 내에 JWT 문자열을 입력하기 전에 오른쪽 하단에 비밀 키로 사용된 zerock12345678을 먼저 입력한다.              
비밀 키를 먼저 입력한 후 테스트 결과로 발생하는 문자열을 입력해서 검증 결과를 확인할 수 있다.          

JWTUtil의 generateToken()에 대한 검증을 테스트 코드에서도 확인해 볼 필요가 있다.           

```java
JWTTests 클래스 일부

    @Test
    public void testValidate() throws Exception{
        String email = "user95@zerock.org";
        String str = jwtUtil.generateToken(email);
        Thread.sleep(5000);
        String resultEmail = jwtUtil.validateAndExtract(str);
        System.out.println(resultEmail);
    }
```

위의 테스트 코드를 실행했을 때 결과는 email 변수의 값과 동일한 문자열이 출력되는지 확인한다.          
```
18:20:35.773 [Test worker] INFO org.zerock.club.security.util.JWTUtil - class io.jsonwebtoken.impl.DefaultClaims
18:20:35.774 [Test worker] INFO org.zerock.club.security.util.JWTUtil - ----------------------
user95@zerock.org
```

테스트 코드에는 Thread.sleep(5000)을 이용해서 고의로 잠깐 JWT에 대한 검증을 미루는 부분이 있다.          
JWTUtil에서 코드를 변경하여 만료를 1초 뒤로 지정한다.           

```java
JWTUtil 클래스 일부

public String generateToken(String content) throws Exception{
        return Jwts.builder()
        .setIssuedAt(new Date())
        .setExpiration(Date.from(ZonedDateTime.now().plusSeconds(1).toInstant()))
        .claim("sub", content)
        .signWith(SignatureAlgorithm.HS256, secretKey.getBytes("UTF-8"))
        .compact();
        }
```

만료 시간이 1초 뒤인 경우에는 테스트 코드에서 io.jsonwebtoken.ExpiredJwtException이 발생하는지 확인한다.              
```
io.jsonwebtoken.ExpiredJwtException: JWT expired at 2022-08-22T18:24:45Z. Current time: 2022-08-22T18:24:49Z, a difference of 4529 milliseconds.  Allowed clock skew: 0 milliseconds.
	at io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:385)
	at io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:481)
	at io.jsonwebtoken.impl.DefaultJwtParser.parseClaimsJws(DefaultJwtParser.java:541)
	at org.zerock.club.security.util.JWTUtil.validateAndExtract(JWTUtil.java:31)
	at org.zerock.club.security.JWTTests.testValidate(JWTTests.java:29)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	```
```

#### Filter에 JWT 적용하기          
JWT에 대한 생성과 검증에 문제가 없다면 최종적으로 ApiLoginFilter/ApiCheckFilter에 적용해야 한다.            

##### ApiLoginFilter 수정              
ApiLoginFilter에서는 성공한 후에 JWT 문자열을 사용자에게 전송한다.           

ApiLoginFilter는 JWTUtil을 생성자를 통해서 주입받는 구조로 수정한다.             

```java
ApiLoginFilter 클래스 일부

@Log4j2
public class ApiLoginFilter extends AbstractAuthenticationProcessingFilter {

    private JWTUtil jwtUtil;
    public ApiLoginFilter(String defaultFilterProcessesUrl, JWTUtil jwtUtil){
        super(defaultFilterProcessesUrl);
        this.jwtUtil = jwtUtil;
    }
```

주입받은 JWTUtil을 이용해서 successfulAuthentication() 내에서 문자열을 발행해 준다.          

```java
ApiLoginFilter 클래스 일부

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("---------------ApiLoginFilter--------------");
        log.info("successfulAuthentication:"+authResult);

        log.info(authResult.getPrincipal());

        String email = ((ClubAuthMemberDTO)authResult.getPrincipal()).getUsername();

        String token = null;
        try{
            token = jwtUtil.generateToken(email);
            response.setContentType("text/plain");
            response.getOutputStream().write(token.getBytes());
            log.info(token);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

SecurityConfig 클래스에서는 ApiLoginFilter를 생성하는 부분에 JWTUtil을 생성자에서 사용할 수 있도록 수정해 준다.           
```java
Security Config 클래스 일부

    @Bean
    public ApiLoginFilter apiLoginFilter() throws Exception{
        ApiLoginFilter apiLoginFilter = new ApiLoginFilter("/api/login", jwtUtil());
        apiLoginFilter.setAuthenticationManager(authenticationManager());

        apiLoginFilter.setAuthenticationFailureHandler(new ApiLoginFailHandler());
        return apiLoginFilter;
    }
    
    @Bean
    public JWTUtil jwtUtil(){
        return new JWTUtil();
    }
```

프로젝트를 실행하고 브라우저에서 http://localhost:8080/api/login?email=user90@zerock.org&pw=1111을 확인하면 JWT가 발행된 것을 확인할 수 있다.       
```
eyJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE2NjExNjEwNzAsImV4cCI6MTY2MTE2MTA3MSwic3ViIjoidXNlcjkwQHplcm9jay5vcmcifQ.9yBsPVD8-M97n0BdF8kOpjA8sXgKVkwCxU1AZoz9thM
```

##### ApiCheckFilter 수정          
ApiCheckFilter는 Authorization 헤더 메시지를 통해 JWT를 확인하도록 수정해야 한다.          
ApiCheckFilter는 JWTUtil이 필요하므로 생성자를 통해서 주입하도록 수정한다.           

```java
ApiCheckFilter 클래스 일부

@Log4j2
public class ApiCheckFilter extends OncePerRequestFilter {

    private AntPathMatcher antPathMatcher;
    private String pattern;
    private JWTUtil jwtUtil;

    public ApiCheckFilter(String pattern, JWTUtil jwtUtil){
        this.antPathMatcher = new AntPathMatcher();
        this.pattern = pattern;
        this.jwtUtil = jwtUtil;
    }
```
ApiCheckFilter 내부의 checkAuthHeader()는 아래와 같이 JWTUtil의 validateAndExtract()를 호출하도록 수정한다.        

```java
ApiCheckFilter 클래스 일부

    private boolean checkAuthHeader(HttpServletRequest request){
        boolean checkResult = false;
        String authHeader = request.getHeader("Authorization");

        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")){
            log.info("Authorization exist:"+authHeader);
            try {
                String email = jwtUtil.validateAndExtract(authHeader.substring(7));
                log.info("validate result:"+email);
                checkResult = email.length()>0;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return checkResult;
    }
```

checkAuthHeader()는 내부에서 Authorization 헤더를 추출해서 검증하는 역할을 한다. Authorization 헤더 메시지의 경우 앞에는 인증 타입을 이용하는데          
일반적인 경우에는 Basic을 사용하고, JWT를 이용할 때는 Bearer를 사용한다.            
SecurityConfig에서는 ApiCheckFilter를 이용할 때 JWTUtil을 사용하도록 수정한다.               

```java
SecurityConfig 클래스 일부

    @Bean
    public ApiCheckFilter apiCheckFilter(){
        return new ApiCheckFilter("/notes/**/*", jwtUtil());
    }
```

최종적으로 테스트 도구를 이용해서 확인한다. 우선은 GET 방식으로 테스트할 수 있는 URL을 작성한다.        

Header를 작성할 때 Bearer와 같이 JWT 토큰 앞에 공백 문자를 주고 JWT 문자열을 추가한다.            

#### CORS 필터 처리           
REST 방식의 테스트는 모두 성공했지만 결정적으로 외부에서 Ajax를 이용해서 API를 사용하기 위해서는 CORS 문제를 해결해야만 한다.
CORS 처리를 위한 필터를 만들거나 설정하는 방법은 여러 가지가 있겠지만 프로젝트에서는 지금껏 작성했던 프로젝트처럼 추가하는 형태로 작성한다.         

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CORSFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Allow-Credentials","true");
        response.setHeader("Access-Control-Allow-Methods","*");
        response.setHeader("Access-Control-Max-Age","3600");
        response.setHeader("Access-Control-Allow-headers","Origin, X-Requested-With, Content-Type, Accept, Key, Authorization");
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            response.setStatus(HttpServletResponse.SC_OK);
        }else {
            filterChain.doFilter(request, response);
        }
    }
}
```

CORSFilter는 모든 필터 중에서 가장 먼저 동작하도록 @Order(Ordered.HIGHEST_PRECEDENCE)로 지정한다.       
만일 jQuery로 외부에서 /notes/xxx를 이용한다면 다음과 같이 코드를 작성하게 된다.            

```html
$(".btn").click(function(){
    $.ajax({
        beforeSend: function(request){
            request.setRequestHeader("Authorization",'Bearer '+jwtValue);
        },
        dataType:"json",
        url: 'http://localhost:8080/notes/all',
        data:{email:'user10@zerock.org'},
        success:function(arr){
            console.log(arr);
        }
    });
})
```
