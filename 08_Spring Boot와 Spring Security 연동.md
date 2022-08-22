# Spring Boot와 Spring Security 연동        

여기서는 전통적인 ID/PW 기반의 로그인 처리를 학습한다.          
스프링 시큐리티는 다양한 방식으로 사용자 정보를 유지할 수 있는 방법을 제공하는데, 스프링 부트와 결합하면 최소환의 설정만으로도 처리할 수 있다.       
여기서 만들어지는 프로젝트는 세션 기반으로 사용자 정보는 서버에서 보관하고, 필요한 경우에 설정을 통해서 제어하도록 구성한다.         

여기서는 다음과 같은 내용을 학습한다.        
- 스프링 시큐리티에서 제공하는 로그인 처리 방식의 이해          
- JPA와 연동하는 커스텀 로그인 처리          
- Thymeleaf에서 로그인 정보 활용하기          

#### 시큐리티 설정 클래스 작성         

스프링 부트가 아닌 스프링만으로 프로젝트를 생성하는 경우에는 web.xml의 설정을 변경하고 복잡한 설정이 필요하지만,       
스프링 부트는 자동 설정 기능이 있어 별도의 설정없이도 연동처리가 완료된다.        

하지만 스프링 시큐리티를 이용하는 모든 프로젝트는 프로젝트에 맞는 설정을 추가하는 것이 일반적이므로 이를 위한 별도의 시큐리티 설정 클래스를 사용하는 것이 일반적이다.           
프로젝트 내에 config 패키지를 추가하고, SecurityConfig 클래스를 추가한다.         

SecurityConfig 클래스는 시큐리티 관련 기능을 쉽게 설정하기 위해서 WebSecurityConfigurerAdapter라는 클래스를 상속으로 처리한다.           
WebSecurityConfigurerAdapter 클래스는 주로 override를 통해서 여러 설정을 조정하게 된다.        

```java
@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {
}
```

SecurityConfig 클래스에는 아직 아무런 설정을 조정하지 않았지만,            
모든 시큐리티 관련 설정이 추가되는 부분이므로 앞으로 작성하는 프로젝트에서 핵심적인 역할을 하게 된다.          

#### 확인을 위한 SampleController          

시큐리티와 관련된 설정이 정상적으로 동작하는지를 확인하기 위해서 간단한 컨트롤러를 구성하고, 여기에 맞는 화면을 구성한다.        
프로젝트에는 controller 패키지를 구성하고, SampleController 클래스를 추가한다.      

```java
@Controller
@Log4j2
@RequestMapping("/sample/")
public class SampleController {
    @GetMapping("/all")
    public void exAll(){
        log.info("exAll.............");
    }
    @GetMapping("/member")
    public void exMember(){
        log.info("exMember...............");
    }
    @GetMapping("/admin")
    public void exAdmin(){
        log.info("exAdmin.............");
    }
}
```

SampleController에는 현재 사용자의 권한에 따라 접근할 수 있는 경로를 지정한다.        

- 로그인을 하지 않은 사용자도 접근할 수 있는 /sample/all                   
- 로그인한 사용자만이 접근할 수 있는 /sample/member                  
- 관리자 권한이 있는 사용자만이 접근할 수 있는 /sample/admin                 

templates 폴더에는 sample 폴더를 작성하고 각 경로에 맞는 페이지를 작성한다.           

```html
이렇게 all, member, admin을 작성한다.

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>For Member</title>
</head>
<body>

</body>
</html>
```

#### 스프링 시큐리티 용어와 흐름         

스프링 시큐리티로 원하는 작업을 하기 위해서는 반드시 스프링 시큐리티가 어떤 객체로 구성되어 있고,            
이들이 어떤 흐름을 가지고 동작하는지를 이해하는 것이 중요하다.            
프로젝트를 실행하고 /sample/all과 같은 경로를 호출하면 시큐리티로 인해 로그인 화면이 보이는 것을 확인할 수 있는데 서버의 로그를 중심으로 살펴보겠다.          
서버에서는 /sample/all을 호출할 경우 내부적으로 여러 개의 필터가 동작하는 것을 확인할 수 있다.            

스프링 시큐리티의 동작에는 여러 개의 객체가 서로 데이터를 주고 받으면서 이루어진다.           

핵심 역할은 Authentication Manager를 통해서 이루어진다.       
Authentication Provider는 인증 매니저가 어떻게 동작해야 하는지를 결정하고 최종적으로 실제 인증은 UserDetailsService에 의해서 이루어진다.              

스프링 시큐리티를 관통하는 가장 핵심 개념은 인증Authentication과 인가Authorization 이다.           
이 개념을 이해하는 것은 실제 오프라인에서 이루어지는 행위와 상당히 유사하다.         
쉽게 이해하기 위해 예를 들면 은행에 금고가 하나 있고, 사용자가 금고의 내용을 열어 본다고 가정해 보면 다음과 같은 과정을 거치게 된다.           

1. 사용자는 은행에 가서 자신이 어떤 사람인지 자신의 신분증으로 자신을 증명한다.         
2. 은행에서는 사용자의 신분을 확인한다.           
3. 은행에서 사용자가 금고를 열어 볼 수 있는 사람인지를 판단한다.        
4. 만일 적절한 권리나 권한이 있는 사용자의 경우 금고를 열어준다.               

위의 과정에서 1은 인증에 해당하는 작업으로 자신을 증명하는 것이다. 3에서는 사용자를 인가하는 일종의 허가를 해 주는 작업이다.         
스프링 시큐리티 역시 내부적으로 위와 유사한 과정을 거쳐서 동작한다.              

