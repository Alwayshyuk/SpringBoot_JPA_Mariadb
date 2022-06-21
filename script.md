## 프로젝트의 와이어프레임     
웹 프로젝트를 구성할 때는 가장 먼저 와이어프레임(화면 설계서)을 제작하고 진행하는 것이 좋다.        
와이어프레임을 제작하면 화면의 URI와 전달되는 파라미터 등을 미리 결정할 수 있고,      
데이터베이스 설계에 필요한 칼럼들을 미리 파악하는데도 도움이 된다.       

#### 프로젝트의 화면 구성
이번에 작성하려는 프로젝트는 하나의 Entity 클래스를 이용해서 CRUD 기능과 검색/페이징 기능을 가지는 웹 애플리케이션이다.    

> 목록: URL(/questbook/list), GET, 기능(목록/페이징/검색)      
> 등록: URL(/guestbook/register), GET, 입력화면         
> 등록: URL(/guestbook/register), POST, 등록처리, Redirect URL(/guestbook/list)         
> 조회: URL(/guestbook/read), GET, 조회 화면         
> 수정: URL(/guestbook/modify), GET, 수정/삭제 기능 화면          
> 수정: URL(/guestbook/modify), POST, 수정 처리, Redirect URL(/guestbook/read)             
> 삭제: URL(/guestbook/remove), POST, 삭제 처리, Redirect URL(/guestbook/list)        

프로젝트의 기본 구조는 다음과 같다.      

> ThymeleafPage1, ...2, ...         
> Controller 계층 : GuestbookController          
> Service 계층 : GuestbookService 인터페이스, GuestbookServiceImple 클래스            
> Repository 계층 : GuestbookRepository             

1. 브라우저에서 들어오는 Request는 GuestbookController라는 객체로 처리한다.          
2. GuestbookController는 GuestbookService 타입을 주입받는 구조로 만들고, 이를 이용해서 원하는 작업을 처리한다.           
3. GuestbookRepository는 Spring Data JPA를 이용해서 구성하고, GuestbookServiceImpl 클래스에 주입해서 사용한다.           
4. 마지막 결과는 Thymeleaf를 이용해서 레이아웃 템플릿을 활용해서 처리한다.             

위 구조를 처리하는데 있어서 각 계층 사이에는 데이터를 주고 받는 용도의 클래스를 이용하게 된다.     

DTO -> GuestbookController -> DTO -> GuestbookServiceImpl -> Entity 객체 -> GuestRepository      
-> Entity 객체 -> GuestbookServiceImpl -> DTO -> GuestController -> DTO -> Thymeleaf page            

- 브라우저에서 전달되는 Request는 GuestbookController에서 DTO의 형태로 처리된다.          
- GuestbookRepository는 Entity타입을 이용하므로 중간에 Service 계층에서는 DTO와 Entity의 변환을 처리한다.       

JPA를 이용하는 경우 Entity객체는 항상 JPA가 관리하는 콘텍스트context에 속해 있기 때문에          
가능하면 JPA 영역을 벗어나지 않도록 작성하는 방식을 권장한다.         

#### 프로젝트 생성        
생성된 guestbook 프로젝트는 데이터베이스 관련된 설정이 존재하지 않으므로 실행 시에 문제가 발생한다.        
먼저 MariaDB관련 JDBC 드라이버를 추가하고, JPA와 관련된 설정을 추가한다.          
또한, Thymeleaf에서 사용하게 될 java8 날짜 관련 라이브러리도 추가한다.                         

```
build.gradle 추가

	compile group: 'org.mariadb.jdbc', name: 'mariadb-java-client'
	compile group: 'org.thymeleaf.extras', name:'thymeleaf-extras-java8time'
	
	compile은 구버전에서 사용하던 방법이다. 이대로 작성하면 에러뜬다.
	implementation 로 바꾸자.
```

생성된 프로젝트를 실행하면 데이터베이스 관련된 내용에서 에러가 발생하므로 데이터 베이스 관련 설정도 추가한다.            

```
application.properties 추가

spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.datasource.url=jdbc:mariadb://localhost:3306/bootex
spring.datasource.username=bootuser
spring.datasource.password=bootuser


spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=true


spring.thymeleaf.cache=false

```

#### 컨트롤러/화면 관련 준비

Thymeleaf를 이용하는 설정은 이전에 작성해 둔 layout 폴더와 static 폴더의 파일들을 그대로 사용한다.     

```java
package org.zerock.guestbook.controller;

@Controller
@RequestMapping("/guestbook")
@Log4j2
public class GuestbookController {
    @GetMapping({"/", "/list"})
    public String list(){
        log.info("list.........");
        return"/guestbook/list";
    }
}

```

list.html은 layout 폴더의 basic.html을 이용하는 구조로 작성하고 간단한 텍스트를 출력하는 내용으로 작성한다.        

```html
list.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1>GuestBook List Page</h1>
    </th:block>
</th:block>
```

## 자동으로 처리되는 날짜/시간 설정
Entity와 관련된 작업을 하다보면, 데이터의 등록 시간, 수정 시간과 같이 자동으로 추가되고 변경되어야 하는 칼럼들이 있다.        
이를 매번 프로그램 안에서 처리하는 일은 번거롭기 때문에 자동으로 처리할 수 있도록 어노테이션을 이용해서 설정한다.       

프로젝트 내에 entity 패키지를 생성하고, 엔티티 객체의 등록 시간과 최종 수정 시간을 담당하게 될 BaseEntity 클래스를 추상 클래스로 작성한다.      

```java
package org.zerock.guestbook.entity;

@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
@Getter
abstract class BaseEntity {
    @CreatedDate
    @Column(name="regdate", updatable = false)
    private LocalDateTime regDate;
    
    @LastModifiedDate
    @Column(name="moddate")
    private LocalDateTime modDate;
}

```

BaseEntity 클래스에는 @MappedSuperClass라는 특별한 어노테이션이 적용되는데 이 어노테이션이 적용된 클래스는 테이블로 생성되지 않는다.      
실제 테이블은 BaseEntity클래스를 상속한 Entity의 클래스로 데이터베이스 테이블이 생성된다.          

JPA는 JPA만의 고유한 메모리 공간(context)을 이용해서 Entity 객체들을 관리한다.       
기존의 MyBatis 기반의 프로그램과 달리 단계가 하나 더 있는 방식이다.        

MyBatis를 이용하는 경우에는 SQL을 위해서 전달되는 객체는 모두 SQL 처리가 끝난 후에는 어떻게 되는 상관없는 객체들인 반면에        
JPA에서 사용하는 Entity 객체들은 영속 콘텍스트라는 곳에서 관리되는 객체이다.       
이 객체들이 변경되면 결과적으로 데이터베이스에 이를 반영하는 방식이다.       

JPA 내부에서 엔티티 객체가 생성/변경되는 것을 감지하는 역할은 AuditingEntityListener로 이루어 진다.        
이를 통해서 regDate, modDate에 적절한 값이 지정된다.        

@CreatedDate는 JPA에서 Entity의 생성 시간을 처리하고, @LastModifiedDate는 최종 수정 시간을 자동으로 처리하는 용도로 사용한다.         
속성으로 insertable, updateable 등이 있는데 위 코드에서는 updatable=false로 지정했다.          
이를 통해서 해당 Entity 객체를 데이터베이스에 반영할 때 regdate 칼럼값은 변경되지 않는다.          

JPA를 이용하면서 AuditingEntityListener를 활성화시키기 위해서는 프로젝트에 @EnableJpaAuditing 설정을 추가해야 한다.        
프로젝트 생성 시에 존재하는 GuestbookApplication을 수정한다.          

```java
package org.zerock.guestbook;

@SpringBootApplication
@EnableJpaAuditing
public class GuestbookApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuestbookApplication.class, args);
	}

}

```

## Entity 클래스와 Querydsl 설정
 
기존의 Entity 클래스를 작성할 때와 달리 BaseEntity 클래스를 상속해서 entity 패키지에 Guestbook 엔티티 클래스를 작성한다.        

```java
package org.zerock.guestbook.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Guestbook extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gno;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 1500, nullable = false)
    private String content;

    @Column(length = 50, nullable = false)
    private String writer;
}
```

작성된 Guestbook의 내용은 이전 메모장 예제와 등록 시간과 수정 시간을 처리해준다는 것을 제외하면 거의 동일하다.
프로젝트를 시작하면 정상적으로 테이블이 SQL에 만들어진 것을 확인할 수 있다.        
```
Hibernate: 
    
    create table guestbook (
       gno bigint not null auto_increment,
        moddate datetime(6),
        regdate datetime(6),
        content varchar(1500) not null,
        title varchar(100) not null,
        writer varchar(50) not null,
        primary key (gno)
    ) engine=InnoDB
```
프로젝트 내에 repository 패키지를 추가하고, GuestbookRepository 인터페이스를 작성한다.     

GuestbookRepository는 먼저 기존과 동일하게 JpaRepository를 상속받는 인터페이스로 구성한다.      

```java
package org.zerock.guestbook.repository;

public interface GusetbookRepository extends JpaRepository<Guestbook, Long> {
}

```

#### 동적 쿼리 처리를 위한 Querydsl 설정       
JPA의 쿼리 메서드의 기능과 @Query를 통해서 많은 기능을 구현할 수는 있지만,           
아쉽게도 선언할 때 고정된 형태의 값을 가진다는 단점이 있다.      
이 때문에 단순한 몇 가지의 검색 조건을 만들어야 하는 상황에서는 기본 기능만으로 충분하지만,          
복잡한 조합을 이용하는 경우의 수가 많은 상황에서는 동적으로 쿼리를 생성해서 처리할 수 있는 기능이 필요하다.         
Querydsl은 이러한 상황을 처리할 수 있는 기술이다.       
Querydsl을 이용하면 복잡한 검색조건이나 조인, 서브 쿼리 등의 기능도 구현이 가능하다.       
공식 사이트는 http://www.querydsl.com/ 으로 이 중에서 JPA 관련 부분을 적용할 것이다.          

