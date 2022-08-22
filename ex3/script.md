# 스프링 MVC와 Thymeleaf     
스프링 부트는 설정을 통해서 JSP 등을 사용할 수도 있지만 기본적으로 JSP 대신에      
Thymeleaf나 FreeMarker, Mustache 등을 이용해서 화면을 처리한다.      

여러 기술 중 Thymeleaf를 선택하는 이유는 다음과 같다.     
> JSP와 유사하게 ${}을 변도의 처리 없이 이용할 수 있다.    
> Model에 담긴 객체를 화면에서 JavaScript로 처리하기 편리하다.      
> 연산이나 포맷과 관련된 기능을 추가적인 개발 없이 지원한다.     
> 개발 도구를 이용할 때, html 파일로 생성하는데 문제가 없고 별도의 확장자를 이용하지 않는다.     

Thymeleaf를 이용하는 프로젝트는 변경 후에 만들어진 결과를 보관(캐싱)하지 않도록 설정해 두는 것이 편리하다.     
이를 위해 application.properties 파일에 다음 항목을 추가한다.     

```java
spring.thymeleaf.cache=false
```

Thymeleaf는 JSP처럼 서버에서 결과를 만들어 브라우저로 전송한다.     
위 설정은 이미 만들어진 결과를 서버에서 계속 보관할 것인지에 대한 설정이며,     
즉, Thymeleaf 파일을 수정하고 저장한 후 브라우저에서 변경된 결과를 확인하기 위한 설정이다.     

```java
@Controller
@RequestMapping("/sample")
@Log4j2
public class SampleController {
    @GetMapping("/ex1")
    public void ex1(){
        log.info("ex1...........");
    }
}
```
SampleController에는 동작을 확인하기 위해 @Log4j2를 적용했다.     
@Log4j2는 Lombok의 기능으로 스프링 부트가 로그 라이브러리 중에 Log4j2를 기본으로 사용하고 있기 때문에 별도의 설정없이 적용이 가능하다.     
(다만, Intellij의 경우 테스트 코드에 반영하기 위해서는 추가적인 조정이 필요하다.)     

Thymeleaf는 기본적으로 프로젝트 생성 시에 추가되는 templates 폴더를 기본으로 사용하므로 templates 폴더 내에     
sample 폴더를 생성하고 ex1.html 파일을 추가한다.     

```html
ex2.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
    <h1 th:text="${'Hello world'}"></h1>
</body>
</html>
```

Thymeleaf의 기본적인 사용 방법은 기존의 속성 앞에 'th:'를 붙여주고 속성값을 지정하는 것이다.     
JSP와 달리 별도의 태그를 이용하지 않고 HTML을 그대로 유지한 상태에서 필요한 내용을 추가해서 개발할 수 있다.     
(JSP는 별도의 <% %>등을 이용해서 블록을 설정하는 등 기존의 HTML 구조를 훼손하는 경우가 많았다면      
Thymeleaf는 HTML은 그대로 두고 필요한 동작이나 값을 추가하는 방식이다.)      

## Thymeleaf의 기본 사용법      
Thymeleaf는 JSP를 사용해 본 적이 있다면 별다른 어려움없이 적응이 가능하다는 장점이 있다.     

```java
@Data
@Builder(toBuilder = true)
public class SampleDTO {
    private Long sno;
    private String first;
    private String last;
    private LocalDateTime regTime;
}
```
Lombok의 @Data는 Getter/Setter, toString(), equals(), hashCode()를 자동으로 생성한다.     
SampleController에서는 작성된 SampleDTO의 객체를 Model에 추가해서 전달한다.     

```java
    @GetMapping({"/ex2"})
    public void exModel(Model model){
        List<SampleDTO> list = IntStream.rangeClosed(1, 20).asLongStream()
                .mapToObj(i->{
                    SampleDTO dto = SampleDTO.builder().sno(i)
                                                    .first("First..."+i)
                                                    .last("Last..."+i)
                                                    .regTime(LocalDateTime.now())
                                                    .build();
                    return dto;
                }).collect(Collectors.toList());
            model.addAttribute("list", list);
    }
```
SampleDTO 타입의 객체를 20개 추가하고 이를 Model에 담아서 전송한다.      
(@GetMapping의 value 속성값을 '{}'로 처리하면 하나 이상의 URL을 지정할 수 있다.)      