##### 필터와 필터 체이닝           

스프링 시큐리티에서 필터는 서블릿이나 JSP에서 사용하는 필터와 같은 개념이지만, 스프링 시큐리티에서는 스프링의 빈과 연동할 수 있는 구조로 설계되어 있다.           
일반적인 필터는 스프링의 빈을 사용할 수 없기 때문에 별도의 클래스를 상속받는 형태가 많다.       

스프링 시큐리티의 내부에는 여러 개의 필터가 Filter Chain이라는 구조로 Request를 처리하게 된다.         
앞에서 실행되었던 로그를 살펴보면 15개 정도의 필터가 동작하는 것을 볼 수 있다.          
개발 시에 필터를 확장하고 설정하면 스프링 시큐리티를 이용해서 다양한 형태의 로그인 처리가 가능하게 된다.        
실제 스프링 시큐리티 내부에 사용되는 주요 필터는 아래와 같다.        

- ChannelProcessingFilter           
- SecurityContextPersistenceFilter          
- ConcurrentSessionFilter           
- UsernamePasswordAuthenticationFilter           
- SecurityContextHolderAwareRequestFilter           
- RememberMeAuthenticationFilter           
- AnonymousAuthenticationFilter           
- ExceptionTranslationFilter          
- FilterSecurityInterceptor          

##### 인증을 위한 AuthenticationManager            

필터의 핵심적인 동작은 AthenticationManager를 통해서 인증Authentication이라는 타입의 객체로 작업을 하게 된다.                 
흥미롭게도 AuthenticationManager가 가진 인증 처리 메서드는 파라미터도 Authentication 타입으로 받고 리턴 타입 역시 Authentication 이다.             

인증Authentication을 쉽게 이해하려면 주민등록증과 비슷하다고 생각하면 된다.        
인증이라는 용어는 스스로 증명하다라는 의미이다. 예를 들어 로그인하는 과정에서는 사용자의 아이디/패스워드로 자신이 어떤 사람인지를 전달한다.           
전달된 아이디/패스워드로 실제 사용자에 대해서 검증하는 행위는 인증 매니저AuthenticationManager를 통해서 이루어진다.         

실제 동작에서 전달되는 파라미터는 UsernamePasswordAuthenticationToken과 같이 토큰이라는 이름으로 전달된다.                 
이 사실이 의미하는 바는 스프링 시큐리티 필터의 주요 역할이 인증 관련된 정보를 토큰이라는 객체로 만들어서 전달한다는 의미이다.             
아래 코드는 기본으로 제공되는 필터 중에서 UsernamePasswordAuthenticationFilter 클래스 코드 중 일부이다.           

```
String username = obtainUsername(request);
username=(username!=null)? username : "";
username=username.trim();
String password = obtainPassword(request);
password=(password!=null) ? password : "";
UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
//Allow subclasses to set the "details" property
setDetails(request, authRequest);
return this.getAuthenticationManager().authenticate(authRequest);
```

코드의 내용을 보면 request를 이용해서 사용자의 아이디와 패스워드를 받아서 UsernamePasswordAuthenticationToken이라는 객체를 만들고             
이를 AuthenticationManager의 authenticate()에 파라미터로 전달하는 것을 볼 수 있다.              
AuthenticationManager는 다양한 방식으로 인증처리 방법을 제공해야 한다.               
예를 들어 데이터베이스를 이용할 것인지, 메모리상에 있는 정보를 활용할 것인지와 같이 다양한 방법을 사용할 수 있다.       
AuthenticationManager는 이러한 처리를 AuthenticationProvider로 처리한다.            

AuthenticationProvider는 전달되는 토큰의 타입을 처리할 수 있는 존재인지를 확인하고, 이를 통해서 authenticate()를 수행하게 된다.       
그렇기 때문에 다양한 인증처리를 할 수 있는 객체들을 가지는 구조가 된다.            

AuthenticationProvider는 내부적으로 UserDetailsService를 이용한다.        
UserDetailsService는 실제로 인증을 위한 데이터를 가져오는 역할을 한다.               
예를 들어 JPA로 Repository를 제작했다면 UserDetailsService를 활용해서 사용자의 인증 정보를 처리한다.        

##### 인가(Authentication)와 권한/접근 제한            

인증처리 단계가 끝나면 다음으로 동작하는 부분은 사용자의 권한이 적절한가?에 대한 처리이다.           
인가Authorization는 승인의 의미이다.       
인증Authentication이 사용자가 스스로 자신을 증명하는 것이라면 인가Authorization는 허가의 의미이다.             

필터에서 호출하는 AuthenticationManager에는 authenticate()라는 메서드가 있는데 이 메서드의 리턴값은 Authentication이라는 인증 정보이다.            
이 인증 정보 내에는 Roles라는 권한에 대한 정보가 있다. 이 정보로 사용자가 원하는 작업을 할 수 있는 지 허가하게 되는데,      
이러한 행위를 접근 제한Access-Control이라고 한다.         
일반적인 경우라면 설정으로 원하는 목적지에 접근 제한을 걸고, 스프링 시큐리티에 이에 맞는 인증을 처리한다.           

지금까지 설명한 과정을 실제 결과와 같이 이어서 생각해 보겠다.             
1. 사용자는 원하는 URL을 입력한다.         
2. 스프링 시큐리티에서는 인증/인가가 필요하다고 판단하고(필터에서 판단) 사용자가 인증하도록 로그인 화면을 보여준다.                 
3. 로그인 정보가 전달된다면 AuthenticationManager가 적절한 AuthenticationProvider를 찾아서 인증을 시도한다.          