Querydsl을 이용하면 코드 내부에서 상황에 맞는 쿼리를 생성할 수 있지만 이를 위해서는 작성된 엔티티 클래스를 그대로 이용하는 것이 아닌         
Q도메인이라는 것을 이용해야만 한다. 그리고 이를 작성하기 위해서는 Querydsl 라이브러리를 이용해서 Entity 클래스를 Q도메인 클래스로       
변환하는 방식을 이용하기 때문에 이를 위한 추가적인 설정이 필요하다.           

우선 build.gradle 파일에 아래의 내용을 처리해야 한다.       

- plugins 항목에 querydsl 관련 부분을 추가한다.         
- dependencis 항목에 필요한 라이브러리를 추가한다.        
- Gradle에서 사용할 추가적인 task를 추가한다.         

```java
build.gradle 수정
버전 때문에 고생하다 다 바꿨다..

        buildscript {
        ext {
        queryDslVersion = "5.0.0"
        }
        }


        plugins {
        id 'org.springframework.boot' version '2.6.4'
        id 'io.spring.dependency-management' version '1.0.11.RELEASE'
        id 'java'
        }

        group = 'org.zerock'
        version = '0.0.1-SNAPSHOT'
        sourceCompatibility = '11'

        configurations {
        compileOnly {
        extendsFrom annotationProcessor
        }
        }

        repositories {
        mavenCentral()
        }

        dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
        implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
        implementation 'org.springframework.boot:spring-boot-starter-web'
        compileOnly 'org.projectlombok:lombok'
        developmentOnly 'org.springframework.boot:spring-boot-devtools'
        runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'

        testCompileOnly 'org.projectlombok:lombok'
        testAnnotationProcessor 'org.projectlombok:lombok'

        implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.1.0'

        implementation "com.querydsl:querydsl-jpa:${queryDslVersion}"

        annotationProcessor(

        "javax.persistence:javax.persistence-api",

        "javax.annotation:javax.annotation-api",

        "com.querydsl:querydsl-apt:${queryDslVersion}:jpa")


        implementation 'org.modelmapper:modelmapper:3.1.0'

        implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.1.0'

        implementation 'org.springframework.boot:spring-boot-starter-validation'



        implementation 'net.coobird:thumbnailator:0.4.16'

        }

        tasks.named('test') {
        useJUnitPlatform()
        }

        sourceSets {
        main {
        java {
        srcDirs = ["$projectDir/src/main/java", "$projectDir/build/generated"]
        }
        }
        }
```

위 내용을 추가하고 build.gradle 파일을 갱신한다.     
갱신 후 프로젝트 내 build 폴더 안에 Q로 시작하고 엔티티 클래스의 이름과 동일한 파일이 생성된 것을 확인할 수 있다.     
생성된 QGuestBook 클래스를 보면 내부적으로 선언된 필드(칼럼)들이 변수 처리된 것을 확인할 수 있다.         

자동으로 생성된다는 사실이 의미하는 것은 Q로 시작되는 클래스들은 개발자가 직접 건드리지 않는다는 것이다.         
gradle의 compileQuerydsl과 같은 task를 통해서 자동으로 생성되는 방법만을 사용한다.        
Querydsl을 이용하게 되면 GuestbookRepository 인터페이스 역시 QuerydslPredicateExecutor라는 인터페이스를 추가로 상속한다.         

```java
package org.zerock.guestbook.repository;

public interface GusetbookRepository extends JpaRepository<Guestbook, Long>, QuerydslPredicateExecutor<Guestbook> {

}

```

#### 엔티티의 테스트
test 폴더 내에 repository 패키지를 생성하고 GuestbookRepositoryTests 클래스를 추가한다.      

```java
package org.zerock.guestbook.repository;

@SpringBootTest
public class GuestbookRepositoryTests {
    @Autowired
    private GuestbookRepository guestbookRepository;

    @Test
    public void insertDummies(){
        IntStream.rangeClosed(1, 300).forEach(i->{
            Guestbook guestbook = Guestbook.builder()
                    .title("Title..."+i)
                    .content("Content..."+i)
                    .writer("user"+(i%10))
                    .build();
            System.out.println(guestbookRepository.save(guestbook));
        });
    }
}
```

GuestbookRepositoryTests에는 300개의 테스트 데이터를 먼저 넣는다.      
생성된 데이터의 regdate(생성시간), moddate(수정시간)이 자동으로 채워진 것을 확인할 수 있다.        

###### 수정 시간 테스트      
엔티티 클래스는 가능하면 setter 관련 기능을 만들지 않는 것이 권장 사항이긴 하지만,      
필요에 따라서는 수정 기능을 만들기도 한다.(엔티티 객체가 애플리케이션 내부에서 변경되면 JPA를 관리하는 쪽이       
복잡해질 우려가 있기 때문에 가능하면 최소한의 수정이 가능하도록 하는 것을 권장한다.)          

제목title과 내용content을 수정할 수 있도록 Guestbook 클래스에 changeTitle(), changeContent()와 같은 메서드를 추가한다.     

```java
    public void changeTitle(String title){
        this.title = title;
    }
    public void changeContent(String content){
        this.content = content;
    }
```

BaseEntity의 modeDate는 최종 수정 시간이 반영되기 때문에 특정한 엔티티를 수정한 후에 save()했을 경우 동작한다.         
정상적으로 동작하는지를 확인하기 위해 테스트 코드를 작성한다.         

```java
@Test
public void updateTest(){
    Optional<Guestbook> result=guestbookRepository.findById(300L);

    if(result.isPresent()){
        Guestbook guestbook=result.get();

        guestbook.changeTitle("Change Title...");
        guestbook.changeContent("Change Content...");

        guestbookRepository.save(guestbook);
        }
}
```

#### Querydsl 테스트

Querydsl의 실습은 다음과 같은 상황을 처리한다.      

- 제목/내용/작성자와 같이 단 하나의 항목으로 검색하는 경우       
- 제목+내용/내용+작성자/제목+작성자와 같이 2개의 항목으로 검색하는 경우          
- 제목+내용+작성자와 같이 3개의 항목으로 검색하는 경우          

만일 Guestbook 엔티티 클래스에 많은 멤버 변수들이 선언되어 있었다면 이러한 조합의 수는 굉장히 많아지게 된다.         
이런 상황을 대비해서 상황에 맞게 쿼리를 처리할 수 있는 Querydsl이 필요하다.          

Querydsl의 사용법은 다음과 같다.        

- BooleanBuilder를 생성한다.        
- 조건에 맞는 구문은 Querydsl에서 사용하는 Predicate 타입의 함수를 생성한다.          
- BooleanBuilder에 작성된 Predicate를 추가하고 실행한다.          

###### 단일 항목 검색 테스트          

예제로 제목title에 1이라는 글자가 있는 엔티티들을 검색해보면 다음과 같이 작성할 수 있다.      

```java
    @Test
    public void testQuery1(){
        Pageable pageable = PageRequest.of(0, 10, Sort.by("gno").descending());

        QGuestbook qGuestbook = QGuestbook.guestbook; //1

        String keyword="1";

        BooleanBuilder builder = new BooleanBuilder(); //2

        BooleanExpression expression = qGuestbook.title.contains(keyword); //3

        builder.and(expression); //4

        Page<Guestbook> result = guestbookRepository.findAll(builder, pageable); //5
        result.stream().forEach(guestbook -> {
            System.out.println(guestbook);
        });
    }
```

1. 가장 먼저 동적으로 처리하기 위해 Q도메인 클래스를 얻어온다. Q도메인 클래스를 이용하면                 
    엔티티 클래스에 선언된 title, content같은 필드들을 변수로 활용할 수 있다.     
2. BooleanBuilder는 where문에 들어가는 조건들을 넣어주는 컨테이너라고 생각하면 된다.        
3. 원하는 조건은 필드 값과 같이 결합해서 생성한다.      
    BooleanBuilder 안에 들어가는 값은 con.querydsl.core.types.Predicate 타입이어야 한다.          
4. 만들어진 조건은 where문에 and나 or같은 키워드와 결합시킨다.           
5. BooleanBuilder는 GuestbookRepository에 추가된 QuerydslPredicateExecutor 인터페이스의 findAll()을 사용할 수 있다.         

이를 통해서 페이지 처리와 동시에 검색 처리가 가능해진다.          

###### 다중 항목 검색 테스트       
복합 조건은 여러 조건이 결합된 형태이다.          
예를 들어 제목 혹은 내용에 특정한 키워드가 있고 gno가 0보다 크다와 같은 조건을 처리해 보도록 한다.       
BooleanBuilder는 and() 혹은 or()의 파라미터로 BooleanBuilder를 전달할 수 있어서 복잡한 쿼리를 생성할 수 있다.         
아래 예제는 제목 혹은 내용에 특정한 키워드가 있고, gno가 0보다 크다라는 조건을 처리한다.        

```java
    @Test
    public void testQuery2(){
        Pageable pageable = PageRequest.of(0,10,Sort.by("gno").descending());
        QGuestbook qGuestbook = QGuestbook.guestbook;
        String keyword = "1";
        BooleanBuilder builder = new BooleanBuilder();
        BooleanExpression exTitle = qGuestbook.title.contains(keyword);
        BooleanExpression exContent = qGuestbook.content.contains(keyword);

        BooleanExpression exAll = exTitle.or(exContent);//1
        builder.and(exAll);//2
        builder.and(qGuestbook.gno.gt(0L));//3
        Page<Guestbook> result = guestbookRepository.findAll(builder, pageable);

        result.stream().forEach(guestbook -> {
            System.out.println(guestbook);
        });
    }
```

코드의 작성 과정은 이전과 유사하지만 중간에 exTitle과 exContent라는 BooleanExpression을 결합하는 부분(1)과         
이를 BooleanBuilder에 추가하고(2), 이후에 gno가 0보다 크다라는 조건을 추가한(3) 부분이 차이가 난다.        