#### 반복문 처리

Thymeleaf에서 반복은 th:each라는 속성을 이용한다.
```java
th:each = "변수: ${목록}"
```
```html
ex2.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
<meta charset="UTF-8">
<title>Title</title>
</head>
<body>
<ul>
<li th:each="dto : ${list}">
        [[${dto}]]
</li>
</ul>
</body>
</html>
```

<li> 태그 내에는 'dto'라는 변수를 만들어서 사용하고 Model로 전달된 데이터는 ${list}를 이용해서 처리하고 있다.      
<li> 태그의 안쪽에 사용된 '[[]]'는 인라인 표현식으로 별도의 태그 속성을 지정하지 않고 사용하고자 할 때 유용하게 사용할 수 있다.     

###### 반복문의 상태 status 객체      
반복문에는 부가적으로 사용할 수 있는 상태status 객체라는 것이 있다.     
상태 객체를 이용하면 순번이나 인덱스 번호, 홀수/짝수 등을 지정할 수 있다.     

```html
<ul>
    <li th:each="dto, status : ${list}">
        [[${status.index}]] --- [[${dto}]]
    </li>
</ul>
```

추가된 코드를 보면 status라는 변수가 추가된 것을 볼 수 있다.     
변수의 이름은 다른 이름을 사용하는 것도 가능하다. 상태 객체에서는 index 혹은 count라는 속성을 이용할 수 있는데      
index는 0부터 시작하고, count는 1부터 시작한다는 점이 가장 큰 차이이다.      

#### 제어문 처리
Thymeleaf의 제어문 처리는 th:if~unless 등을 이용할 수도 있고, 삼항연산자 스타일을 사용할 수도 있다.    
예를 들어 sno의 값이 5의 배수인 수들만 출력하라는 지시가 있다면 다음과 같이 하면 된다.      

```html
<ul>
    <li th:each="dto, status : ${list}" th:if="${dto.sno % 5 == 0}">
        [[${dto}]]
    </li>
</ul>
```

th:if와 th:unless를 이용한다면 상황에 맞게 다른 내용을 출력하는 것이 가능하다.    
다른 언어들은 'if~else'가 하나의 묶음으로 처리되지만 Thymeleaf는 단독으로 처리한다는 차이가 있다.    
예를 들어, sno를 5로 나눈 나머지가 0인 경우에는 sno만을 출력하고 그렇지 않다면 SampleDTO의 first를 출력하라는 지시가 있다면 다음과 같이 하면 된다.     

```html
    <ul>
        <li th:each="dto, status : ${list}">
            <span th:if="${dto.sno%5==0}" th:text="${'-----------------'+dto.sno}"></span>
            <span th:unless="${dto.sno%5==0}" th:text="${dto.first}"></span>
        </li>
    </ul>
```

Thymeleaf의 th:if를 이용하는 방식이 번거롭다면 삼항연산자 방식을 이용하는 것도 좋다.      
Thymeleaf는 삼항연산자를 사용할 수 있는데 특이하게도 단순 if와 같이 2개의 항만으로 처리할 수 있다.       
예를 들어 sno가 5로 나눈 나머지가 0인 경우 sno만 출력하고,    
나머지는 first를 출력하고 싶다면 다음과 같이 작성할 수 있다.     

```html
    <ul>
        <li th:each="dto, status : ${list}" th:text="${dto.sno%5==0}?${dto.sno}: ${dto.first}">
        </li>
    </ul>
```

이와 같은 방식을 이용하면 특정한 상황에만 적용하는 CSS의 클래스를 지정하는 등의 작업을 편하게 할 수 있다.      
sno를 5로 나눈 나머지가 0인 경우에만 특정한 CSS를 적용한다면 다음과 같은 코드를 작성할 수 있다.