AuthenticationProvider의 실제 동작은 UserDetailsService를 구현한 객체로 처리한다.          
만일 올바른 사용자라고 인증되면 사용자의 정보를 Authentication 타입으로 전달한다.(인증)              
전달된 객체로 사용자가 적절한 권한이 있는지 확인하는 인가 과정을 거치게 된다. 이때 문제가 없다면 정상적인 화면을 볼 수 있게 된다.             

## 스프링 시큐리티 커스터마이징          

앞의 실행 결과와 같이 별도의 설정 없이도 기본적으로 스프링 시큐리티는 동작하지만 개발 시에는 적절하게 인증 방식이나 접근 제한을 지정할 수 있어야 한다.            
앞에서 만들어둔 SecurityConfig 클래스를 이용해서 약간의 override로 이러한 동작을 제어할 수 있다.          

#### 반드시 필요한 PasswordEncoder           

가장 먼저 설정이 필요한 것은 PasswordEncoder라는 객체이다.           
PasswordEnoder는 말 그대로 패스워드를 인코딩하는 것인데 주목적은 역시 패스워드를 암호화하는 것이다.             
과거 스프링 부트에서는 PasswordEncoder를 지정하지 않을 수도 있었지만, 스프링 부트 2.0부터는 인증을 위해서 반드시 PasswordEncoder를 지정해야 한다.             

PasswordEncoder는 인터페이스로 설계되어 있으므로 실제 설정에서는 이를 구현하거나 구현된 클래스를 이용해야만 한다.          
고맙게도 스프링 시큐리티에는 여러 종류의 PasswordEncoder를 제공하고 있는데 그 중에서도 가장 많이 사용하는 것은 BCryptPasswordEncoder라는 클래스이다.          
BCryptPasswordEncoder는 bcrypt라는 해시 함수를 이용해서 패스워드를 암호화하는 목적으로 설계된 클래스이다.        
BCryptPasswordEncoder로 암호화된 패스워드는 다시 원래대로 복호화가 불가능하고 매번 암호화된 값도 다르게 된다.(길이는 동일)           
대신에 특정한 문자열이 암호화된 결과인지만을 확인할 수 있기 때문에 원본 내용을 볼 수 없으므로 최근에 많이 사용되고 있다.          
SecurityConfig에는 @Bean을 이용해서 BCryptPasswordEncoder를 지정한다.           

```java
@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
```

##### BCryptPasswordEncoder 테스트           
BCryptPasswordEncoder를 이용하는 암호화는 문자열로 알아보기 어렵기 때문에 테스트 코드로 미리 어떤 값들을 사용할 수 있는지 확인해 두는 것이 좋다.           
test 폴더 내에 security 패키지를 생성하고, PasswordTests라는 테스트 클래스를 작성한다.         

```java
@SpringBootTest
public class PasswordTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testEncode(){
        String password="1111";
        String enPw = passwordEncoder.encode(password);
        System.out.println("enPw: "+enPw);
        boolean matchResult = passwordEncoder.matches(password, enPw);

        System.out.println("matchResult:"+matchResult);
    }
}
```
PasswordTests를 작성한 이유는 BCryptPAsswordEncoder의 동작을 확인하기 위한 용도이다.                 
testEncode()는 내부적으로 1111이라는 문자열을 암호화 하고, 해당 문자열을 암호화한 결과가 1111에 맞는지 확인하는 것이다.           
testEncode()를 실행하면 매번 다른 결과가 만들어지는 것을 확인할 수 있는데 이에 대한 matches()의 결과는 늘 true로 나오는 것을 확인할 수 있다.          
```
enPw: $2a$10$odia6IyGmiLeWVWzsK81M.FRQW02N4xN5l2fEMTxgYAs1sPTiElCq
matchResult:true
```
암호화된 패스워드는 잠시 후에 로그인 과정에서 사용하게 될 것이므로 저장해두자.           

#### AuthenticationManager 설정             

암호화된 패스워드를 이용하기 위해서는 해당 암호를 사용하는 사용자가 필요하다.          
이를 위해서 SecurityConfig에는 AuthenticationManager의 설정을 쉽게 처리할 수 있도록 도와주는 configure()메서드를 override해서 처리한다.         

configure()는 파라미터가 다른 여러 개의 메서드가 있는데 이 중에서 AuthenticationmanagerBuilder라는 타입을 파라미터로 사용하는 메서드를 구현한다.          
파라미터로 사용하는 AuthenticationManagerBuilder는 말 그대로 코드를 통해서 직접 인증 매니저를 설정할 때 사용한다.         

```java
@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser("user1")
                //1111 패스워드 인코딩 결과
                .password("$2a$10$odia6IyGmiLeWVWzsK81M.FRQW02N4xN5l2fEMTxgYAs1sPTiElCq")
                .roles("USER");
    }
}
```

configure() 내부에는 최소한의 코드로 로그인을 확인할 수 있는 inMemoryAuthentication()의 리턴 객체를 이용해서 한 명의 사용자를 생성한다.          
코드로 작성된 계정은 조금 뒤쪽에서는 사용할 수 없지만 단순한 테스트를 하는 정도의 용도로는 적합하다.          
withUser("user1")은 사용자의 계정이고, 패스워드는 조금 전 테스트 코드에서 생성된 결과를 사용한다.       
사용자가 가지는 권한에는 USER라는 권한을 지정한다.          

위와 같이 설정을 변경하고 프로젝트를 실행해서 결과를 확인한다.       
로그인 정보가 없는 상태에서 브라우저를 이용해 /sample/all에 접근하면 로그인 창을 볼 수 있게 된다.          
로그인 창의 사용자 아이디는 user1로 지정하고 패스워드는 1111을 입력한 뒤에 로그인을 시도하면 정상적으로 접근하는 것을 확인할 수 있다.         