## 서비스 계층과 DTO       

이전 예제들은 단순히 쿼리를 작성하거나 테스트하는 수준이었지만, 실제 프로젝트를 작성할 경우에 엔티티 객체를 영속 계층 바깥쪽에서 사용하는 방식보다      
DTO(Data Transfer Object)를 이용하는 방식을 권장한다.         

DTO는 엔티티 객체와 달리 각 계층끼리 주고받는 우편물이나 상자의 개념이다.          
순수하게 데이터를 담고 있다는 점에서는 엔티티 객체와 유사하지만,        
목적 자체가 데이터의 전달이므로 읽고, 쓰는 것이 모두 허용되는 점이 가능하고 일회성으로 사용되는 성격이 강하다.            

JPA를 이용하게 되면 엔티티 객체는 단순히 데이터를 담는 객체가 아니라 실제 데이터베이스와 관련이 있고,        
내부적으로 엔티티 매니저가 관리하는 객체이다.          
DTO가 일회성으로 데이터를 주고받는 용도로 사용되는 것과 달리 생명주기도 전혀 다르기 때문에 분리해서 처리하는 것을 권장한다.            

> 웹 애플리케이션을 제작할 때는 HttpServletRequest나 HttpServletResponse를 서비스 계층으로 전달하지 않는 것을 원칙으로 한다.        
> 유사하게 엔티티 객체가 JPA에서 사용하는 객체이므로 JPA 외에서 사용하지 않는 것을 권장한다.          

예제에서는 서비스 계층을 생성하고 서비스 계층에서는 DTO로 파라미터와 리턴 타입을 처리하도록 구성할 것이다.         
DTO를 사용하면 엔티티 객체의 범위를 한정지을 수 있기 때문에 좀 더 안전한 코드를 작성할 수 있고,              
화면과 데이터를 분리하려는 취지에도 좀 더 부합한다.           
DTO를 사용하는 경우 가장 큰 단점은 Entity와 유사한 코드를 중복으로 개발한다는 점과,            
엔티티 객체를 DTO로 변환하거나 반대로 DTO 객체를 엔티티로 변환하는 과정이 필요하다는 것이다.         

```java
package org.zerock.guestbook.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestbookDTO {
    private Long gno;
    private String title, content, writer;
    private LocalDateTime regDate, modDate;
}
```

작성된 GuestbookDTO는 엔티티 클래스인 Guestbook과 거의 동일한 필드들을 가지고 있고,        
getter/setter를 통해 자유롭게 값을 변경할 수 있게 구성한다.            
서비스 계층에서는 GuestbookDTO를 이용해서 필요한 내용을 전달받고, 반환하도록 처리하는데          
GuestbookService 인터페이스와 GuestbookServiceImpl 클래스를 작성한다.       

가장 먼저 GuestbookDTO를 이용해서 새로운 방명록을 등록하는 시나리오를 처리한다.        

```java
package org.zerock.guestbook.service;

public interface GuestbookService {
    Long register(GuestbookDTO dto);
}



package org.zerock.guestbook.service;

@Service
@Log4j2
public class GuestbookServiceImpl  implements GuestbookService{

    @Override
    public Long register(GuestbookDTO dto) {
        return null;
    }
}

```

#### 등록과 DTO를 엔티티로 변환하기        
서비스 계층에서는 파라미터를 DTO 타입으로 받기 때문에 이를 JPA로 처리하기 위해서는 엔티티 타입의 객체로 변환해야 하는 작업이 필요하다.        
이 기능을 DTO 클래스에 적용하거나 ModelMapper라이브러리나 MapStruct 등을 이용하기도 하는데 여기서는 직접 처리하는 방식으로 한다.       
기존의 DTO와 엔티티 클래스를 변경하지 않기위해 GuestbookService 인터페이스에 default 메서드를 이용해서 이를 처리한다.     

> Java 8버전부터는 인터페이스의 실제 내용을 가지는 코드를 default라는 키워드로 생성할 수 있다.       
> 이를 이용하면 기존에 추상 클래스를 통해서 전달해야 하는 실제 코드를 인터페이스에 선언할 수 있다.     
> 이를 통해서 인터페이스->추상 클래스->구현 클래스의 형태로 구현되던 방식에서 추상 클래스를 생략하는 것이 가능해진다.         

```java
default Guestbook dtoToEntity(GuestbookDTO dto){
        Guestbook entity = Guestbook.builder()
                .gno(dto.getGno())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(dto.getWriter())
                .build();
        return entity;
    }
```

GuestbookService에는 인터페이스 내에 default 기능을 활용해서 구현클래스에서 동작할 수 있는 dtoToEntity()를 구성한다.            
GuestbookServiceImpl 클래스에서는 이를 활용해서 파라미터로 전달되는 GuestbookDTO를 변환한다.        

```java
    @Override
    public Long register(GuestbookDTO dto) {
        log.info("DTO--------------");
        log.info(dto);

        Guestbook entity = dtoToEntity(dto);
        
        log.info(entity);

        return null;
    }
```

###### 서비스 계층의 테스트      

작성된 GuestbookService 역시 최종적으로 테스트 작업을 통해서 확인하고 이후에 컨트롤러와 연동시키는 것이 좋다.        
test폴더에 service 패키지를 추가하고, 아래의 코드를 작성한다.         

```java
package org.zerock.guestbook.service;

@SpringBootTest
public class GuestbookServiceTests {

    @Autowired
    private GuestbookService service;

    @Test
    public void testRegister(){

        GuestbookDTO guestbookDTO = GuestbookDTO.builder()
                .title("Sample Title...")
                .content("Sample Content...")
                .writer("user0")
                .build();

        System.out.println(service.register(guestbookDTO));
    }
}
```

testRegister()는 실제로 데이터베이스에 저장되지는 않지만 GuestbookDTO를 Guestbook엔티티로 변환한 결과를 확인할 수 있다.       
변환 작업에 문제가 없다면 GuestbookServiceImpl 클래스를 수정해서 실제로 데이터베이스에 처리가 완료되도록 한다.        

```java
public class GuestbookServiceImpl  implements GuestbookService{

    private final GuestbookRepository repository;

    @Override
    public Long register(GuestbookDTO dto) {
        log.info("DTO--------------");
        log.info(dto);

        Guestbook entity = dtoToEntity(dto);
        log.info(entity);

        repository.save(entity);

        return entity.getGno();
    }
}
```

GuestbookServiceImpl 클래스는 JPA 처리를 위해서 GuestbookRepository를 입력하고 클래스 선언 시에         
@RequiredArgsConstructor를 이용해서 자동으로 주입한다.         
register()의 내부에서는 save()를 통해서 저장하고, 저장된 후에 해당 엔티티가 가지는 gno값을 반환한다.          

## 목록 처리

목록을 처리하는 과정은 다음과 같은 상황을 고려해야 한다.       

- 화면에서 필요한 목록 데이터에 대한 DTO 생성           
- DTO를 Pageable 타입으로 전환            
- Page<Entity>를 화면에서 사용하기 쉬운 DTO의 리스트 등으로 변환           
- 화면에 필요한 페이지 처리

#### 목록 처리를 위한 DTO            
목록을 처리하는 작업은 단순히 현재 예제에서만 사용되는 것이 아니고 거의 모든 게시판 관련 기능에서                 
사용될 가능성이 높기 때문에 재사용이 가능한 구조로 생성하는 것이 좋다.            
모든 목록을 처리하는 기능에는 페이지 번호나 한 페이지당 몇 개나 출력될 것인가와 같은              
공톡적인 부분이 많기 때문에 이를 클래스를 이용하면 재사용이 가능하다.           

###### 페이지 요청 처리 DTO(PageRequestDTO)         
작성하려고 하는 PageRequestDTO는 목록 페이지를 요청할 때 사용하는 데이터를 재사용하기 쉽게 만드는 클래스이다.          
목록 화면에서는 페이지 처리를 하는 경우가 많이 있기 때문에 페이지 번호나 페이지 내 목록의 개수, 검색 조건 들이 많이 사용된다.           
PageRequestDTO는 이러한 파라미터를 DTO로 선언하고 나중에 재사용하는 용도로 사용한다.        

화면에서 전달되는 목록 관련 데이터에 대한 DTO를 PageRequestDTO라는 이름으로 생성하고,          
화면에서 필요한 결과는 PageRequestDTO라는 이름의 클래스로 생성한다.         

```java
package org.zerock.guestbook.dto;

@Builder
@AllArgsConstructor
@Data
public class PageRequestDTO {
    private int page, size;

    public PageRequestDTO(){
        this.page = 1;
        this.size = 10;
    }

    public Pageable getPageable(Sort sort){
        return PageRequest.of(page -1, size sort);
    }
}

```

PageRequestDTO는 화면에서 전달되는 page라는 파라미터와 size라는 파라미터를 수집하는 역할을 한다.          
다만 페이지 번호 등은 기본값을 가지는 것이 좋기 때문에 1과 10이라는 값을 사용한다.           
PageRequestDTO의 진짜 목적은 JPA쪽에서 사용하는 Pageable 타입의 객체를 생성하는 것이다.           
추후에 수정이 필요하지만, JPA를 이용하는 경우에는 페이지 번호가 0부터 시작한다는 점을 감안해서           
1페이지의 경우 0이 될 수 있도록 page-1을 하는 형태로 작성한다.         
정렬은 나중에 다양한 상황에서 사용하기 위해 별도의 파라미터를 받도록 설계했다.         

###### 페이지 결과 처리 DTO(PageResultDTO)              
JPA를 이용하는 Repository에서는 페이지 처리 결과를 Page<Entity> 타입으로 반환하게 된다.          
따라서 서비스 계층에서 이를 처리하기 위해서도 별도의 클래스를 만들어서 처리해야 한다.           
처리하는 클래스는 크게 다음과 같은 내용이다.         

- Page<Entity>의 엔티티 객체들을 DTO 객체로 변환해서 자료구조로 담아 주어야 한다.         
- 화면 출력에 필요한 페이지 정보들을 구성해 주어야 한다.        