```html
    <style>
        .target{
            background-color:red;
        }
    </style>
    <ul>
        <li th:each="dto, status : ${list}" th:class="${dto.sno%5==0}?'target'" th:text="${dto}"></li>
    </ul>
```

#### inline 속성
Thymeleaf의 여러 속성 중에서 개발에 많이 도움되는 기능이 inline 속성이다.     
inline 속성은 주로 javaScript 처리에서 유용하다.     

```java
SampleController 추가분

    @GetMapping({"/exInline"})
    public String exInline(RedirectAttributes redirectAttributes){
        log.info("exInline.............");

        SampleDTO dto = SampleDTO.builder()
        .sno(100L)
        .first("First...100")
        .last("Last...100")
        .regTime(LocalDateTime.now())
        .build();
        redirectAttributes.addFlashAttribute("result", "success");
        redirectAttributes.addFlashAttribute("dto", dto);

        return "redirect:/sample/ex3";
        }
    @GetMapping("/ex3")
    public void ex3(){
        log.info("ex3");
        }
```

추가된 exInline()은 내부적으로 RedirectAttributes를 이용하여 '/ex3'으로 result와 dto라는 이름을 데이터에 심어서 전달한다.    
result는 단순한 문자열이지만, dto는 SampleDTO의 객체이다.     

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>

    <h1 th:text="${result}"></h1>
    <h1 th:text="${dto}"></h1>

    <script th:inline="javascript">
        var msg = [[${result}]];
        var dto = [[${dto}]];
    </script>
</body>
</html>
```

ex3.html에서 가장 중요한 부분은 < script > 태그에 사용된 th:inline 속성이다.        
속성값이 javascript로 지정되었는데 이로 인해 많은 변화가 생긴다.     
또한, 브라우저에서는 /sample/exInline을 호출해도 리다이렉트되기 때문에 브라우저는 /sample/ex3을 다시 호출하고 아래의 결과를 보여준다.
```html
var msg = "success";
var dto = {"sno":100,"first":"First...100","last":"Last...100","regTime":"2022-06-15T23:33:37.4604202"};
```

별도의 처리가 없음에도 불구하고 자동으로 ""이 추가되어 문자열이 되는 것을 볼 수 있고,      
같이 전송된 dto는 JSON 포맷의 문자열이 된 것을 볼 수 있다.      
만일 위의 코드를 javaScript 객체로 변환해서 사용하고자 한다면 간단히 JSON.parse('/"'+dto+'/"'); 와 같은 형태로 ""을 추가해서 사용할 수 있다.       

###### th:block     
Thymeleaf의 th:block은 조금 독특하지만 유용한 기능이다. th:block은 별도의 태그가 필요하지 않기 때문에     
반드시 태그에 붙어서 th:text나 th:value 등을 써야 하는 제약이 없다.      
예를 들어 sno를 5로 나눈 나머지가 0인 경우에는 sno를 출력하고 그렇지 않다면 first를 출력하는 경우는 다음과 같다.    

```html
    <ul>
        <th:block th:each="dto: ${list}">
            <li th:text="${dto.sno%5==0}?${dto.sno}:${dto.first}"></li>
        </th:block>
    </ul>
```

th:block은 실제 화면에서는 html로 처리되지 않기 때문에 루프 등을 별도로 처리하는 용도로 많이 사용된다.     

#### 링크 처리
Thymeleaf의 링크는 '@{}'를 이용해서 사용한다. 특별하게 다른 점이라고 할 수는 없지만,    
파라미터를 전달해야 하는 상황에서는 좀 더 가독성 좋은 코드를 만들 수 있다.      

```java
exModel()을 재사용하기 위해 /exLink 추가

@GetMapping({"/ex2", "/exLink"})
public void exModel(Model model){
        List<SampleDTO> list = IntStream.rangeClosed(1, 20).asLongStream()
        .mapToObj(i->{
        SampleDTO dto = SampleDTO.builder()
        .sno(i)
        .first("First..."+i)
        .last("Last..."+i)
        .regTime(LocalDateTime.now())
        .build();
        return dto;
        }).collect(Collectors.toList());
        model.addAttribute("list", list);
        }