#### 인가Authorization가 필요한 리소스 설정          

스프링 시큐리티를 이용해서 특정한 리소스(자원-웹의 경우에는 특정한 URL)에 접근 제한을 하는 방식은 크게 1) 설정을 통해서 패턴을 지정하거나, 2)어노테이션을           
이용해서 적용하는 방법이 있다. 어노테이션을 이용하는 방식이 더 간단하긴 하지만 우선은 Security Config 클래스로 설정해 본다.           
SecurityConfig 클래스에는 configure()메서드 중에서 HttpSecurity 타입을 파라미터로 받는 메서드를 override해서 접근 제한을 처리할 수 있다.         

```java
SecurityConfig 클래스 일부

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll();
    }
```

추가된 configure()에는 http.authorizeRequests()로 인증이 필요한 자원들을 설정할 수 있고,         
antMatchers()는 **/*와 같은 앤트 스타일의 패턴으로 원하는 자원을 선택할 수 있다.          
마지막으로 permitAll()의 경우는 말 그대로 모든 사용자에게 허락한다는 의미이므로 로그인하지 않은 사용자도 익명의 사용자로 간주되어 접근이 가능하다.         
위와 같이 설정을 변경한 후 프로젝트를 재시작하면 /sample/all에 별도의 로그인 없이도 접근이 가능한 것을 확인할 수 있다.           

##### formLogin()

HttpSecurity 객체는 대부분의 경우 연속적으로 '.'을 이용해서 처리하는 빌더 방식의 구성이 가능하다.       
이를 통해서 추가적인 자원에 대한 설정을 아래와 같이 사용할 수 있다.          
예를 들어 /sample/member라는 경로는 USER라는 권한이 있는 사용자만 사용할 수 있도록 구성한다면 다음과 같은 형태가 된다.          

```java
SecurityConfig 클래스 일부

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
                .antMatchers("/sample/member").hasRole("USER");
    }
```

위와 같이 설정을 변경하고 /sample/member를 호출하면 Access Denied 결과를 볼 수 있다.        

Access Denied는 현재 사용자가 적절한 권한이 없는 경우이기 때문에 발생하는 결과이다.         
HttpSecurity의 formLogin()이라는 기능은 이와 같이 인가/인증 절차에서 문제가 발생했을 때 로그인 페이지를 보여주도록 지정할 수 있고,          
화면으로 로그인 방식을 지원한다는 의미로 사용된다.             

```java
SecurityConfig 클래스 일부

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
        .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
    }
```

formLogin()이 적용되면 인가/인증에 실패하는 경우 로그인 페이지를 볼 수 있게 된다.            

formLogin()을 이용하는 경우 별도의 디자인을 적용하기 위해서는 추가적인 설정이 필요하다.           
loginPage()나 loginProcessUrl(), defaultSuccessUrl(), failureUrl() 등을 이용해서 필요한 설정을 지정할 수 있다.            
대부분의 애플리케이션은 고유한 디자인을 적용하기 때문에 loginPage()를 사용해서 별도의 로그인 페이지를 이용하는 경우가 많다.            

##### ROLE_USER라는 권한           

위의 설정에서 /sample/member는 USER라는 권한이 있도록 지정한 부분이 있다.          
이때 USER라는 단어는 ROLE_USER라는 상수와 같은 의미이다. 이 단어 자체가 특별한 의미를 가지는 것은 아니고,            
스프링 시큐리티의 내부에서 USER라는 단어를 상수처럼 인증된 사용자르르 의미하는 용도로 사용하기 때문에 미리 알고 있어야 한다.         
예를 들어 곧 살펴볼 소셜 로그인의 경우 역시 로그인에 성공하면 사용자는 ROLE_USER 권한을 가지도록 지정된다.           

#### CSRF 설정           
스프링 시큐리티는 기본적으로 CSRF(Cross Site Request Forgery - 크로스 사이트 요청 위조)라는 공격을 방어하기 위해서          
임의의 값을 만들어서 이를 GET방식을 제외한 모든 요청 방식(POST,PUT,DELETE) 등에 포함시켜야만 정상적인 동작이 가능하다.          

CSRF 공격은 사이트간 요청 위조라고 변역할 수 있다. 서버에서 받아들이는 정보가 특별히 사전 조건을 검증하지 않는다는 단점을 이용하는 공격 방식이다.         
CSRF를 이용해서 단순히 게시물의 조회수를 늘리는 등의 조작부터 피해자의 계정을 이용하는 다양한 공격이 가능하다.        

현재 프로젝트에서 자동으로 만들어지는 로그인 페이지는 페이지 소스 보기 기능으로 내용을 살펴보면 hidden값으로 만들어진 CSRF 토큰값이 있다.          
CSRF 토큰은 기본적으로 세션당 하나씩 생성되기 때문에 아래 그림과 같이 세션이 다른 사용자는 다른 값으로 생성된다.                  
일반적으로 세션을 이용하고, < form > 태그를 이용하는 방식에서는 CSRF 토큰이 보안상으로 권장되지만, REST 방식 등에서 매번 CSRF 토큰의 값을          
알아내야 하는 불편함이 있기때문에 경우에 따라서는 CSRF 토큰의 발행을 하지 않는 경우도 있다.         

##### csrf 토큰 비활성화         

HttpSecurity의 csrf() 메서드를 이용해서 CSRF 토큰을 발행하지 않도록 설정하기 위해서는 아래와 같이 간단한 설정을 추가할 수 있다.         

```java
SecurityConfig 클래스 일부

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
        .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
        http.csrf().disable();
    }
```

위와 같이 disable()을 지정한 후에 만들어지는 로그인 페이지의 코드는 디자인의 차이는 없지만 CSRF 토큰이 필요하지 않은 상태로 만들어지는 것을 볼 수 있다.          
여기서는 외부에서 REST 방식으로 이용할 수 있는 보안 설정을 다루기 위해 CSRF 토큰을 발행하지 않는 방식으로 설정하고 진행한다.           

#### logout 설정          

formLogin()과 마찬가지로 logout() 메서드를 이용하면 로그아웃 처리가 가능하다.       
formLogout() 역시 로그인과 마찬가지로 별도의 설정이 없는 경우에는 스프링 시큐리티가 제공하는 웹 페이지를 보게 된다.       
SecurityConfig에 logout()을 적용해주기만 하면 된다.         

```java
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
                .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
        http.csrf().disable();
        http.logout();
    }
```

logout()에서 주의해야 할 점은 CSRF 토큰을 사용할 때는 반드시 POST방식으로만 로그아웃을 처리한다는 점이다.         
CSRF 토큰을 이용하는 경우에는 /logout 이라는 URL을 호출했을 때 < form > 태그와 버튼으로 구성된 화면을 보게 된다.           
반면에 CSRF 토큰을 disable()로 비활성화 시키면 GET방식/logout 으로도 로그아웃이 처리된다.       
로그아웃도 formLogin()과 마찬가지로 사용자가 별도의 로그아웃관련 설정을 추가할 수 있다. logoutUrl(), logoutSuccessUrl() 등을 지정할 수 있다.                 
스프링 시큐리티는 기본적으로 HttpSession을 이용하는데 invalidatedHttpSession()과 deleteCookies()를 이용해서 쿠키나 세션을 무효화 시킬 수 있도록 설정할 수 있다.             

## 프로젝트를 위한 JPA 처리              

프로젝트에 스프링 시큐리티를 적용하기 위해서는 당연히 이에 맞는 데이터베이스 관련 처리가 필요하다.              
여기서는 최근에 아이디 대신 소셜 로그인에서 많이 사용하는 이메일을 아이디로 구성해서 회원을 처리한다.            
회원 정보는 다음과 같이 구성한다.         

- 이메일(아이디 역할)           
- 패스워드               
- 이름(닉네임)             
- 소셜 가입 여부(소셜 로그인으로 회원 가입된 경우)                   
- 기타(등록일/수정일)            

일반적인 회원 관리와 가장 다른 점은 권한에 대한 처리이다. 여기서는 권한을 다음과 같이 구분해서 사용한다.                
- USER: 일반 회원           
- MANAGER: 중간 관리 회원               
- ADMIN: 총괄 관리자               

사실 회원의 권한은 한 가지만을 가지는 것이 정상적인 설계지만, 한명의 클럽 회원이 여러 개의 권한을 가진다는 전제로 프로젝트를 구성한다.            
ClubMember와 ClubMemberRole의 관계는 1:N의 관계이지만, 사실상 ClubMemberRole 자체가 핵심적인 역할을 하지는 못하기 때문에            
별도의 엔티티 보다는 @ElementCollection이라는 것을 이용해서 별도의 PK 생성 없이 구성한다.          
프로젝트 내에는 entity 패키지를 추가하고, 이전 프로젝트와 동일하게 BaseEntity 클래스와 ClubMember라는 클래스를 생성한다.             

```java
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ClubMember extends BaseEntity{
    @Id
    private String email;

    private String password, name;

    private boolean fromSocial;
    
}
```

ClubMember는 여러 개의 권한을 가질 수 있어야 한다.         
다만 이 권한은 ClubMember객체의 일부로만 사용되기 때문에 JPA의 @ElementCollection이라는 것을 이용한다.           
BaseEntity를 이용하기 위해서 프로젝트 생성 시에 만들어진 ClubApplication 클래스에는 @EnableJpaAuditing 어노테이션을 추가한다.               

```java
@SpringBootApplication
@EnableJpaAuditing
public class ClubApplication {
	public static void main(String[] args) {
		SpringApplication.run(ClubApplication.class, args);
	}
}
```

entity 패키지 내에 ClubMemberRole 이라는 enum 타입을 생성하고 정해진 권한들을 지정한다.        
```java
public enum ClubMemberRole {
    USER, MANAGER, ADMIN;
}
```

ClubMember는 ClubMemberRole 타입값을 처리하기 위해서 Set< ClubMemberRole > 타입을 추가하고, Fetch는 LAZY 타입으로 지정한다.          

```java
ClubMember 클래스 일부

    @ElementCollection(fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ClubMemberRole> roleSet = new HashSet<>();
    
    public void addMemberRole(ClubMemberRole clubMemberRole){
        roleSet.add(clubMemberRole);
    }
```

프로젝트가 실행되면서 2개의 테이블이 생성되는 것을 확인할 수 있다.              

#### Repository와 더미데이터 추가하기            
ClubMember의 데이터 중에서 패스워드는 반드시 암호화해서 데이터를 추가해야 하기 때문에 테스트 코드를 작성해서 100개의 계정을 생성한다.          
프로젝트 내에 repository 패키지를 추가하고, ClubMemberRepository를 생성한다.         

```java
public interface ClubMemberRepository extends JpaRepository<ClubMember, String> {
}
```

test 폴더에는 데이터베이스에 여러 개의 데이터를 생성하는 테스트 코드를 작성한다. ClubMemberTests라는 클래스를 생성해서 진행한다.                
ClubMemberTests에는 우선 여러 명의 회원을 추가한다. 이때 한 명의 회원이 여러 개의 권한을 가질 수 있도록 데이터를 추가한다.          

```java
@SpringBootTest
public class ClubMemberTests {

    @Autowired
    private ClubMemberRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void insertDummies(){
        IntStream.rangeClosed(1,100).forEach(i->{
            ClubMember clubMember = ClubMember.builder()
                    .email("user"+i+"@zerock.org")
                    .name("사용자"+i)
                    .fromSocial(false)
                    .password(passwordEncoder.encode("1111"))
                    .build();

            clubMember.addMemberRole(ClubMemberRole.USER);

            if(i>80){
                clubMember.addMemberRole(ClubMemberRole.MANAGER);
            }
            if(i>90){
                clubMember.addMemberRole(ClubMemberRole.ADMIN);
            }
            repository.save(clubMember);
        });
    }
}
```

ClubMemberTests의 insertDummies()는 100명의 회원을 추가하면서 80명은 USER라는 권한만을, 20명은 USER/MANAGER,                 
10명은 USER/MANAGER/ADMIN 권한을 가지도록 설계한다.       
테스트 코드를 실행하면 데이터베이스의 club_member 테이블에는 회원 정보가 기록되고 club_member_role_set 테이블에는 권한이 기록된다.          

#### 회원 데이터 조회 테스트           
ClubMember의 조회 시에는 이메일을 기준으로 조회하고,              
일반 로그인 사용자와 뒤에 추가되는 소셜 로그인 사용자를 구분하기 위해서 ClubMemberRepository에 별도의 메서드로 처리한다.              

```java
public interface ClubMemberRepository extends JpaRepository<ClubMember, String> {

    @EntityGraph(attributePaths = {"roleSet"}, type=EntityGraph.EntityGraphType.LOAD)
    @Query("select m from ClubMember m where m.fromSocial=:social and m.email = :email")
    Optional<ClubMember> findByEmail(@Param("email") String email, @Param("social") boolean social);
}
```

findByEmail()은 사용자의 이메일과 소셜로 추가된 회원 여부를 선택해서 동작하도록 설계한다.          
@EntityGraph를 이용해서 left outer join으로 ClubMemberRole이 처리될 수 있도록 한다. 테스트 코드는 아래와 같이 작성한다.              

```java
ClubMemberTests 클래스 일부

    @Test
    public void testRead(){
        Optional<ClubMember> result = repository.findByEmail("user95@zerock.org", false);
        ClubMember clubMember = result.get();
        System.out.println(clubMember);
    }
```

testRead()의 결과는 아래와 같이 left outer join으로 처리되는 것을 확인할 수 있고, 권한도 역시 같이 로딩하는 것을 확인할 수 있다.        

```
Hibernate: 
    select
        clubmember0_.email as email1_0_,
        clubmember0_.moddate as moddate2_0_,
        clubmember0_.regdate as regdate3_0_,
        clubmember0_.from_social as from_soc4_0_,
        clubmember0_.name as name5_0_,
        clubmember0_.password as password6_0_,
        roleset1_.club_member_email as club_mem1_1_0__,
        roleset1_.role_set as role_set2_1_0__ 
    from
        club_member clubmember0_ 
    left outer join
        club_member_role_set roleset1_ 
            on clubmember0_.email=roleset1_.club_member_email 
    where
        clubmember0_.from_social=? 
        and clubmember0_.email=?
ClubMember(email=user95@zerock.org, password=$2a$10$b9JPkIKaSFAy4yyFyvrL1uEd3q5RXf0EHDgoIUVpo87PgBhwHUaPe, name=사용자95, fromSocial=false, roleSet=[USER, MANAGER, ADMIN])
```

## 시큐리티를 위한 UserDetailsService          

JPA로 회원 정보를 처리하는데 문제가 없다는 것을 확인했다면 다음으로 진행할 작업은 스프링 시큐리티가 ClubMemberRepository를 이용해서               
회원을 처리하는 부분을 제작해야 한다. 로그인 처리를 개발해 본 적이 있다면 일반적으로는 회원 아이디와 패스워드로 데이터베이스를 조회하고,          
올바른 데이터가 있다면 세션이나 쿠키로 처리하는 형태의 제작을 하게 된다.         
반면에 스프링 시큐리티는 기존과는 좀 다른 방식으로 동작한다. 몇 가지 차이점을 정리하면 다음과 같다.          

- 스프링 시큐리티에서는 회원이나 계정에 대해서 User라는 용어를 사용한다. User라는 단어를 사용할 때는 상당히 주의해야 한다. 이러한 이유 때문에 앞의 예제에서도 ClubMember와 같이 다른 이름을 사용하고 있다.                  
- 회원 아이디라는 용어 대신에 username이라는 단어를 사용한다. 스프링 시큐리티에서는 username이라는 단어 자체가 회원을 구별할 수 있는 식별 데이터를 의미한다. 문자열로 처리하는 점은 같지만 일반적으로 사용하는 회원의 이름이 아니라 오히려 id에 해당한다.                
- username과 password를 동시에 사용하지 않는다. 스프링 시큐리티는 UserDetailsService를 이용해서 회원의 존재만을 우선적으로 가져오고, 이후에 password가 틀리면 Bad Cridential이라는 결과를 만들어낸다.                     
- 사용자의 username과 password로 인증 과정이 끝나면 원하는 자원에 접근할 수 있는 적절한 권한이 있는지를 확인하고 인가 과정을 실행한다. 이 과정에서는 Access Denied와 같은 결과가 만들어 진다.                 

위와 같은 차이점을 처리하는 가장 핵심적인 부품은 UserDetailsService이다.         
UserDetailsService는 loadUserByUsername()이라는 단 하나의 메서드를 가지고 있다.            

#### UserDetails 인터페이스           
loadUserByUsername()은 말 그대로 username이라는 회원 아이디와 같은 식별 값으로 회원 정보를 가져온다.              
메서드의 리턴 타입은 UserDetails라는 타입인데 이를 통해서 다음과 같은 정보를 알아낼 수 있도록 구성되어 있다.         
- getAuthorities() 사용자가 가지는 권한에 대한 정보          
- getPassword() 인증을 마무리하기 위한 패스워드 정보               
- getUsername() 인증에 필요한 아이디와 같은 정보            
- 계정 만료 여부 : 더이상 사용이 불가능한 계정인지 알 수 있는 정보               
- 계정 잠김 여부 : 현재 계정의 잠김 여부             

이를 처리하기 위해서 ClubMember를 처리할 수 있는 방법은 크게 두 가지 방식을 고민할 수 있다.           
- 기존 DTO 클래스에 UserDetails 인터페이스를 구현하는 방법              
- DTO와 같은 개념으로 별도의 클래스를 구성하고 이를 활용하는 방법             

위의 방식 중에서 개인적으로 선호하는 방식은 별도의 클래스를 하나 구성하고 이를 DTO처럼 사용하는 방식을 선호한다.             
UserDetails 인터페이스를 별도의 클래스로 구성하는 것은 어려운 일이 아니지만, 인터페이스를 구현한 별도의 클래스가 있기 때문에 이를 사용하는 것이 더 수월하다.             

프로젝트 내에 security 패키지를 구성하고 내부에 dto 패키지와 service 패키지를 생성한다.          
dto 패키지에는 ClubAuthMemberDTO라는 클래스를 아래와 같이 작성한다.            

```java
@Log4j2
public class ClubAuthMemberDTO extends User {
    public ClubAuthMemberDTO(String username, String password, Collection<? extends GrantedAuthority> authorities){
        super(username, password, authorities);
    }
}
```

ClubAuthMemberDTO를 구성하는 첫 번째 단계는 User 클래스를 상속하고 부모 클래스인 User 클래스의 생성자를 호출할 수 잇는 코드를 만드는 것이다.            
부모 클래스인 User 클래스에 사용자 정의 생성자가 있으므로 반드시 호출할 필요가 있다.            
기존의 코드에서 엔티티 클래스와 DTO 클래스를 별도로 구성했듯이 ClubAuthMemberDTO가 바로 이러한 역할을 하는 클래스이다.            
DTO와 유사하게 ClubAuthMemberDTO를 구성하면 아래와 같은 코드가 된다.          
```java
@Log4j2
@Getter
@Setter
@ToString
public class ClubAuthMemberDTO extends User {
    private String email,name;
    private boolean fromSocial;

    public ClubAuthMemberDTO(String username, String password, boolean fromSocial, Collection<? extends GrantedAuthority> authorities){
        super(username, password, authorities);
        this.email = username;
        this.fromSocial = fromSocial;
    }
}
```

ClubAuthMemberDTO는 DTO 역할을 수행하는 클래스인 동시에 스프링 시큐리티에서 인가/인증 작업에 사용할 수 있다.        
password는 부모 클래스를 사용하므로 별도의 멤버 변수로 선언하지 않았다.          

#### UserDetailsService 구현         
ClubMember가 ClubAuthMemberDTO라는 타입으로 처리된 가장 큰 이유는 사용자의 정보를 가져오는 핵심적인 역할을 하는 UserDetailsService라는 인터페이스 때문이다.          
스프링 시큐리티의 구조에서 인증을 담당하는 AuthenticationManager는 내부적으로 UserDetailsService를 호출해서 사용자의 정보를 가져온다.           
현재 프로젝트와 같이 JPA로 사용자의 정보를 가져오고 싶다면 이 부분을 UserDetailService가 이용하는 구조로 작성할 필요가 있다.         
추가된 service 패키지에는 이를 위한 ClubUserDetailsService 클래스를 다음과 같이 추가한다.             

```java
@Log4j2
@Service
public class ClubUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("ClubUserDetailsService loadUserByUsername"+username);
        return null;
    }
}
```

주목해서 봐야하는 점은 @Service를 사용해서 자동으로 스프링에서 빈으로 처리될 수 있게 되어 있다는 점과          
loadUserByUsername()에서는 별도의 처리없이 로그를 기록하고 있다는 점이다.            

##### SecurityConfig 클래스변경             

ClubUserDetailsService가 빈Bean으로 등록되면 이를 자동으로 스프링 시큐리티에서 UserDetailsService로 인식하기 때문에             
기존에 임시로 코드로 직접 설정한 configure(AuthenticationManagerBuilder auth) 부분을 사용하지 않도록 수정한다.            
아니면 메서드에서 ClubUserDetailsService를 주입받아서 구성하는 코드를 작성할 수 있다.           

```java
@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests().antMatchers("/sample/all").permitAll()
                .antMatchers("/sample/member").hasRole("USER");
        http.formLogin();
        http.csrf().disable();
        http.logout();
    }
    
//    @Override 더이상 사용하지 않는다.
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.inMemoryAuthentication().withUser("user1")
//                //1111 패스워드 인코딩 결과
//                .password("$2a$10$odia6IyGmiLeWVWzsK81M.FRQW02N4xN5l2fEMTxgYAs1sPTiElCq")
//                .roles("USER");
//    }
}
```

프로젝트를 시작한 후에 브라우저를 통해서 /sample/member로 이동하려고 시도하면 로그인 창이 나타나게 되고, 데이터베이스에 있는 실제 계정으로 로그인을 시도할 수 있다.            
아직까지는 ClubMemberRepository나 ClubAuthMemberDTO를 이용하는 처리가 이루어지지 않은 상태이기 때문에 에러가 발생하는 것을 확인할 수 있다.                 
에러가 발생하기는 하지만 서버에서는 정상적으로 ClubUserDetailsService가 동작하는 것을 확인할 수 있다.              

##### ClubMemberRepository 연동           

정상적인 처리를 위해서 ClubUserDetailsService와 ClubMemberRepository를 연동하는 것은 아래와 같이 처리할 수 있다.             

```java
ClubUserDetailsService 클래스 수정

@Log4j2
@Service
@RequiredArgsConstructor
public class ClubUserDetailsService implements UserDetailsService {

    private final ClubMemberRepository clubMemberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("ClubUserDetailsService loadUserByUsername"+username);

        Optional<ClubMember> result = clubMemberRepository.findByEmail(username, false);

        if(result.isEmpty()){
            throw new UsernameNotFoundException("Check Email or Social");
        }

        ClubMember clubMember = result.get();

        log.info("--------------------------");
        log.info(clubMember);

        ClubAuthMemberDTO clubAuthMember = new ClubAuthMemberDTO(
                clubMember.getEmail(),
                clubMember.getPassword(),
                clubMember.isFromSocial(),
                clubMember.getRoleSet().stream().map(role->new SimpleGrantedAuthority("ROLE_"+role.name()))
                        .collect(Collectors.toSet()));

        clubAuthMember.setName(clubMember.getName());
        clubAuthMember.setFromSocial(clubMember.isFromSocial());

        return clubAuthMember;
    }
}
```

코드의 변경 사항은 다음과 같다.           
- ClubMemberRepository를 주입받을 수 있는 구조로 변경하고 @RequiredArgsConstructor 처리            
- username이 실제로는 ClubMember에서는 email을 의미하므로 이를 사용해서 ClubMemberRepository의 findByEmail()을 호출(소셜 여부는 false)             
- 사용자가 존재하지 않으면 UsernameNotFoundException으로 처리           
- ClubMember를 UserDetails 타입으로 처리하기 위해서 ClubAuthMemberDTO 타입으로 변환          
- ClubMemberRole은 스프링 시큐리티에서 사용하는 SimpleGrantedAuthority로 변환, 이때 ROLE_이라는 접두어를 추가해서 사용            

변경후에 다시 /sample/member에 접근하면 동일하게 로그인 페이지로 이동하는데 user95@zerock.org와 같은 계정으로 로그인을 시도하면          
정상적으로 원하는 URL로 이동하는 것을 볼 수 있다.              

## Thymeleaf/Controller에서 사용자 정보 출력하기            

로그인이 정상적으로 처리되었다면 사용자의 정보를 화면이나 컨트롤러에서 출력하는 작업을 시도한다.           
스프링 시큐리티를 사용하는 경우에는 Authentication이라는 타입을 이용해서 사용자의 정보를 추출할 수 있다.         
예를 들어 현재 프로젝트의 구조에서는 Authentication이라는 존재는 실제로 ClubAuthMemberDTO 객체가 된다.            

#### Thymeleaf에서의 출력           

sample 폴더 내에 member.html은 다음과 같이 수정한다.          

```html
<h1>For Member..........</h1>

<div sec:authorize="hasRole('USER')">Has USER ROLE</div>
<div sec:authorize="hasRole('MANAGER')">Has MANAGER ROLE</div>
<div sec:authorize="hasRole('ADMIN')">Has ADMIN ROLE</div>

<div sec:authorize="isAuthenticated()">
    Only Authenticated user can see this Text
</div>

Authenticated username:
<div sec:authentication="principal.username"></div>
Authenticated user roles:
<div sec:authentication="principal.authorities">

</div>
```

코드의 상단에는 접두어인 sec 라는 단어를 이용해서 시큐리티와 관련된 부분을 처리하는데 사용한다.         
sec:authorize를 이용하면 인가authorization와 관련된 정보를 알아내거나 제어가 가능하다.          
Authentication의 principal이라는 변수를 사용하면 ClubAuthMemberDTO의 내용을 이용할 수있다.          

#### 컨트롤러에서의 출력           
컨트롤러에서 로그인된 사용자 정보를 확인하는 방법은 크게 2가지 방식이 있다. 1) SecurityContextHolder라는 객체를 이용하는 방법과 2) 직접            
파라미터와 어노테이션을 사용하는 방식이 있다. 여기서는 @AuthenticationPrincipal 어노테이션을 사용해서 처리한다.       
기존의 SampleContoller 부분을 아래 코드로 수정해서 확인한다.       

```java
SampleController 클래스 일부

    @GetMapping("/member")
    public void exMember(@AuthenticationPrincipal ClubAuthMemberDTO clubAuthMember){
        log.info("exMember...............");
        log.info("-----------------------");
        log.info(clubAuthMember);
    }
```

스프링 시큐리티의 org.springframework.security.core.annotation.Authentication.Principal 타입은 getPrincipal() 메서드를         
통해서 Object 타입의 반환 타입이 있다. 위 코드에서 @AuthenticationPrincipal은 별도의 캐스팅 작업 없이 직접 실제 ClubAuthMEmberDTO 타입을             
사용할 수 있기 때문에 좀 더 편하게 사용할 수 있다.        