이러한 작업을 위해 PageResultDTO는 임시로 다음과 같은 형태로 구성한다.       

```java
package org.zerock.guestbook.dto;

@Data
public class PageResultDTO<DTO, EN> {
    private List<DTO> dtoList;

    public PageResultDTO(Page<EN> result, Function<EN, DTO> fn){
        
        dtoList = result.stream().map(fn).collect(Collectors.toList());
    }
}

```

PageResultDTO 클래스는 다양한 곳에서 사용할 수 있도록 제네릭 타입을 이용해서 DTO와 EN(Entity)이라는 타입을 지정한다.          
PageResultDTO는 Page<Entity> 타입을 이용해서 생성할 수 있도록 생성자로 작성한다.       
이때 특별한 Function<EN, DTO>는 엔티티 객체들을 DTO로 변환해 주는 기능이다.         

위와 같은 구조를 이용하면 나중에 어떤 종류의 Page<E> 타입이 생성되더라도, PageResultDTO를 이용해서 처리할 수 있다는 장점이 있다.         
프로젝트를 진행하는 과정에서는 다양한 종류의 엔티티를 다루기 때문에 위와 같은 제네릭 방식으로 적용해 두면      
나중에 추가적인 클래스를 작성하지 않고도 목록 데이터를 처리할 수 있게 된다.          

> PageResultDTO는 List<DTO> 타입으로 DTO 객체들을 보관한다. 그렇다면 Page<Entity>의 내용물 중에서 엔티티 객체를 DTO로 변환하는 기능이 필요하다.       
> 가장 일반적인 형태는 추상 클래스를 이용해서 이를 처리해야 하는 방식이지만 이 경우 매번 새로운 클래스가 필요하다는 단점이 있다.          
> 여기서 엔티티 객체의 DTO 변환은 서비스 인터페이스에 정의한 메서드(entityToDto())와 별도로 Function 객체로 만들어서 처리한다.      

###### 서비스 계층에서는 목록 처리       
서비스 계층에서는 PageRequestDTO를 파라미터로, PageResultDTO를 리턴타입으로 사용하는 getList()를 설계하고,           
엔티티 객체를 DTO 객체로 변환하는 entityToDto()를 정의한다.         

```java
public interface GuestbookService {
    Long register(GuestbookDTO dto);

    PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO requestDTO);
    
    default Guestbook dtoToEntity(GuestbookDTO dto){
        Guestbook entity = Guestbook.builder()
                .gno(dto.getGno())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(dto.getWriter())
                .build();
        return entity;
    }

    default GuestbookDTO entityToDto(Guestbook entity){
        GuestbookDTO dto = GuestbookDTO.builder()
                .gno(entity.getGno())
                .title(entity.getTitle())
                .content(entity.getContent())
                .writer(entity.getWriter())
                .regDate(entity.getRegDate())
                .modDate(entity.getModDate())
                .build();

        return dto;
    }
}

@Service
@Log4j2
@RequiredArgsConstructor
public class GuestbookServiceImpl  implements GuestbookService{

    private final GuestbookRepository repository;

    @Override
    public Long register(GuestbookDTO dto) {
        log.info("DTO--------------");
        log.info(dto);

        Guestbook entity = dtoToEntity(dto);
        log.info(entity);

        repository.save(entity);

        return entity.getGno();
    }

    @Override
    public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.by("gno").descending());

        Page<Guestbook> result = repository.findAll(pageable);

        Function<Guestbook, GuestbookDTO> fn = (entity-> entityToDto(entity));

        return new PageResultDTO<>(result, fn);
    }


}
```

getList()에서 눈여겨 볼 부부은 entityToDTO()를 이용해서 java.util.Function을 생성하고 이를 PageResultDTO로 구성하는 부분이다.          
PageResultDTO에는 JPA의 처리 결과인 Page<Entity>와 Function을 전달해서 엔티티 객체들을 DTO의 리스트로 변환하고, 화면에 페이지 처리와 필요한 값들을 생성한다.        

###### 목록 처리 테스트       
목록 처리 테스트는 우선적으로 엔티티 객체들이 DTO 객체들로 변환되었는지를 살펴본다.         
```java
GuestbookServiceTests 추가

    @Test
    public void testList(){
        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .page(1).size(10).build();
        PageResultDTO<GuestbookDTO, Guestbook> resultDTO = service.getList(pageRequestDTO);

        for(GuestbookDTO guestbookDTO : resultDTO.getDtoList()){
            System.out.println(guestbookDTO);
        }
    }
```
PageRequestDTO를 이용하기 때문에 생성할 때는 1페이지부터 처리할 수 있고, 정렬은 상황에 맞게 Sort 객체를 생성해서 전달하는 형태로 사용한다.       

테스트 결과를 보면 Page<Guestbook>이 List<GuestbookDTO>로 변환되어 출력 결과에 GuestbookDTO 타입으로 출력되는 것을 볼 수 있다.         

###### 목록 데이터 페이지 처리          
화면까지 전달되는 데이터는 PageResultDTO이고, 이를 이용해서 화면에서는 페이지 처리를 진행하게 될 것이다.           
PageResultDTO 타입으로 처리된 결과에는 시작 페이지, 끝 페이지 등 필요한 모든 정보를 담아서 화면에서는        
필요한 내용들만 찾아서 구성이 가능하도록 작성한다.      

화면에서 필요한 구성은 다음과 같다.           

- 화면에서 시작 페이지 번호 start          
- 화면에서 끝 페이지 번호 end           
- 이전/ 다음 이동 링크 여부 prev, next            
- 현재 페이지 번호 page         

페이징 처리를 하기 위해서 가장 필요한 정보는 현재 사용자가 보고 있는 페이지의 정보이다.            
예를 들어, 사용자가 5페이지를 본다면 화면의 페이지 번호는 1부터 시작하지만        
사용자가 19페이지를 본다면 11부터 시작해야 하기 때문이다.       
흔히들 페이지를 계산할 때 시작 번호를 먼저 하려고 하지만, 오히려 끝 번호를 먼저 계산해 두는 것이 수월하다.       
끝 번호는 다음과 같은 공식으로 구할 수 있다.     

tempEnd = (int)(Math.ceil(페이지번호 / 10.0)) * 10;

Math.ceil()은 소수점을 올림으로 처리하기 때문에 다음과 같은 상황이 가능하다.         

- 1페이지의 경우: Math.ceil(0.1)*10 = 10           
- 10페이지의 경우: Math.ceil(1) * 10 = 10           
- 11페이지의 경우: Math.ceil(1.1) * 10 = 20         

끝 번호는 아직 개선의 여지가 있다.           
만일 전체 데이터 수가 적다면 10페이지로 끝나면 안되는 상황이 생길 수 있기 때문이다.         
그럼에도 끝 번호를 먼저 계산하는 이유는 시작 번호를 계산하기 수월하기 때문이다.           
만일 화면에 10개씩 보여준다면 시작 번호는 무조건 임시로 만든 끝 번호에서 9라는 값을 뺀 값이 된다.      

start = tempEnd-9;         

끝 번호는 실제 마지막 페이지와 다시 비교할 필요가 있다. 예를 들어 Page<Guestbook>의 마지막 페이지가 33이라면      
위의 계싼이라면 40이 되기 때문에 이를 반영해야 한다.         
이를 위해서는 Page<Guestbook>의 getTotalPages()를 이용할 수 있다.            

totalPage = result.getTotalPages();//result는 Page<Guestbook>           
end = totalPage>tempEnd?tempEnd:totalPage;          

이전prev과 다음next는 간단하게 구할 수 있다.     
이전의 경우는 시작 번호가 1보다 큰 경우라면 존재하게 된다.        

prev = start>1;         

다음으로 가는 링크는 위의 totalPage가 끝 번호보다 큰 경우에만 존재하게 된다.        

next = totalPage>tempEnd;     

위의 내용들을 PageResultDTO 클래스에 반영하면 다음과 같은 형태가 된다.        

```java
package org.zerock.guestbook.dto;

@Data
public class PageResultDTO<DTO, EN> {

    //DTO리스트
    private List<DTO> dtoList;

    //총 페이지 번호
    private int totalPage;

    //현재 페이지 번호
    private int page;
    //목록 사이즈
    private int size;

    //시작 페이지 번호, 끝 페이지 번호
    private int start, end;

    //이전, 다음
    private boolean prev, next;

    //페이지 번호 목록
    private List<Integer> pageList;

    public PageResultDTO(Page<EN> result, Function<EN, DTO> fn){

        dtoList = result.stream().map(fn).collect(Collectors.toList());

        totalPage = result.getTotalPages();
        makePageList(result.getPageable());
    }

    private void makePageList(Pageable pageable){
        this.page = pageable.getPageNumber()+1;//0부터 시작하므로 1 추가
        this.size = pageable.getPageSize();

        //temp end page
        int tempEnd = (int)(Math.ceil(page/10.0))*10;

        start = tempEnd-9;

        prev = start>1;

        end = totalPage > tempEnd ? tempEnd : totalPage;

        next = totalPage > tempEnd;

        pageList = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
    }
}
```

```java
GuestbookServiceTests 일부

    @Test
    public void testList(){
        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
        .page(1).size(10).build();
        PageResultDTO<GuestbookDTO, Guestbook> resultDTO = service.getList(pageRequestDTO);

        System.out.println("PREV: "+resultDTO.isPrev());
        System.out.println("NEXT: "+resultDTO.isNext());
        System.out.println("TOTAL: "+resultDTO.getTotalPage());

        System.out.println("------------------------------------");
        for(GuestbookDTO guestbookDTO : resultDTO.getDtoList()){
        System.out.println(guestbookDTO);
        }

        System.out.println("=====================================");
        resultDTO.getPageList().forEach(i-> System.out.println(i));

        }
```

## 컨트롤러와 화면에서의 목록 처리
등록 작업과 목록 처리가 완료되었으므로 컨트롤러를 작성하고, 실제 화면에 이를 적용한다.        
GuestbookController에는 목록을 처리하기 위해 /guestbook/list 로 처리되는 부분을 다음과 같이 처리한다.     