```
@GetMapping()에는 배열을 이용해서 하나 이상의 URL을 처리할 수 있으므로 /sample/exLink라는 경로를 처리할 수 있는 구조로 수정한다.    

```html
exLink.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
  <ul>
    <li th:each="dto:${list}">
      <a th:href="@{/sample/exView}">[[${dto}]]</a>
    </li>
  </ul>
</body>
</html>
```

exLink.html 내에는 @{}로 구성된 링크를 처리하고 있다.       
현재 생성된 링크는 단순히 /sample/exView라는 링크이므로,      
sno와 같은 파라미터를 추가하기 위해서는 다음과 같이 키와 값의 형태를 추가한다.

```html
    <ul>
        <li th:each="dto:${list}">
            <a href="@{/sample/exView(sno=${dto.sno})}">[[${dto}]]</a>
        </li>
    </ul>
```

만일 /exLink/3과 같이 sno를 path로 이용하고 싶다면 다음과 같이 처리할 수 있다.    

```html
    <ul>
        <li th:each="dto:${list}">
            <a th:href="@{/sample/exView/{sno}(sno=${dto.sno})}">[[${dto}]]</a>
        </li>
    </ul>
```

## Thymeleaf의 기본 객체와 LocalDateTime      
Thymeleaf는 내부적으로 여러 종류의 기본 객체basic objects라는 것을 지원한다.     
기본 객체는 문자나 숫자, 웹에서 사용되는 파라미터, request, response, session 등 다양한데    
개발자는 이를 이용해서 JSP와 유사하거나 좀 더 편리하게 코드를 작성할 수 있다.    

JSP를 이용해서 숫자나 날짜를 처리하기 위해서는 별도의 JSTL 설정 등이 필요하지만 Thymeleaf는 #numbers나 #dates 등을       
별도의 설정 없이 사용할 수 있다. 예를 들어 화면에 출력하는 sno를 모두 5자리로 만드는 상황이라면 아래와 같이 하면 된다.       

```html
    <ul>
        <li th:each="dto:${list}">
            [[${#numbers.formatInteger(dto.sno, 5)}]]
        </li>
    </ul>
```

내장 객체들이 많은 기능을 지원하지만 아쉽게도 Java 8버전에 등장한 LocalDate 타입이나     
LocalDateTime에 대해서는 상당히 복잡한 방식으로 처리해야 하는 단점이 있다.     
이를 좀 더 편하게 처리하기 위해서는 https://github.com/thymeleaf/thymeleaf-extras-java8time 을 이용하는 것이 편리하다.      
프로젝트 내에 생성된 build.gradle에 의존성을 다음과 같이 추가한다.

```xml
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
	implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.jetbrains:annotations:20.1.0'
    compileOnly 'org.projectlombok:lombok'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'
	annotationProcessor 'org.projectlombok:lombok'
	providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	
	compile group: 'org.thymeleaf.extras', name:'thymeleaf-extras-java8time'
}
버전 차이인지 추가하면 에러뜨고 안하면 된다.
spring-boot-starter-thymeleaf가 다 불러주는 듯 하다.
```

실제 화면에서는 #temporals라는 객체를 이용해서 format()으로 처리한다.    

## Thymeleaf의 레이아웃

Thymeleaf의 레이아웃 기능은 크게 2가지 형태로 사용할 수 있다.     

- JSP의 include와 같이 특정 부분을 외부 혹은 내부에서 가져와서 포함하는 형태.     
- 특정한 부분을 파라미터로 전달해서 내용에 포함하는 형태.          

#### include 방식의 처리

Thymeleaf의 기능 중에서는 특정한 부분을 다른 내용으로 변경할 수 있는 th:insert나 th:replace라는 기능이 있다.     
(예전에는 th:include가 있었지만 3버전부터는 사용할 수 없다.)      
th:replace를 이용하는 경우는 기존의 내용을 완전히 대체하는 방식이고,     
th:insert의 경우에는 기존 내용의 바깥쪽 태그는 그대로 유지하면서 추가되는 방식이라는 차이가 있다.      

```java
SampleController 추가

@GetMapping("/exLayout1")
public void exLayout1(){
        log.info("exLayout......");
        }
```

작성하려는 exLayout.html은 내부적으로 다른 파일에 있는 일부분을 조각처럼 가져와서 구성할 것이다.    

```html
fragment1.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<div th:fragment="part1">
    <h2>Part 1</h2>
</div>
<div th:fragment="part2">
    <h2>Part 2</h2>
</div>
<div th:fragment="part3">
    <h2>Part 3</h2>
</div>
</body>
</html>

```
fragment1.html 파일은 일부분의 태그들을 th:fragment라는 속성으로 표현한다.     
위 코드에서 th:fragment로 작성된 부분은 작성할 exLayout1.html에서 가져다 쓰기 위한 부분이다.     
exLayout1.html은 화면을 담당한다.

```html
exLayout1.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
    <h1>Fragment Test</h1>

    <h1>Layout 1 - 1</h1>
    <div th:replace="~{/fragments/fragment1 :: part1}"></div>
    <h1>Layout 1 - 2</h1>
    <div th:insert="~{/fragments/fragment1 :: part2}"></div>
    <h1>Layout 1 - 3</h1>
    <th:block th:replace="~{/fragments/fragment1 :: part3}"></th:block>
</body>
</html>
```

작성된 코드를 보면 < div > 혹은 < th:block >을 이용해서 fragment1 파일에 있는 조각들을 사용하는 것을 볼 수 있다.      
소스 코드에서 Layout1-2부분은 th:insert를 이용해서 th:replace와 비교한다.    

생성된 코드를 보면 th:insert를 이용하는 경우에는 <div> 태그 내에 다시 <div> 태그가 생성된 것을 확인할 수 있다.     
th:replace를 이용할 때 :: 뒤에는 fragment의 이름을 지정하거나 CSS의 #id와 같은 선택자를 이용할 수 있다.     

파일 전체를 사용하는 예제의 실습을 위해 fragments 폴더에 파일의 모든 내용이 하나의 조각이 되는 fragment2.html 파일을 작성한다.     

```html
<div>
  <hr/>
  <h2>Fragment2 File</h2>
  <h2>Fragment2 File</h2>
  <h2>Fragment2 File</h2>
  <hr/>
</div>
```
exLayout1.html에는 작성된 fragment2.html 전체를 가져오는 부분을 추가한다.

```html
    <h1>Fragment Test</h1>
    <div style="border: 1px solid blue">
        <th:block th:replace="~{/fragments/fragment2}"></th:block>
    </div>
    <h1>Layout 1 - 1</h1>
    <div th:replace="~{/fragments/fragment1 :: part1}"></div>

    <h1>Layout 1 - 2</h1>
    <div th:replace="~{/fragments/fragment1 :: part2}"></div>

    <h1>Layout 1 - 3</h1>
    <th:block th:replace="~{/fragments/fragment1 :: part3}"></th:block>
```
코드에서 th:replace="~{/fragments/fragment2}"부분에 :: 로 처리되는 부분이 없으므로 전체의 내용을 반영하게 된다.


###### 파라미터 방식의 처리
기존의 JSP와 달리 Thymeleaf를 이용하면 특정한 태그를 파라미터처럼 전달해서 다른 th:fragment에서 사용할 수 있다.    
SampleController의 exLayout1()을 재사용하기 위하여 수정한다.

```java
@GetMapping({"/exLayout1", "/exLayout2"})
public void exLayout1(){
    log.info("exLayout...........");
}
```

fragment3.html 내부에는 파라미터를 받는 th:fragment를 선언한다.
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
  <div th:fragment="target(first, second)">
      <style>
          .c1{
            background-color:red;
          }
          .c2{
            background-color:bluew;
          }
      </style>
      <div class="c1">
          <th:block th:replace="${first}"></th:block>
      </div>
      <div class="c2">
          <th:block th:replace="${second}"></th:block>
      </div>      
  </div>
</body>
</html>
```

선언된 target 부분에는 first와 second라는 파라미터를 받을 수 있도록 구성한 것을 알 수 있다.     
또한 내부적으로 th:block으로 이를 표현하고 있다. 실제 target을 사용하는 작업은 exLayout2.html로 진행한다.      
templates/sample 폴더 내에 exLayout2.html 파일을 추가한다.   

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
    <th:block th:replace="~{/fragments/fragment3::target(~{this::#ulFirst}, ~{this::#ulSecond})}">
        <ul id="ulFirst">
            <li>AAA</li>
            <li>BBB</li>
            <li>CCC</li>
        </ul>
        <ul id="ulFSecond">
            <li>111</li>
            <li>222</li>
            <li>333</li>
        </ul>
    </th:block>
</html>
```
코드를 보면 exLayout2.html에는 화면 구성과 관련된 기능이 없는 것을 볼 수 있다.    
대신 상단에 target을 사용할 때 파라미터를 2개 사용하고 있다.    

- this: #urlFirst - this는 현재 페이지를 의미할 때 사용하는데 생략이 가능하다.     
- #urlFirst는 CSS의 id 선택자를 의미한다.     

최종 결과는 fragments/fragment3.html의 내용의 일부로 전달된 #ulFirst, #ulSecond를 사용하게 된다.      

#### 레이아웃 템플릿 만들기      
파라미터로 필요한 영역을 전달해서 처리할 수 있다면 레이아웃 전체를 하나의 페이지로 구성하고,      
필요한 부분을 파라미터로 전달하는 방식을 통해 공통의 레이아웃을 사용할 수 있다는 의미가 된다.      

```html
layout1.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<style>
        * {
            margin:0;
            padding:0;
        }
        .header{
            width:100vw;
            height:20vh;
            background-color:aqua;
        }
        .content{
            width:100vw;
            height:70vh;
            background-color: lightgray;
        }
        .footer{
            width:100vw;
            height:10vh;
            background-color:green;
        }
    </style>
<div class="header">
    <h1>HEADER</h1>
</div>
<div class="content">
    <h1>CONTENT</h1>
</div>
<div class="footer">
    <h1>FOOTER</h1>
</div>
</body>
</html>
```
실제 개발을 한다면 중간에 CONTENT로 표시된 영역이 다른 내용으로 변경되어야 하기 때문에 위의 화면 전체는 하나의 템플릿으로 만들고,      
중간의 CONTENT 영역을 변경 가능하도록 수정한다.

```html
layout1.html 의 <head> 상단에 추가
<th:block th:fragment="setContent(content)">
```

상단에 처리된 부분은 th:fragment로 setContent로 지정하고 content라는 파라미터를 받을 수 있도록 변경되었다.      
화면의 중간 부분은 파라미터로 전달되는 content를 표시할 수 있도록 수정한다.     

```html
<div class="content">
    <th:block th:replace = "${content}">
    </th:block>
</div>
```

중간 부분은 th:replace를 이용해서 파라미터로 전달되는 content를 출력한다.    
마지막 하단부는 <th:block>을 닫아주도록 한다.    

```html
</body>
</th:block>
</html>
```

SampleController에서는 layout에 있는 layout1.html을 사용하기 위한 /exTemplate 경로를 작성한다.     

```java
@GetMapping({"/exLayout1", "/exLayout2", "/exTemplate"})
public void exLayout1(){
    log.info("exLayout............");
}
```

sample 폴더에는 exTemplate.html 파일을 추가한다.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/layout1 :: setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1>exTemplate Page</h1>
    </th:block>
</th:block>
```

작성된 exTemplate.html 파일은 전체 내용은 layout1.html로 처리하고 파라미터로 현재 파일의 content 부분만을 전달한다.     

결과를 보면 전체적인 레이아웃은 layout1.html의 내용이 사용되는 것을 볼 수 있고,     
중간 CONTENT로 출력된 영역이 exTemplate.html의 내용으로 변경된 것을 확인할 수 있다.       
위와 같이 Thymeleaf를 이용해서 전체 레이아웃을 가지는 페이지를 작성하고    
필요한 부분만을 개발해서 템플릿으로 적용할 수 있다.     

#### 부트스트랩 템플릿 적용하기