```java
package org.zerock.guestbook.controller;

@Controller
@RequestMapping("/guestbook")
@Log4j2
@RequiredArgsConstructor // 자동 주입을 위한 Annotation
public class GuestbookController {

    private final GuestbookService service;

    @GetMapping({"/"})
    public String list(){
        log.info("list.........");
        return"/guestbook/list";
    }

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model){
        log.info("list............"+pageRequestDTO);

        model.addAttribute("result", service.getList((pageRequestDTO)));
    }
}
```

GuestbookController에서 list()에는 파라미터로 PageRequestDTO를 이용한다.      
스프링 MVC는 파라미터를 자동으로 수집해 주는 기능이 있으므로, 화면에서 page와 size라는 파라미터를 전달하면         
PageRequestDTO 객체로 자동으로 수집될 것이다. Model은 결과 데이터를 화면에 전달하기 위해 사용하고 있다.        

> 사실 Spring Data JPA를 이용할 때는 @PageableDefault라는 어노테이션으로 Pageable타입을 이용할 수도 있고,        
> application.properties에 0이 아닌 1부터 페이지 번호를 받을 수 있게 처리할 수도 있다.            
> 예제에서 이 방식을 이용하지 않는 것은 추후에 검색 조건 등과 같이 추가로 전달되어야 하는 데이터가      
> 많은 경우에 오히려 더 복잡하게 동작할 여지가 있기 때문이다.         

list()에 Model을 이용해서 GuestbookServiceImpl에서 반환하는 PageResultDTO를 result라는 이름으로 전달한다.         
실제 내용을 출력하는 list.html에서는 부트스트랩의 테이블 구조를 이용해서 출력한다.   

우선 list.html 쪽으로 전달된 dtoList를 이용해서 GuestbookDTO들을 출력한다.       

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">
  <th:block th:fragment="content">
    <h1 class="mt-4">GuestBook List Page</h1>

    <table class="table table-striped">
      <thead>
      <tr>
        <th scope="col">#</th>
        <th scope="col">Gno</th>
        <th scope="col">Title</th>
        <th scope="col">RegDate</th>
      </tr>
      </thead>
      <tbody>
      <tr th:each="dto : ${result.dtoList}">
        <th scope="row">[[${dto.gno}]]</th>
        <td>[[${dto.title}]]</td>
        <td>[[${dto.writer}]]</td>
        <td>[[${#temporals.format(dto.regDate, 'yyyy/MM/dd')}]]</td>
      </tr>
      </tbody>
    </table>
  </th:block>
</th:block>
```

th:each를 이용해서 PageResultDTO 안에 들어있는 dtoList를 반복 처리한다.         
마지막에 시간 처리는 년/월/의 포맷으로 출력하도록 조정한다.      

#### 목록 페이지 처리       
다음 작업은 정상적으로 페이지 이동이 동작하는지 확인하고 화면 아래쪽에 페이지 처리와 클릭 시 페이지의 이동을 처리하는 것이다.       

- /guestbook/list 혹은 /guestbook/list?page=1의 경우 1페이지가 출력된다.     
- /guestbook/list?page=2와 같이 페이지 번호를 변경하면 해당 페이지가 나온다.      

list.html에는 아래와 같은 코드를 이용해 화면에 페이지가 출력되도록 조정한다.   
```html
</table>
<ul class="pagenation h-100 justify-content-center align-items-center">
    <li class="page-item" th:if="${result.prev}">
        <a class="page-link" href="#" tabindex="-1">Previous</a>
    </li>
    <li th:class="'page-item'+${result.page == page? 'active' : ''}"
        th:each="page:${result.pageList}">
        <a class="page-link" href="#">
            [[${page}]]
        </a>
    </li>
    <li class="page-item" th:if="${result.next}">
        <a class="page-link" href="#">Next</a>
    </li>
</ul>
```

페이지의 이전과 다음 부분은 Thymeleaf의 if를 이용해서 처리하고,       
페이지 중간에 현재 페이지 여부를 체크해서 active라는 이름의 클래스가 출력되도록 작성한다.     

아직 링크나 이벤트 처리가 없는 상태이므로 페이지 번호를 이동하기 위해서는 브라우저의 주소창에서 page 파라미터 값을 변경해야 한다.       

###### 페이지 번호 링크 처리          
Thymeleaf에서는 링크를 th:href를 이용해서 작성한다.     

```html
list.html 일부
<ul class="pagination h-100 justify-content-center align-items-center">
    <li class="page-item" th:if="${result.prev}">
        <a class="page-link" tabindex="-1" th:href="@{/guestbook/list(page=${result.start-1})}">Previous</a>
    </li>
    <li th:class="'page-item'+${result.page == page? 'active' : ''}" th:each="page:${result.pageList}">
        <a class="page-link" th:href="@{/guestbook/list(page=${page})}">
            [[${page}]]
        </a>
    </li>
    <li class="page-item" th:if="${result.next}">
        <a class="page-link" th:href="@{/guestbook/list(page=${result.end+1})}">Next</a>
    </li>
</ul>
```

이전의 경우에는 PageResultDTO의 start 값에서 1보다 적은 값으로 지정하고,        
다음의 경우는 반대로 PageResultDTO의 end 값보다 1이 크도록 지정한다.      

만일 링크 처리 시에 (page={page}, size=${result.size})와 같은 내용을 추가한다면      
10개가 아닌 원하는 수만큼의 목록을 조회할 수 있다.        

## 등록 페이지와 등록 처리         
등록의 처리는 이미 GuestbookService까지 완성되었기 때문에 GuestbookController에 약간의 코드를 추가해서 처리가 가능하다.       
우선 GuestbookController에 @GetMapping과 @PostMapping을 이용해서 등록 작업을 처리하는 메서드를 작성한다.        

```java
    @GetMapping("/register")
    public void register(){
        log.info("register get...");
    }

    @PostMapping("/register")
    public String registerPost(GuestbookDTO dto, RedirectAttributes redirectAttributes){
        log.info("dto..."+dto);

        Long gno = service.register(dto);

        redirectAttributes.addFlashAttribute("msg", gno);
        return "redirect:/guestbook/list";
    }
```

등록 작업은 GET 방식에서는 화면을 보여주고, POST 방식에서는 처리 후에 목록 페이지로 이동하도록 설계한다.         
이떄 RedirectAttributes를 이용해서 한 번만 화면에서 msg라는 이름의 변수를 사용할 수 있도록 처리한다.        
addFlashAttribute()는 단 한 번만 데이터를 전달하는 용도로 사용한다.       
브라우저에 전달되는 msg를 이용해서는 화면상에 모달 창을 보여주는 용도로 사용할 것이다.      

등록 화면 register.html은 부트스트랩의 Form 태그를 이용하는 방식을 참고로 해서 구성한다.      

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">
  <th:block th:fragment="content">
    <h1 class="mt-4">GuestBook Register Page</h1>

    <form th:action="@{/guestbook/register}" th:method="post">
      <div class="form-group">
        <label>Title</label>
        <input type="text" class="form-control" name="title" placeholder="Enter Title">
      </div>
      <div class="form-group">
        <label>Content</label>
        <textarea class="form-control" rows="5" name="content"></textarea>
      </div>
      <div class="form-group">
        <label>Writer</label>
        <input type="text" class="form-control" name="writer" placeholder="Enter Writer">
      </div>
      <button type="submit" class="btn btn-primary">Submit</button>
    </form>
  </th:block>
</th:block>
```
< form > 태그에는 action 속성값을 /guestbook/register로 지정하고 POST 방식으로 전송할 수 있도록 처리한다.     
각 < input > 태그에는 적절한 name 값을 지정해야 하는데 GuestbookDTO로 수집될 데이터이므로 동일하게 맞춰주면 자동으로 수집된다.         

###### 등록 처리와 목록 페이지의 모달창      
등록 처리는 POST 방식으로 이루어지고 자동으로 /guestbook/list로 이동하도록 처리되어 있다.          
처리된 후에 목록 화면에서는 처리되었다는 결과를 보여줄 필요가 있으므로 부트스트랩의 모달창을 이용해서 이를 처리한다.      
우선은 list.html 화면에서 JavaScript를 이용해서 등록한 후에 전달되는 msg 값을 확인한다.    

```html
    </ul>
    <script th:inline="javascript">
      var msg = [[${msg}]];

      console.log(msg);
    </script>
```

Thymeleaf의 inline 속성을 이용해서 처리하면 별도의 타입 처리가 필요하지 않기 때문에 새로운 글이 동록되면 번호가 출력된다.      
등록 후 redirect되는 목록 화면이 아니고 단순한 링크 이동의 경우에는 msg 변수의 값은 자동으로 null처리 된다.      
등록 후에는 msg 변수에 새로 생성된 글의 번호가 할당되므로 msg 변수의 값을 이용해서 모달창을 실행한다.          

```html
list.html 일부

    <div class="modal" tabindex="-1" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Modal title</h5>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
            <div class="modal-body">
              <p>Modal body text goes here</p>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
              <button type="button" class="btn btn-primary">Save Changes</button>
            </div>
        </div>
      </div>
    </div>
    <script th:inline="javascript">
      var msg = [[${msg}]];

      console.log(msg);

      if(msg){
        $(".modal").modal();
      }
    </script>
```

###### 등록 페이지의 링크와 조회 페이지 링크 처리            
목록 페이지에서 마지막으로 남은 작업은 새로운 글을 작성할 수 있는 링크를 제공하는 것과         
목록에 있는 각 글의 번호나 제목을 클릭했을 때 조회 페이지로 이동하는 작업이다.          
등록 페이지로 가는 링크는 <table>의 위쪽에 버튼을 추가해 적용한다.          

```html
list.html 일부

<h1 class="mt-4">GuestBook List Page</h1>
<span>
      <a th:href="@{/guestbook/register}">
        <button type="button" class="btn btn-outline-primary">REGISTER</button>
      </a>
</span>
```

조회 페이지로 이동하는 링크를 작성할 때는 항상 다시 목록 페이지로 돌아오는 것을 염두에 두고 작업해야 한다.      
최근의 웹 페이지들은 단순히 새 창을 띄우는 방식을 많이 사용하기 때문에 이런 이동을 염두에 두지 않는 경향이 있지만       
고전적인 화면에서는 조회 페이지에서 다시 목록 페이지로 이동하는 방식을 기본으로 사용한다.         

조회 페이지로 이동하는 링크를 /guestbook/read?gno=xxx와 같은 형태라고 가정한다면         
페이지의 번호와 사이즈를 같이 전달하는 방식이므로 /guestbook/read?gno=xxx와 같이 길어지는 문자열을 생성해야만 한다.         
Thymeleaf를 이용할 때 링크 처리는 파라미터와 값을 (키=값)의 형태로 처리할 수 있기 때문에 좀 더 가독성이 좋은 코드를 작성할 수 있다.        

```html
list.html 일부

<tbody>
<tr th:each="dto : ${result.dtoList}">
    <th scope="row">
        <a th:href="@{/guestbook/read(gno=${dto.gno}, page=${result.page})}">
            [[${dto.gno}]]
        </a>
    </th>
    <td>[[${dto.title}]]</td>
    <td>[[${dto.writer}]]</td>
    <td>[[${#temporals.format(dto.regDate, 'yyyy/MM/dd')}]]</td>
</tr>
</tbody>
```

## 방명록의 조회 처리      

방명록의 조회는 아직 GuestbookService에 기능이 구현되지 않은 상태이므로 서비스 계층부터 구현한다.        

```java
GuestbookService 일부 
        GuestbookDTO read(Long gno);
```

GuestbookService에는 read()라는 메서드를 추가하고 파라미터는 Long 타입의 gno 값을, 리턴 타입은 GuestbookDTO로 지정한다.      

```java
GuestbookServiceImpl 일부

@Override
public GuestbookDTO read(Long gno) {
        Optional<Guestbook> result = repository.findById(gno);

        return result.isPresent()?entityToDto(result.get()):null;
}
```

GuestbookServiceImpl 클래스의 내부에는 인터페이스에 정의된 read() 기능을 구현한다.          
GuestbookRepository에서 findById()를 통해서 엔티티 객체를 가져왔다면,      
entityToDto()를 이용해서 DTO를 변환해서 엔티티 객체를 반환한다.       

GuestbookController에서는 GET 방식으로 gno 값을 받아서 Model에 GuestbookDTO 객체를 담아서 전달하도록 코드를 작성한다.         
추가로 나중에 다시 목록 페이지로 돌아가는 데이터를 같이 저장하기 위해서 PageRequestDTO를 파라미터로 같이 사용한다.       
@ModelAttribute는 없어도 처리가 가능하지만 명시적으로 requestDTO라는 이름으로 처리해 두었다.      

```java
GuestbookController 일부

@GetMapping("/read")
public void read(long gno, @ModelAttribute("requestDTO") PageRequestDTO requestDTO, Model model){
        log.info("gno: " + gno);

        GuestbookDTO dto = service.read(gno);

        model.addAttribute("dto", dto);
}
```

방명록의 조회는 등록 화면과 유사하지만 readonly 속성이 적용되고, 다시 목록 페이지로 이동하는 링크와 수정과 삭제가 가능한 링크를 제공한다.        

```java
read.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">

<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">

    <th:block th:fragment="content">
        <h1 class="mt-4">GuestBook Read Page</h1>

    <div class="form-group">
        <label>Gno</label>
        <input type="text" class="form-control" name="gno" th:value="${dto.gno}" readonly>
    </div>
    <div class="form-group">
        <label>Title</label>
        <input type="text" class="form-control" name="title" th:value="${dto.title}" readonly>
    </div>
    <div class="form-group">
        <label>Content</label>
        <input type="text" class="form-control" name="content" th:value="${dto.content}" readonly>
    </div>
    <div class="form-group">
        <label>Writer</label>
        <input type="text" class="form-control" name="writer" th:value="${dto.writer}" readonly>
    </div>
    <div class="form-group">
        <label>RegDate</label>
        <input type="text" class="form-control" name="regDate"
            th:value="${#temporals.format(dto.regDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
    </div>
    <div class="form-group">
        <label>ModeDate</label>
        <input type="text" class="form-control" name="modDate"
            th:value="${#temporals.format(dto.modDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
    </div>
    <a th:href="@{/guestbook/modify(gno=${dto.gno}, page=${requestDTO.page})}">
        <button type="button" class="btn btn-primary">Modify</button>
    </a>
    <a th:href="@{/guestbook/list(page=${requestDTO.page})}">
        <button type="button" class="btn btn-info">List</button>
    </a>

    </th:block>
</th:block>
```

read.html 에서는 dto라는 이름으로 전달된 DTO를 이용해서 글의 내용을 출력하고,             
@ModelAttribute로 처리된 requestDTO로 페이지와 관련된 부분을 처리한다.     

## 방명록의 수정/삭제 처리       

수정과 삭제의 모든 시작은 GET 방식으로 진입하는 수정화면에서 작업을 선택해서 처리하게 된다.      

- Guestbook의 수정(1)은 POST 방식으로 처리하고 다시 수정된 결과를 확인할 수 있는 조회 화면으로 이동한다.       
- 삭제(2)는 POST 방식으로 처리하고 목록 화면으로 이동한다.          
- 목록을 이동하는 작업은 GET 방식으로 처리한다. 이때 기존에 사용하던 페이지 번호 등을 유지해서 이동해야 한다.         

방명록의 수정과 삭제를 구현할 때는 삭제 작업이 상대적으로 단순하므로 삭제를 먼저 처리하고 수정은 마지막에 처리한다.        
게시물의 수정과 삭제는 동일하게 수정/삭제가 가능한 페이지로 이동한 상태에서 선택을 통해서 이루어진다.         

이를 위해 GuestbookController에서는 조회와 비슷하게 GET 방식으로 진입하는      
/Guestbook/modify를 기존의 read()에 어노테이션의 값을 변경해서 처리한다.       

```java
//    @GetMapping("/read")
    @GetMapping({"/read", "/modify"})
    public void read(long gno, @ModelAttribute("requestDTO") PageRequestDTO requestDTO, Model model){
        log.info("gno: " + gno);

        GuestbookDTO dto = service.read(gno);

        model.addAttribute("dto", dto);
    }
```

화면으로 사용하는 modify.html을 생성하고 read.html을 복사해서 그대로 추가한다.     
modify.html은 가장 먼저 화면의 < h1 > 태그의 내용을 수정한다.      
modify.html은 수정/삭제 작업을 POST 방식으로 처리하므로 < form > 태그로 수정하는 내용을 감싸도록 처리한다.     

```html
modify.html

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">

<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1 class="mt-4">GuestBook Modify Page</h1>

        <form action="/guestbook/modify" method="post">
            <input type="hidden" name="page" th:value="${requestDTO.page}">
            <div class="form-group">
                <label>Gno</label>
                <input type="text" class="form-control" name="gno" th:value="${dto.gno}" readonly>
            </div>
            <div class="form-group">
                <label>Title</label>
                <input type="text" class="form-control" name="title" th:value="${dto.title}">
            </div>
            <div class="form-group">
                <label>Content</label>
                <input type="text" class="form-control" name="content" th:value="${dto.content}">
            </div>
            <div class="form-group">
                <label>Writer</label>
                <input type="text" class="form-control" name="writer" th:value="${dto.writer}" readonly>
            </div>
            <div class="form-group">
                <label>RegDate</label>
                <input type="text" class="form-control"
                       th:value="${#temporals.format(dto.regDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
            </div>
            <div class="form-group">
                <label>ModDate</label>
                <input type="text" class="form-control"
                       th:value="${#temporals.format(dto.modDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
            </div>
        </form>

        <button type="button" class="btn btn-primary modifyBtn">Modify</button>

        <button type="button" class="btn btn-info listBtn">List</button>

        <button type="button" class="btn btn-danger removeBtn">Remove</button>

    </th:block>
</th:block>

```

위 코드에서 날짜 부분은 화면에서 수정 자체도 불가능하고 JPA에서 자동으로 처리될 것이므로,         
name 속성 없이 작성되었다. 그리고 화면에 기능을 처리할 때 사용하는 버튼들을 추가하였다.          

modify.html에 있는 버튼은 Modify(수정), Remove(삭제), List(목록) 세 가지이다.        
각 버튼을 클릭할 때 마다 다르게 이벤트를 처리해야 한다. 수정과 삭제 작업은 < form > 태그의 action 속성을 조정해서 처리할 수 있다.      

#### 서비스 계층에서의 수정과 삭제      
현재까지 서비스 계층에 구현된 기능은 수정과 삭제 기능 자체가 없었기 때문에 관련된 기능들을 추가한다.      

```java
GuestbookService 일부
        
        void remove(Long gno);

        void modify(GuestbookDTO dto);
        
        
GuestbookServiceImpl 일부

@Override
public void remove(Long gno) {
        repository.deleteById(gno);
}

@Override
public void modify(GuestbookDTO dto) {
        //업데이트 하는 항목은 제목, 내용

        Optional<Guestbook> result = repository.findById(dto.getGno());

        if(result.isPresent()){
        Guestbook entity = result.get();

        entity.changeTitle(dto.getTitle());
        entity.changeContent(dto.getContent());

        repository.save(entity);
        }
}

```

방명록의 수정은 기존의 엔티티에서 제목과 내용만을 수정하고 다시 저장하는 방식으로 구현한다.      

###### 컨트롤러 게시물 삭제           
방명록의 삭제는 POST 방식으로 gno 값을 전달하고, 삭제 후에는 다시 목록의 첫 페이지로 이동하는 방식이 가장 보편적이다.     
GuestbookController에 메서드를 추가한다.       

```java
    @PostMapping("/remove")
    public String remove(long gno, RedirectAttributes redirectAttributes){
        log.info("gno: "+ gno);
        service.remove(gno);

        redirectAttributes.addFlashAttribute("msg", gno);
        return "redirect:/guestbook/list";
    }
```

#### modify.html 삭제 처리

삭제 작업은 GET 방식으로 수정 페이지에 들어가서 삭제 버튼을 클릭하는 방식으로 제작한다.         
modify.html에는 삭제 외에도 다른 버튼들도 있음으로 이들을 구분하기 위해서 클래스 속성을 아래와 같이 추가한다.          

```html
        <button type="button" class="btn btn-primary modifyBtn">Modify</button>

        <button type="button" class="btn btn-info listBtn">List</button>

        <button type="button" class="btn btn-danger removeBtn">Remove</button>
```

삭제 버튼을 클릭할 때의 이벤트 처리는 아래와 같이 구성한다.      

```html
modify.html 일부

<script th:inline="javascript">
    var actionForm = $("form");

    $(".removeBtn").click(function(){

    actionForm.attr("action", "/guestbook/remove")
                .attr("method", "post");

    actionForm.submit();
    });
</script>
```

Remove 버튼을 클릭하면 < form > 태그의 action 속성과 method 속성을 조정한다.        
< form > 태그 내에는 < input > 태그로 gno가 있기 때문에 컨트롤러에서는 여러 파라미터 중에서 gno를 추출해서 삭제 시에 이용하게 된다.       

#### POST 방식의 수정 처리      
수정 처리는 POST 방식으로 이루어져야 하는데 다음과 같은 점들을 고려해야 한다.        

- 수정 시에 수정해야하는 내용(제목, 내용, 글번호)이 전달되어야 한다.        
- 수정된 후에는 목록 페이지로 이동하거나 조회 페이지로 이동해야 한다. 가능하면 기존의 페이지 번호를 유지하는 것이 좋다.              

현재 modify.html에는 /guestbook/read로 이동할 때 페이지 번호가 파라미터로 전달되고 있고,        
수정 페이지로 이동할 경우에도 같이 전달된다.      

이를 이용해서 수정이 완료된 후에도 다시 동일한 정보를 유지할 수 있도록 page 값 역시 < form > 태그에 추가해서 전달한다.       

```html
modify.html 일부

<form action="/guestbook/modify" method="post">
    <input type="hidden" name="page" th:value="${requestDTO.page}">
```

###### 컨트롤러의 수정 처리      
GuestbookController에서는 Guestbook 자체의 수정과 페이징 관련 데이터 처리를 같이 진행해야 한다.        

```java
    @PostMapping("/modify")
    public String modify(GuestbookDTO dto, @ModelAttribute("requestDTO") PageRequestDTO requestDTO, 
                            RedirectAttributes redirectAttributes){
        log.info("post modify...............");
        log.info("dto: "+dto);
        
        service.modify(dto);
        
        redirectAttributes.addAttribute("page", requestDTO.getPage());
        redirectAttributes.addAttribute("gno", dto.getGno());
        
        return "redirect:/guestbook/read";
    }
```
GuestbookController에서는 세 개의 파라미터가 사용되는 것을 볼 수 있다.      
수정해야 하는 글의 정보를 가지는 GuestbookDTO, 기존의 페이지 정보를 유지하기 위한 PageRequestDTO,         
마지막으로 리다이렉트로 이동하기 위한 RedirectAttributes가 필요하다.        
수정 작업이 진행된 이후에는 조회 페이지로 이동한다.     
이때 기존 페이지 정보도 같이 유지해서 조회 페이지에서 다시 목록 페이지로 이동하는데 어려움이 없도록 한다.       

#### 수정 화면에서의 이벤트 처리       
컨트롤러를 호출하는 화면에서는 Modify 버튼의 이벤트 처리를 통해서 작업한다.        

```html
        $(".modifyBtn").click(function(){
            if(!confirm("수정하시겠습니까?")){
                return;
            }
            actionForm.attr("action", "/guestbook/modify")
                    .attr("method", "post")
                    .submit();
        });
```
#### 수정 화면에서 다시 목록 페이지로         
글의 수정과 삭제가 완료되었다면 마지막으로 다시 목록 페이지로 이동하는 버튼을 처리한다.       
현재 modify.html은 < form > 태그를 이용해서 모든 작업이 이루어지고 있고     
여러 개의 < input >을 이용해서 파라미터들을 전송한다.        

목록 페이지로 이동하는 경우에는 page와 같은 파라미터 외에 다른 파라미터들은 별도로 필요하지 않다.         
따라서 목록을 이동할 경우에는 page를 제외한 파라미터들은 지운 상태로 처리하는 것이 깔끔하다.        

```html
modify.html 일부

$(".listBtn").click(function(){
    var pageInfo = $("input[name='page']");
    
    actionForm.empty(); //form 태그의 모든 내용을 지우고
    actionForm.append(pageInfo);    //목록 페이지 이동에 필요한 내용을 다시 추가
    actionForm.attr("action", "/guestbook/list")
                .attr("method", "get");
    
    console.log(actionForm.html());
    actionForm.submit();
});
```

이벤트 처리 시에는 우선 page와 관련된 부분만 따로 보관하고, empty()를 이용해서 < form > 태그 내의 모든 파라미터를 삭제한다.      
빈 < form > 태그에 보관해둔 page 관련 정보를 추가하고 목록 페이지로 이동하도록 구현한다.          

## 검색 처리      
검색 처리는 크게 서버 사이드 처리와 화면 쪽의 처리로 나누어 볼 수 있다.      

서버 사이드 처리는 다음과 같다.      
- PageRequestDTO에 검색 타입(type)과 키워드(keyword)를 추가         
- 이하 서비스 계층에서 Querydsl을 이용해서 검색 처리        

검색 항목은 크게 다음과 같이 정의한다.        
- 제목t, 내용c, 작성자w로 검색하는 경우        
- 제목 혹은 내용tc으로 검색하는 경우         
- 제목 혹은 내용 혹은 작성자tcw로 검색하는 경우          

#### 서버측 검색 처리         
우선 PageRequestDTO에 검색 조건type과 검색 키워드keyword를 추가한다.       

```java
package org.zerock.guestbook.dto;

@Builder
@AllArgsConstructor
@Data
public class PageRequestDTO {
    private int page, size;
    private String type, keyword;
    
    public PageRequestDTO(){
        this.page = 1;
        this.size = 10;
    }

    public Pageable getPageable(Sort sort){
        return PageRequest.of(page -1, size, sort);
    }
}
```

#### 서비스 계층의 검색 구현과 테스트        
동적으로 검색 조건이 처리되는 경우의 실제 코딩은 Querydsl을 통해서 BooleanBuilder를 작성하고,        
GuestbookRepository는 Querydsl로 작성된 BooleanBuilder를 findAll() 처리하는 용도로 사용한다.         
BooleanBuilder 작성은 별도의 클래스 등을 작성해서 처리할 수 있지만 간단히 하려면       
GuestbookServiceImpl 내에 메서드를 하나 작성해서 처리하면 된다.        

```java
    private BooleanBuilder getSearch(PageRequestDTO requestDTO){    //Querydsl 처리
        String type=requestDTO.getType();

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        QGuestbook qGuestbook = QGuestbook.guestbook;

        String keyword = requestDTO.getKeyword();

        BooleanExpression expression = qGuestbook.gno.gt(0L);   //gno>0 조건만 생성

        booleanBuilder.and(expression);

        if(type==null || type.trim().length()==0){  //검색 조건이 없는 경우
            return booleanBuilder;
        }

        //검색 조건을 작성하기
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if(type.contains("t")){
            conditionBuilder.or(qGuestbook.title.contains(keyword));
        }
        if(type.contains("c")){
            conditionBuilder.or(qGuestbook.content.contains(keyword));
        }
        if(type.contains("w")){
            conditionBuilder.or(qGuestbook.content.contains(keyword));
        }

        //모든 조건 통합
        booleanBuilder.and(conditionBuilder);

        return booleanBuilder;
    }
```

GuestbookServiceImpl에 작성한 getSearch()는 PageRequestDTO를 파라미터로 받아서 검색 조건(type)이 있는 경우에는      
conditionBuilder 변수를 생성해서 각 검색 조건을 or로 연결해서 처리한다.         
반면에 검색 조건이 없다면 gno > 0 으로만 생성된다.      
GuestbookServiceImpl에서 목록을 조회할 때 사용하던 getList()는 기존의 코드를 조금 수정해서 다음과 같이 작성한다.        

```java
    @Override
    public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.by("gno").descending());

        BooleanBuilder booleanBuilder = getSearch(requestDTO);  //검색 조건 처리

        Page<Guestbook> result = repository.findAll(booleanBuilder, pageable);

        Function<Guestbook, GuestbookDTO> fn = (entity-> entityToDto(entity));

        return new PageResultDTO<>(result, fn);
    }
```

위와 같이 서비스 영역에서 검색 조건을 처리할 수 있도록 구성했다면 테스트 코드로 결과를 확인한다.      

```java
    @Test
    public void testSearch(){
        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .page(1)
                .size(10)
                .type("tc")
                .keyword("한글")
                .build();

        PageResultDTO<GuestbookDTO, Guestbook> resultDTO = service.getList(pageRequestDTO);

        System.out.println("PREV: "+resultDTO.isPrev());
        System.out.println("NEXT: "+resultDTO.isNext());
        System.out.println("TOTAL: "+resultDTO.getTotalPage());

        System.out.println("-------------------------------------");
        for(GuestbookDTO guestbookDTO : resultDTO.getDtoList()){
            System.out.println(guestbookDTO);
        }
        System.out.println("=====================================");
        resultDTO.getPageList().forEach(i-> System.out.println(i));

    }
```

위 테스트에서 실행되는 쿼리는 아래와 같다.                

```
Hibernate: 
    select
        guestbook0_.gno as gno1_0_,
        guestbook0_.moddate as moddate2_0_,
        guestbook0_.regdate as regdate3_0_,
        guestbook0_.content as content4_0_,
        guestbook0_.title as title5_0_,
        guestbook0_.writer as writer6_0_ 
    from
        guestbook guestbook0_ 
    where
        guestbook0_.gno>? 
        and (
            guestbook0_.title like ? escape '!' 
            or guestbook0_.content like ? escape '!'
        ) 
    order by
        guestbook0_.gno desc limit ?
```

쿼리의 where절 안쪽에서는 검색 조건이 처리되고 gno > 0 조건은 바깥쪽으로 처리된 것을 볼 수 있다.          

#### 목록 페이지 검색 처리           
검색 처리를 위해서는 화면에서 검색 타입type과 키워드keyword를 입력할 수 있는 UI가 필요하지만            
검색 자체가 GET 방식이므로 한글이 아니면 간단히 GET 방식의 쿼리 스트링 query string을 조작해서 테스트가 가능하다.          
예를 들어 프로젝트를 실행한 상태에서 브라우저의 주소창에 /guestbook/list?page=1&type=t&keyword=11 같이 조작하면       
제목 안에 11이라는 문자열이 포함된 글들의 1페이지를 확인할 수 있다.              

목록 화면을 처리하는 list.html에는 검색 타입과 키워드를 입력하고 검색 버튼을 추가해야 한다.      

```html
list.html 일부

<form action="/guestbook/list" method="get" id="searchForm">
    <div class="input-group">
        <input type="hidden" name="page" value="1">
        <div class="input-group-prepend">
            <select class="custom-select" name="type">
                <option th:selected="${pageRequestDTO.type==null}">-----------</option>
                <option value="t" th:selected="${pageRequestDTO.type=='t'}">제목</option>
                <option value="c" th:selected="${pageRequestDTO.type=='c'}">내용</option>
                <option value="w" th:selected="${pageRequestDTO.type=='w'}">작성자</option>
                <option value="tc" th:selected="${pageRequestDTO.type=='tc'}">제목+내용</option>
                <option value="tcw" th:selected="${pageRequestDTO.type=='tcw'}">제목+내용+작성자</option>
            </select>
        </div>
        <input type="form-control" name="keyword" th:value="${pageRequestDTO.keyword}">
        <div class="input-group-append" id="button-addon4">
            <button class="btn btn-outline-secondary btn-search" type="button">Search</button>
            <button class="btn btn-outline-secondary btn-clear" type="button">Clear</button>
        </div>
    </div>
</form>
```

list.html에는 새롭게 하나의 < form > 태그와 < select > 태그 등이 추가되었다.       
< select > 태그는 검색 타입을 선택하는 용도로 사용하는데 PageRequestDTO를 이용해서 검색 타입에 맞게 자동으로 선택될 수 있도록 구성한다.        
키워드는 < input > 태그로 처리한다.       

추가된 < form > 태그 안에는 class 속성값으로 btn-search를 가지는 Search 버튼과 btn-clear의 Clear 버튼이 있고,       
< form > 태그의 바로 아래쪽에 < input type='hidden' > 으로 처리된 page 값이 1로 처리되어 있다.         
이는 Search 버튼을 누르는 것은 새롭게 검색을 진행하는 것이므로 1페이지를 지정하도록 하는 것이다.         

Clear버튼을 클릭하면 모든 검색 조건 없이 새로 목록 페이지를 보는 것을 의미한다.         
list.html의 아래 쪽에는 이벤트 처리를 다음과 같이 추가한다.     

```html
list.html 일부

var searchForm = $("#searchForm");

$('.btn-search').click(function(e){
    searchForm.submit();
});

$('.btn-clear').click(function(e){
    searchForm.empty().submit();
});
```

btn-search를 클릭하면 새롭게 선택된 검색 타입과 키워드로 1페이지를 검색하고.       
btn-clear를 클릭하면 모든 검색과 관련된 내용을 삭제하고 검색이 없는 목록 페이지를 호출한다.        

###### 페이지 번호의 검색 조건 추가         

목록 페이지 하단의 검색은 단순히 page라는 값만을 처리하므로, 검색 타입과 키워드를 추가해 주어야 한다.        

```html
    <ul class="pagination h-100 justify-content-center align-items-center">
      <li class="page-item" th:if="${result.prev}">
        <a class="page-link"th:href="@{/guestbook/list(page=${result.start-1}, type=${pageRequestDTO.type},
                                      keyword = ${pageRequestDTO.keyword})}" tabindex="-1" >Previous</a>
      </li>
      <li th:class="'page-item'+${result.page == page? 'active' : ''}" th:each="page:${result.pageList}">
        <a class="page-link" th:href="@{/guestbook/list(page=${page},
                                      type=${pageRequestDTO.type},
                                      keyword=${pageRequestDTO.keyword})}">
          [[${page}]]
        </a>
      </li>
      <li class="page-item" th:if="${result.next}">
        <a class="page-link" th:href="@{/guestbook/list(page=${result.end+1}, 
                                        type=${pageRequestDTO.type}, 
                                        keyword=${pageRequestDTO.keyword})}">Next</a>
      </li>
    </ul>
```

코드가 복잡해 보이지만 실제로는 기존에 @{/guestbook/list(page=...)}와 같이 처리된 부분에 type과 keyword가 추가된 것 뿐이다.        

###### 조회 페이지로 이동하는 검색 처리

목록 페이지에서 마지막으로 처리해야 하느 내용은 특정 글의 번호를 클릭해서 이동하는 부분이다.          
이는 페이지 처리와 동일하게 type과 keyword 항목을 추가하면 된다.       

```html
list.html 일부

<tr th:each="dto : ${result.dtoList}">
    <th scope="row">
        <a th:href="@{/guestbook/read(gno=${dto.gno}, page=${result.page}, 
                          type=${pageRequestDTO.type}, keyword=${pageRequestDTO.keyword)}">
            [[${dto.gno}]]
        </a>
    </th>

```

#### 조회 페이지 검색 처리

기존의 조회 페이지는 page 값만 처리했기 때문에 다시 목록으로 돌아가는 링크 앞에서 처리한 것과 동일하게 type과 keyword 값을 추가해 주어야 한다.     
조회 페이지는 PageRequestDTO를 컨트롤러에서 @ModelAttribute를 이용해서 requestDTO라는 이름으로 처리하고 있다.        

```html
read.html 일부

<a th:href="@{/guestbook/modify(gno=${dto.gno}, page=${requestDTO.page},
                        type=${requestDTO.type}, keyword=${requestDTO.keyword})}">
    <button type="button" class="btn btn-primary">Modify</button>
</a>
<a th:href="@{/guestbook/list(page=${requestDTO.page},
                        type=${requestDTO.type}, keyword=${requestDTO.keyword})}">
    <button type="button" class="btn btn-info">List</button>
</a>
```

조회 페이지의 검색 처리는 다음과 같은 순서로 확인할 수 있다.          
- 목록 페이지에서 특정한 조건으로 검색을 수행        
- 검색한 상태에서 특정 글을 선택해서 조회 페이지로 이동        
- 조회 페이지에서 목록 페이지로 이동하는 버튼을 클릭해서 이동         

#### 수정 작업 후 이동 처리        

GuestbookController는 작업이 끝난 후에 RedirectAttribute를 이용해서 이동하는 경우가 있다.       

- 등록 처리: 1페이지로 이동     
- 삭제 처리: 1페이지로 이동       
- 수정 처리: 조회 페이지로 이동        

수정은 다시 조회 페이지로 이동하기 때문에 검색 조건을 같이 유지해야 한다.      
조회 페이지에서는 다음과 같이 page뿐 아니라 type과 keyword를 처리해야 한다.       

```html
modify.html 일부

<form action="/guestbook/modify" method="post">
    <input type="hidden" name="page" th:value="${requestDTO.page}">
    <input type="hidden" name="type" th:value="${requestDTO.type}">
    <input type="hidden" name="keyword" th:value="${requestDTO.keyword}">
```

기존의 코드에 type과 keyword 부분이 추가된 것을 볼 수 있다.      
수정 페이지에서 다시 목록 페이지로 이동하는 경우에도 type과 keyword가 같이 전달될 수 있도록 이벤트 처리 부분도 수정이 필요하다.        

```html
modify.html 일부

$(".listBtn").click(function(){
//var pageInfo = $("input[name='page']");

var page = $("input[name='page']");
var type = $("input[name='type']");
var keyword = $("input[name='keyword']");

actionForm.empty(); //form 태그의 모든 내용을 지우고

//actionForm.append(pageInfo);    //목록 페이지 이동에 필요한 내용을 다시 추가
actionForm.append(page);
actionForm.append(type);
actionForm.append(keyword);

actionForm.attr("action", "/guestbook/list")
.attr("method", "get");

console.log(actionForm.html());
actionForm.submit();
});


```

수정된 부분은 page뿐 아니라 type과 keyword 부분도 같이 보관했다가 < form > 태그와 같이 전송하도록 수정한 것이다.        
수정 작업의 마지막은 GuestbookController에서 수정한 후에 조회 페이지로 리다이렉트 처리될 때 검색 조건을 유지하도록 추가해 주는 것이다.        

```java
GuestbookController 일부

@PostMapping("/modify")
public String modify(GuestbookDTO dto, @ModelAttribute("requestDTO") PageRequestDTO requestDTO,
        RedirectAttributes redirectAttributes){
        log.info("post modify...............");
        log.info("dto: "+dto);

        service.modify(dto);

        redirectAttributes.addAttribute("page", requestDTO.getPage());
        redirectAttributes.addAttribute("type", requestDTO.getType());
        redirectAttributes.addAttribute("keyword", requestDTO.getKeyword());
        redirectAttributes.addAttribute("gno", dto.getGno());

        return "redirect:/guestbook/read";
        }
```

최종적인 실행 결과는 수정한 후에 다시 원래의 정보를 그대로 유지하는 조회 페이지로 이동하는 것이다.      
