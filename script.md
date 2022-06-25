# N:1(다대일) 연관관계         

실제로 서비스되는 웹 어플리케이션에서 하나의 엔티티 타입만을 이용해서 화면이 구성되는 경우는 많지 않다.         
대부분은 여러 정보가 함께 가공되어서 처리되는데 SQL을 이용할 때와 달리 JPA를 이용하는 경우에는 이러한 처리가 복잡하다.            

여기서는 회원과 게시글 그리고 댓글이라는 주제로 JPA에서 연관관계를 가장 쉽게 적용할 수 있는 방법을 알아본다.

- 연관관계를 분석하고 @ManyToOne을 이용한 연관관계 설정하는 법          
- 연관관계가 없는 상황에서 left (outer) join 처리 방법         
- 즉시(Eager) 로딩와 지연(Lazy) 로딩의 차이와 효율적인 처리 방법         

## 연관관계와 관계형 데이터베이스 설계         
관계형 데이터베이스에서는 개체(entity)간의 관계relation라는 것에 대해 고민하게 된다.        
관계형 데이터베이스에서는 1:1, 1:N, M:N의 관계를 이용해서 데이터가 서로간에 어떻게 구성되었는지를 표현한다.         
이 표현에서 가장 중요한 것이 PK와 FK를 어떻게 설정해서 사용하는가에 대한 설정이다.         

예를 들어 회원과 게시글의 관계를 보면 다음과 같은 명제를 생각해 볼 수 있다.            
- 한 명의 회원은 여러 게시글을 작성할 수 있다.         
- 하나의 게시글은 한 명의 회원에 의해서 작성된다.          

위 두 문장 중 어느 하나도 틀린 것은 아니지만, 실제로 데이터베이스 모델링을 할 때는 상황이 다르다.       
두 번째 문장을 보면 마치 일대일의 관계처럼 보일 수 있지만 실제 테이블 설계에서는 다르다.      
이를 가장 쉽게 확인할 수 있는 방법은 가상의 데이터를 직접 만들어 보는 것이다.         

회원 데이터의 아이디는 PK에 해당되고 아이디는 회원을 구분할 수 있는 고유한 값을 가지게 된다.            
게시글 데이터에서는 작성자 칼럼의 값으로 동일한 회원의 아이디가 여러 번 나올 수 있다.        
회원 데이터의 입장에서는 하나의 PK가 여러 게시글에서 참조FK 되고 있는 관계가 된다.          

하나의 게시글은 한 명의 회원에 의해서 작성된다 라는 문장은 게시글 데이터에 작성자 칼럼이 하나만 필요하다는 뜻일 뿐이다.         
테이블 간의 관계는 특정한 PK가 다른 곳에서 몇 번 FK로 사용되었는지가 중요하다.        
우선 어떠한 PK를 기준으로 할 것인지를 고민하고, 해당 PK가 다른 곳에서 몇 번 사용되었는지를 세어보는 방식으로 찾아낸다.          

#### PK로 설계, FK로 연관관계 해석        
JPA를 이용해서 연관관계를 해석할 때는 PK를 기준으로 잡고, 데이터베이스를 모델링하는 방식으로 구성한다.            

회원이 있어야만 게시글을 작성할 수 있으므로, 회원 테이블을 먼저 설계하고 게시글을 작성할 때는 특정 회원과의 관계를 설정해 주어야 한다.         
댓글은 게시글이 있어야만 작성할 수 있으므로 게시글을 우선 설계하고, 댓글 테이블이 게시글을 FK로 사용한다.         

FK를 기준으로 위 관계를 해석하면 다음과 같다.       
- 게시물은 회원과 다대일 관계이다.         
- 댓글은 게시물과 다대일 관계이다.        

JPA는 객체지향의 입장에서 이들의 관계를 보기 때문에 데이터베이스와 달리 다음과 같은 선택이 가능하다.          
- 회원 엔티티가 게시물 엔티티들을 참조하게 설정해야 하는가?          
- 게시물 엔티티에서 회원 엔티티를 참조하게 설정해야 하는가?           
- 회원, 게시물 엔티티 객체 양쪽에서 서로를 참조하게 설정해야 하는가?          

관계형 데이터베이스에서는 PK와 FK만으로 표현되었던 관계가 객체지향으로 옮겨지면 위와 같이 다양한 선택지가 존재하게 된다.         
흔히 단방향 참조, 양방향 참조라고 표현하기도 하는데 실제 데이터베이스에서는 양방향이라는 말이 존재하지 않기 때문에 객체지향에서만 겪는 문제라고 볼 수 있다.      

위의 3가지 선택지에서 가장 간단한 시작은 객체지향보다는 관계형 데이터베이스 모델링을 위주로 해서 구성하는 것이 편리하다.            
FK를 사용하는 엔티티가 PK를 가진 엔티티를 참조하는 구조로 설계하면 데이터베이스와 동일한 구조가 되기 때문에           
관계를 이해하는 것도 편하고 자동으로 테이블이 생성될 때도 유용하다.         

###### 예제 프로젝트 생성        

생성된 프로젝트에 MariaDB JDBC 드라이버와 Thymeleaf의 시간 처리 관련 라이브러리를 추가한다.       


###### 날짜/시간 처리 설정         

프로젝트 내에 entity 패키지를 생성하고, 이전 예제에서 사용했던 BaseEntity 클래스를 추가한다.      

```java

package org.zerock.board.entity;

@MappedSuperclass
@EntityListeners(value={AuditingEntityListener.class})
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(name="regDate", updatable = false)
    private LocalDateTime regDate;

    @LastModifiedDate
    @Column(name="modDate")
    private LocalDateTime modDate;
}
```

자동 시간 처리를 위하여 프로젝트와 같이 생성된 BoardApplication에는 @EnableJpaAuditing을 추가한다.     

```java
package org.zerock.board;

@SpringBootApplication
@EnableJpaAuditing
public class BoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardApplication.class, args);
	}

}

```

#### 엔티티 클래스 추가       

연관관계의 설정은 처음부터 설정하는 방식보다는 엔티티 클래스들을 구성한 이후에 각 엔티티 클래스의 연관관계를 설정하는 것이 더 수월하다.          
entity 패키지에 Member, Board, Reply 엔티티 클래스를 추가한다.        

###### Member 엔티티 클래스          
회원 엔티티 클래스는 최근에 많이 사용하는 이메일을 사용자의 아이디 대신에 사용할 수 있도록 설정한다.         

```java
package org.zerock.board.entity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Member extends BaseEntity{
    @Id
    private String email;

    private String password;

    private String name;
}
```

Member 클래스는 이메일 주소를 PK로 이용한다.       
데이터베이스 설계에서도 member 테이블은 PK만을 가지고 있고, FK를 사용하지 않으므로 별도의 참조가 필요하지 않다.          

###### Board 클래스
Board 클래스는 Member의 이메일PK을 참조하는 구조이다.        
초기의 설정에서는 우선 연관관계를 작성하지 않고 순수하게 작성한다.           
나중에 회원과의 연관관계를 고려해서 작성자에 해당하는 필드는 작성하지 않는다.        

```java
package org.zerock.board.entity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Board extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    private String title;

    private String content;

    //작성자는 아직 처리하지 않음.
}
```

###### Reply 클래스        
Reply 클래스는 회원이 아닌 사람도 댓글을 남길 수 있다고 가정하고 Board와 연관관계를 맺지 않은 상태로 처리한다.        

```java
package org.zerock.board.entity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Builder
public class Reply extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;
    
    private String text;
    
    private String replyer;
    
    //Board와 연관관계는 아직 작성하지 않음.
}
```

#### @ManyToOne 어노테이션        
데이터베이스 구조로 보면 앞으로 생성될 board 테이블과 member 테이블에는 FK를 이용한 참조가 걸려 있게 된다.           
member의 email을 board에서는 FK로 참조하는 구조이다.       

JPA에서 관계를 고민할 때는 FK쪽을 먼저 해석해 보면 편리하므로, FK를 사용하는 board쪽의 관계를 우선 살펴본다.           
이 경우 board와 member 관계는 N:1(다대일)의 관계가 되므로 JPA에서는 이를 의미하는 @ManyToOne을 적용해야 한다.           
@ManyToOne은 데이터베이스상에서 외래키의 관계로 연결된 엔티티 클래스에 설정한다.             
Board 클래스는 작성자가 Member 엔티티를 의미하므로 아래와 같이 참조하는 부분을 작성한다.            

```java
package org.zerock.board.entity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "writer")//@ToString은 항상 exclude
public class Board extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    private String title;

    private String content;

    @ManyToOne
    private Member writer;  //연관관계 지정
}
```

이와 동일하게 Reply에는 Board의 PK를 참조해서 구성되어야 하므로 아래와 같이 수정한다.     

```java
package org.zerock.board.entity;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Builder
public class Reply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    private String text;

    private String replyer;

    @ManyToOne
    private Board board;    //연관관계 지정
}
```

위의 변경 내용을 저장하고 프로젝트를 실행해서 생성되는 DDL을 살펴보겠다.          
프로젝트의 실행 시에 아래와 같은 DDL이 처리되는 것을 볼 수 있다.        

```
Hibernate: 
    
    create table board (
       bno bigint not null auto_increment,
        mod_date datetime(6),
        reg_date datetime(6),
        content varchar(255),
        title varchar(255),
        writer_email varchar(255),
        primary key (bno)
    ) engine=InnoDB
Hibernate: 
    
    create table member (
       email varchar(255) not null,
        mod_date datetime(6),
        reg_date datetime(6),
        name varchar(255),
        password varchar(255),
        primary key (email)
    ) engine=InnoDB
Hibernate: 
    
    create table reply (
       rno bigint not null auto_increment,
        mod_date datetime(6),
        reg_date datetime(6),
        replyer varchar(255),
        text varchar(255),
        board_bno bigint,
        primary key (rno)
    ) engine=InnoDB
Hibernate: 
    
    alter table board 
       add constraint FK1iu8rhoim4thb0y12cpt01oiu 
       foreign key (writer_email) 
       references member (email)
Hibernate: 
    
    alter table reply 
       add constraint FKr1bmblqir7dalmh47ngwo7mcs 
       foreign key (board_bno) 
       references board (bno)

```

자동으로 생성되는 외래키의 이름은 다르지만 계획과 같은 데이터베이스의 관계가 만들어지는 것을 확인할 수 있다.        

#### Repository 인터페이스 추가        
테이블이 정상적으로 생성되었다면 각 엔티티에 맞는 Repository 인터페이스를 추가한다.

```java
package org.zerock.board.repository;

public interface MemberRepository extends JpaRepository<Member, String> {
}


package org.zerock.board.repository;

public interface BoardRepository extends JpaRepository<Board, Long> {
}


package org.zerock.board.repository;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
}
```

## 연관관계 테스트        
현재 3개의 테이블이 PK와 FK의 관계로 이루어져 있기 때문에 테스트를 위한 데이터를 추가하는 작업도 PK 쪽에서부터 시작하는 것이 좋다.          
프로젝트 내에 test 폴더에 repository 폴더를 생성하고 각 엔티티 클래스를 다루는              
MemberRepositoryTests/BoardRepositoryTests/ReplyRepositoryTests 클래스를 작성한다.      

#### 테스트 데이터 추가하기       
작성하는 MemberRepositoryTests에는 MemberRepository를 주입하고 예제로 사용할 Member 객체를 100개 추가하는 테스트 코드를 작성한다.       

```java
package org.zerock.board.repository;

@SpringBootTest
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void insertMembers(){
        IntStream.rangeClosed(1,100).forEach(i->{
            Member member = Member.builder()
                    .email("user"+i+"@aaa.com")
                    .password("1111")
                    .name("USER"+i)
                    .build();

            memberRepository.save(member);
        });
    }
}
```

BoardRepositoryTests 코드 역시 동일한 패키지에 추가하고 앞에서 만들어진 회원 데이터를 이용해서 Board 객체를 생성해 추가하도록 테스트 코드를 작성한다.      

```java
package org.zerock.board.repository;

@SpringBootTest
public class BoardRepositoryTests {
    @Autowired
    private BoardRepository boardRepository;

    @Test
    public void insertBoard(){
        IntStream.rangeClosed(1,100).forEach(i->{
            Member member = Member.builder().email("user"+i+"@aaa.com").build();

            Board board = Board.builder()
                    .title("Title..."+i)
                    .content("Content..."+i)
                    .writer(member)
                    .build();
            
            boardRepository.save(board);
        });
    }
}
```

testInsert()는 한 명의 사용자가 하나의 게시물을 등록하도록 작성되었다.        

댓글은 ReplyRepositoryTests 클래스를 작성해서 특정한 임의의 게시글을 대상으로 댓글을 추가한다.       
현재 게시글은 1번부터 100번까지의 임의의 번호를 이용해서 300개의 댓글을 추가한다.       

```java
package org.zerock.board.repository;

@SpringBootTest
public class ReplyRepositoryTests {

    @Autowired
    private ReplyRepository replyRepository;

    @Test
    public void insertReply(){
        IntStream.rangeClosed(1,300).forEach(i->{
            long bno = (long)(Math.random()*100)+1; //1부터 100까지 무작위 번호

            Board board = Board.builder().bno(bno).build();

            Reply reply = Reply.builder()
                    .text("Reply..."+i)
                    .board(board)
                    .replyer("guest")
                    .build();

            replyRepository.save(reply);
        });
    }
}
```

insertReply()는 300개의 댓글을 1~100 사이의 번호로 추가한다.      
데이터베이스에는 1부터 100까지 게시물에 대해서 n개의 댓글이 추가된다.       

###### 필요한 쿼리 기능 정리하기      
테스트에 사용할 더미데이터를 추가하였다면 다음 단계는 현재 존재하는 데이터를 이용해서 화면에 어떤 내용을 출력하고 싶은가를 정리해야 한다.     

현재 화면과 필요한 데이터를 정리해서 다음과 같이 정리하였다.      
- 목록 화면: 게시글의 번호, 제목, 댓글 개수, 작성자의 이름/이메일           
- 조회 화면: 게시글의 번호, 제목, 내용, 댓글 개수, 작성자 이름/이메일       

#### @ManyToOne과 Eager/Lazy loading         

두 개 이상의 엔티티 간의 연관관계를 맺고 나면 쿼리를 실행하는 데이터베이스 입장에서는 한 가지 고민이 생긴다.         
그것은 엔티티 클래스들이 실제 데이터베이스상에서는 두 개 혹은 두 개 이상의 테이블로 생성되기 떄문에 연관관계를 맺고 있다는 것은           
데이터베이스의 입장으로 보면 조인이 필요하다는 것이다.   
실제로 @ManyToOne의 경우에는 FK 쪽의 엔티티를 가져올 때 PK 쪽의 엔티티도 같이 가져온다.          

BoardRepositoryTests를 통해 Member를 @ManyToOne으로 참조하고 있는 Board를 조회하는 테스트 코드를 작성한다.       

```java
BoardRepositoryTests 일부

@Test
public void testRead1(){
        Optional<Board> result = boardRepository.findById(100L);

        Board board = result.get();

        System.out.println(board);
        System.out.println(board.getWriter());
}
```

위의 코드를 실행하면 쿼리가 내부적으로 left outer join 처리된 것을 확인할 수 있다.       

```
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.email as email1_1_1_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    where
        board0_.bno=?
Board(bno=100, title=Title...100, content=Content...100)
Member(email=user100@aaa.com, password=1111, name=USER100)
```

Reply와 Board 역시 @ManyToOne의 관계이므로 테스트를 하면 자동으로 조인이 처리되는 것을 확인할 수 있다.       

```java
ReplyRepositoryTests 일부

@Test
public void readReply1(){

        Optional<Reply> result = replyRepository.findById(1L);

        Reply reply = result.get();

        System.out.println(reply);
        System.out.println(reply.getBoard());
        }
```

위 코드를 실행하면 다음과 같이 조인이 처리된 쿼리가 실행되는 것을 볼 수 있다.    

```
Hibernate: 
    select
        reply0_.rno as rno1_2_0_,
        reply0_.mod_date as mod_date2_2_0_,
        reply0_.reg_date as reg_date3_2_0_,
        reply0_.board_bno as board_bn6_2_0_,
        reply0_.replyer as replyer4_2_0_,
        reply0_.text as text5_2_0_,
        board1_.bno as bno1_0_1_,
        board1_.mod_date as mod_date2_0_1_,
        board1_.reg_date as reg_date3_0_1_,
        board1_.content as content4_0_1_,
        board1_.title as title5_0_1_,
        board1_.writer_email as writer_e6_0_1_,
        member2_.email as email1_1_2_,
        member2_.mod_date as mod_date2_1_2_,
        member2_.reg_date as reg_date3_1_2_,
        member2_.name as name4_1_2_,
        member2_.password as password5_1_2_ 
    from
        reply reply0_ 
    left outer join
        board board1_ 
            on reply0_.board_bno=board1_.bno 
    left outer join
        member member2_ 
            on board1_.writer_email=member2_.email 
    where
        reply0_.rno=?
Reply(rno=1, text=Reply...1, replyer=guest, board=Board(bno=39, title=Title...39, content=Content...39))
Board(bno=39, title=Title...39, content=Content...39)
```

실행된 SQL을 보면 reply 테이블, board 테이블, member 테이블까지 모두 조인으로 처리되는 것을 볼 수 있다.         
Reply를 가져올 때 매번 Board와 Member까지 조인해서 가져올 필요가 많지는 않으므로         
위와 같이 여러 테이블이 조인으로 처리되는 상황은 그다지 효율적이지 않다.       

###### fetch는 Lazy loading을 권장
위의 쿼리 실행 결과와 같이 특정한 엔티티를 조회할 때 연관관계를 가진 모든 엔티티를 같이 로딩하는 것을 Eager loading이라고 한다.        
Eager는 열렬한 혹은 적극적인이라는 말로, 일발적으로 즉시 로딩이라는 용어로 표현한다.          

즉시 로딩은 한 번에 연관관계가 있는 모든 엔티티를 가져온다는 장점이 있지만,         
여러 연관관계를 맺고 있거나 연관관계가 복잡할수록 조인으로 인한 성능 저하를 피할 수 없다.       
JPA에서 연관관계의 데이터를 어떻게 가져올 것인가를 fetch라고 하는데 연관관계의 어노테이션의 속성으로 fetch모드를 지정한다.    

즉시 로딩은 불필요한 조인까지 처리해야 하는 경우가 많기 때문에 가능하면 사용하지 않고,        
그와 반대되는 개념으로 Lazy loading으로 처리한다.           
Lazy loading은 지연 로딩, 게으른 로딩이라고 표현하는데 여기서는 지연로딩이라는 표현을 사용한다.          
지연 로딩의 효과를 알아보기 위해서 Board 클래스를 아래와 같이 수정한다.          

```java
package org.zerock.board.entity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "writer")//@ToString은 항상 exclude
public class Board extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    private String title;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)  //명시적으로 Lazy 로딩 지정
    private Member writer;  //연관관계 지정
}
```

Board 클래스에서 수정된 부분은 @ManyToOne 어노테이션에 fetch 속성을 명시하고 FetchType.LAZY라는 지연 로딩을 적용한 부분이다.      
위와 같은 코드를 수정한 후에 BoardRepositoryTests의 testRead1()을 실행하면 이전 실행 결과와 달리 예외가 발생한다.           

발생한 예외의 메시지는 'could not initialize proxy [org.zerock.board.entity.Member#user100@aaa.com] - no Session' 이다.    
이 메시지는 데이터베이스와 추가적인 연결이 필요하다는 것이다.              

지연 로딩 방식으로 로딩하기 때문에 board 테이블만을 가져와서 System.out.println()을 하는 것은 문제가 없지만 board.getWriter()에서 문제가 발생한다.                      
board.getWriter()는 member 테이블을 로딩해야 하는데 이미 데이터베이스와의 연결은 끝난 상태이기 때문이다.         

no.session 이라는 메시지는 이런 경우에 발생한다.          
이 문제를 해결하기 위해서는 다시 한 번 데이터베이스와 연결이 필요한데 @Transactional이 바로 이러한 처리에 도움을 준다.       
메서드 선언부에 @Transactional을 추가한다.         

```java
@Transactional
@Test
public void testRead1(){
Optional<Board> result = boardRepository.findById(100L);

        Board board = result.get();

        System.out.println(board);
        System.out.println(board.getWriter());
    }
```

@Transactional은 해당 메서드를 하나의 트랜잭션으로 처리하라는 의미이다.      
트랜잭션으로 처리하면 속성에 따라 다르게 동작하지만, 기본적으로는 필요할 때 다시 데이터베이스와 연결이 생성된다.            
위 코드 역시 다음과 같은 실행 결과를 만들어 낸다.       

```
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_ 
    from
        board board0_ 
    where
        board0_.bno=?
Board(bno=100, title=Title...100, content=Content...100)
Hibernate: 
    select
        member0_.email as email1_1_0_,
        member0_.mod_date as mod_date2_1_0_,
        member0_.reg_date as reg_date3_1_0_,
        member0_.name as name4_1_0_,
        member0_.password as password5_1_0_ 
    from
        member member0_ 
    where
        member0_.email=?
Member(email=user100@aaa.com, password=1111, name=USER100)
```

실행 결과를 보면 처음에는 board 테이블만을 로딩해서 처리하고 있지만, board, getWriter()를 하기 위해서 member 테이블을 로딩하는 것을 볼 수 있다.        
지연 로딩을 사용하지 않았을 경우에는 자동으로 board 테이블과 member 테이블이 조인으로 처리된 것과는 차이가 있는 것을 알 수 있다.        

###### 연관관계에서는 @ToString()을 주의         
엔티티 간의 연관관계를 지정하는 경우에는 항상 @ToString()을 주의해야 한다.           
@ToString()은 해당 클래스의 모든 멤버 변수를 출력하게 된다.      
예를 들어 Board 객체의 @ToString()을 하면 writer 변수로 선언된 Member 객체 역시 출력해야 한다.      
Member를 출력하기 위해서는 Member 객체의 toString()이 호출되어야 하고 이때 데이터베이스 연결이 필요하게 된다.          

이런 문제로 인해 연관관계가 있는 엔티티 클래스의 경우 @ToString()을 할 때는 습관적으로 exclude 속성을 사용하는 것이 좋다.         
exclude는 해당 속성 값으로 지정된 변수는 toString()에서 제외하기 때문에 지연 로딩을 할 때는 반드시 지정해 주는 것이 좋다.            

###### 지연 로딩 lazy loading의 장/단점      
지연 로딩은 조인을 하지 않기 때문에 단순하게 하나의 테이블을 이용하는 경우에는 빠른 속도의 처리가 가능하다는 장점이 있다.          
반면에 위와 같이 필요한 순간에 쿼리를 실행해야 하기 때문에 연관관계가 복잡한 경우에는 여러 번의 쿼리가 실행된다는 단점이 있다.          

따라서 보편적인 코딩 가이드는 '지연 로딩을 기본으로 사용하고, 상황에 맞게 필요한 방법을 찾는다' 이다.         

#### JPQL과 left (outer) join           
목록 화면에서 게시글의 정보와 함께 댓글의 수를 같이 가져오기 위해서는 단순히 하나의 엔티티 타입을 이용할 수 없다.           
이에 대한 해결책 중에서 가장 많이 쓰이는 방식은 JPQL의 조인을 이용해서 처리하는 방식이다.         

###### left(outer)join        
스프링 부트 2버전 이후에 포함되는 JPA 버전은 엔티티 클래스 내에 전혀 연관관계가 없더라도 조인을 이용할 수 있다.           
조인을 할 때 INNER JOIN 혹은 JOIN과 같이 일반적인 조인을 이용할 수도 있고, LEFT OUTER JOIN 혹은 LEFT JOIN을 이용할 수 있다.          

###### 엔티티 클래스 내부에 연관관계가 있는 경우        
Board 엔티티 클래스의 내부에는 Member 엔티티 클래스를 변수로 선언하고, 연관관계를 맺고 있다.             
이러한 경우에는 Board의 writer 변수를 이용해서 조인을 처리한다.        

```java
BoardRepository 인터페이스

public interface BoardRepository extends JpaRepository<Board, Long> {
    //한개의 로우(Object) 내에 Object[]로 나옴

    @Query("select b, w from Board b left join b.writer w where b.bno = :bno")
    Object getBoardWithWriter(@Param("bno") Long bno);
}
```

getBoardWithWriter()는 Board를 사용하고 있지만, Member를 같이 조회해야 하는 상황이다.          
Board 클래스에는 Member와의 연관관계를 맺고 있으므로 b.writer와 같은 형태로 사용한다.            
이처럼 내부에 있는 엔티티를 이용할 때는 LEFT JOIN 뒤에 ON을 이용하는 부분이 없다. 작성한 getBoardWithWriter()를 테스트 코드로 확인한다.         

```java
BoardRepositoryTests 테스트 코드

@Test
public void testReadWithWriter(){
        Object result = boardRepository.getBoardWithWriter(100L);

        Object[] arr = (Object[])result;

        System.out.println("---------------------------------------");
        System.out.println(Arrays.toString(arr));
}
```

테스트 코드의 실행 결과를 보면 지연 로딩lazy loading으로 처리되었으나 실행되는 쿼리를 보면            
조인 처리가 되어 한 번에 board 테이블과 member 테이블을 이용하는 것을 확인할 수 있다.            

```
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    where
        board0_.bno=?
---------------------------------------
[Board(bno=100, title=Title...100, content=Content...100), Member(email=user100@aaa.com, password=1111, name=USER100)]
```

###### 연관관계가 없는 엔티티 조인 처리에는 on            

Board와 Member 사이에는 내부적으로 참조를 통해서 연관관계가 있지만, Board와 Reply는 좀 상황이 다르다.         
Reply 쪽이 @ManyToOne으로 참조하고 있으나 Board 입장에서는 Reply 객체들을 참조하고 있지 않기 때문에 문제가 된다.         
이런 경우에는 직접 조인에 필요한 조건은 on을 이용해서 작성해 주어야 한다.          

'특정 게시물과 해당 게시물에 속한 댓글들을 조회'해야 하는 상황을 생각하면 board와 reply 테이블을 조인해서 쿼리를 작성하게 된다.         
예를 들어 현재 데이터베이스 내에 100번 게시물이 2개의 댓글을 가지고 있는 상황이고, 이를 순수한 SQL로 처리한다면 다음과 같이 작성된다.      

```
select
    board.bno, board.title, board.writer_email,
    rno, text
from board left outer join reply
on reply.board_bno = board.bno
where board.bno - 100;
```

위의 쿼리를 JPQL로 처리하면 다음과 같은 방식으로 작성할 수 있다.       

```java
BoardRepository 일부

    @Query("SELECT b, r FROM Board b LEFT JOIN Reply r on r.board=b WHERE b.bno = :bno")
    List<Object[]> getBoardWithReply(@Param("bno") Long bno);
```

연관관계가 있는 경우와 비교해 보면 중간에 on이 사용되면서 조인 조건을 직접 지정하는 부분이 추가되는 것을 볼 수 있다.          
BoardRepositoryTests 클래스에 테스트 코드를 작성하면 다음과 같다.      

```java
    @Test
    public void testGetBoardWithReply(){
        List<Object[]> result = boardRepository.getBoardWithReply(100L);

        for(Object[] arr: result){
            System.out.println(Arrays.toString(arr));
        }
    }
```

위 코드를 실행하면 다음과 같은 쿼리가 실행된다.      

```
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        reply1_.rno as rno1_2_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        reply1_.mod_date as mod_date2_2_1_,
        reply1_.reg_date as reg_date3_2_1_,
        reply1_.board_bno as board_bn6_2_1_,
        reply1_.replyer as replyer4_2_1_,
        reply1_.text as text5_2_1_ 
    from
        board board0_ 
    left outer join
        reply reply1_ 
            on (
                reply1_.board_bno=board0_.bno
            ) 
    where
        board0_.bno=?
[Board(bno=100, title=Title...100, content=Content...100), 
    Reply(rno=46, text=Reply...46, replyer=guest, board=Board(bno=100, title=Title...100, content=Content...100))]
[Board(bno=100, title=Title...100, content=Content...100), 
    Reply(rno=209, text=Reply...209, replyer=guest, board=Board(bno=100, title=Title...100, content=Content...100))]
[Board(bno=100, title=Title...100, content=Content...100), 
    Reply(rno=281, text=Reply...281, replyer=guest, board=Board(bno=100, title=Title...100, content=Content...100))]
[Board(bno=100, title=Title...100, content=Content...100), 
    Reply(rno=299, text=Reply...299, replyer=guest, board=Board(bno=100, title=Title...100, content=Content...100))]
```

###### 목록 화면에 필요한 JPQL 만들기

목록 화면에서 필요한 데이터를 다시 정리해보면 다음과 같다.       
- 게시물 : 게시물의 번호, 제목, 게시물의 작성 시간         
- 회원 : 회원의 이름/이메일         
- 댓글 : 해당 게시물의 댓글 수         

위의 세 개의 엔티티 중에서 가장 많은 데이터를 가져오는 쪽은 Board이므로 Board를 중심으로 조인 관계를 작성한다.         
Member는 Board 내에 writer라는 필드로 연관관계를 맺고 있고, Reply는 연관관계가 없는 상황이다.          
조인 후에는 Board를 기준으로 GROUP BY 처리를 해서 하나의 게시물 당 하나의 라인이 될 수 있도록 처리해야 한다.          
(JPQL을 이용하면 SQL에 사용하는 많은 함수를 적용할 수 있다.)       

BoardRepository에는 Pageable을 파라미터로 전달받고, Page< Object[] > 리턴 타입의 getBoardWithReplyCount()를 아래와 같이 작성한다.       

```java
BoardRepository 인터페이스

    @Query(value="SELECT b, w, count(r) " +
        "FROM Board b " +
        "LEFT JOIN b.writer w " +
        "LEFT JOIN Reply r ON r.board = b " +
        "GROUP BY b",
        countQuery = "SELECT count(b) FROM Board b")
    Page<Object[]> getBoardWithReplyCount(Pageable pageable);
    // 목록 화면에 필요한 데이터
```

BoardRepositoryTests에서 정상적으로 JPQL이 동작 가능한지 확인해 준다.         

```java
    @Test
    public void testWithReplyCount(){
        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<Object[]> result = boardRepository.getBoardWithReplyCount(pageable);

        result.get().forEach(row->{
            Object[] arr = (Object[]) row;
            System.out.println(Arrays.toString(arr));
        });
    }
```
1페이지의 데이터를 처리한다고 가정하고 페이지 번호는 0으로 지정하고, 10개를 조회한다.        
위의 테스트 코드의 실행 결과로 발생하는 쿼리와 결과는 아래와 같다.       

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    group by
        board0_.bno 
    order by
        board0_.bno desc limit ?
Hibernate: 
    select
        count(board0_.bno) as col_0_0_ 
    from
        board board0_
[Board(bno=100, title=Title...100, content=Content...100), Member(email=user100@aaa.com, password=1111, name=USER100), 4]
[Board(bno=99, title=Title...99, content=Content...99), Member(email=user99@aaa.com, password=1111, name=USER99), 1]
[Board(bno=98, title=Title...98, content=Content...98), Member(email=user98@aaa.com, password=1111, name=USER98), 2]
[Board(bno=97, title=Title...97, content=Content...97), Member(email=user97@aaa.com, password=1111, name=USER97), 1]
[Board(bno=96, title=Title...96, content=Content...96), Member(email=user96@aaa.com, password=1111, name=USER96), 5]
[Board(bno=95, title=Title...95, content=Content...95), Member(email=user95@aaa.com, password=1111, name=USER95), 1]
[Board(bno=94, title=Title...94, content=Content...94), Member(email=user94@aaa.com, password=1111, name=USER94), 1]
[Board(bno=93, title=Title...93, content=Content...93), Member(email=user93@aaa.com, password=1111, name=USER93), 3]
[Board(bno=92, title=Title...92, content=Content...92), Member(email=user92@aaa.com, password=1111, name=USER92), 4]
[Board(bno=91, title=Title...91, content=Content...91), Member(email=user91@aaa.com, password=1111, name=USER91), 3]
```

###### 조회 화면에서 필요한 JPQL 구성하기        

조회 화면에서 Board와 Member를 주로 이용하고, 해당 게시물이 몇 개의 댓글이 있는지를 알려주는 수준으로 작성한다.         
실제 댓글은 화면에서 주로 Ajax를 이용해서 필요한 순간에 동적으로 데이터를 가져오는 방식이 일반적이다.          
작성하는 JPQL은 목록 화면과 유사하게 다음과 같은 형태가 된다.

```java
BoardRepository 인터페이스 일부

    @Query(value = "SELECT b, w, count(r) " +
        "FROM Board b LEFT JOIN b.writer w " +
        "LEFT OUTER JOIN Reply r on r.board=b " +
        "WHERE b.bno = :bno")
    Object getBoardByBno(@Param("bno") Long bno);
```

BoardRepository에 추가된 getBoardByBno()는 목록 처리와 유사하지만 특정한 게시물 번호를 사용하는 부분이 다르다.         
테스트 코드는 다음과 같이 작성한다.       

```java
    @Test
    public void testRead3(){
        Object result = boardRepository.getBoardByBno(100L);

        Object[] arr = (Object[]) result;

        System.out.println(Arrays.toString(arr));
    }
```

실행되는 쿼리의 결과는 다음과 같다.

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    where
        board0_.bno=?
[Board(bno=100, title=Title...100, content=Content...100), Member(email=user100@aaa.com, password=1111, name=USER100), 4]
```

## 프로젝트 적용하기        
Board와 Member, Reply를 사용하는 테스트가 완료되었다면, 기존의 프로젝트 내용을 참고해서 실제로 브라우저에서 확인할 수 있도록 코드를 작성한다.            

#### DTO 계층과 서비스 계층 작성       
프로젝트 내에 dto, service 패키지를 작성하고, dto 패키지에는 BoardDTO 클래스를 생성한다.       

DTO를 구성하는 기준은 기본적으로 화면에 전달하는 데이터이거나,          
반대로 화면 쪽에서 전달되는 데이터를 기준으로 하기 때문에 엔티티 클래스의 구성과 일치하지 않는 경우가 많다.        
작성하는 BoardDTO의 경우 Member에 대한 참조는 구성하지 않고 작성한다.      

```java
package org.zerock.board.dto;

@Data
@AllArgsConstructor
@ToString
@Builder
@NoArgsConstructor
public class BoardDTO {
    private Long bno;
    private String title, content, writerEmail, wrierName;
    private LocalDateTime regDate, modDate;
    private int replyCount;
    
}
```

BoardDTO 클래스가 Board 엔티티 클래스와 다른 점은 Member를 참조하는 대신에 화면에서 필요한 작성자의 이메일(writerEmail)과           
작성자의 이름(writerName)으로 처리하고 있는 점이다.         
목록 화면에서도 BoardDTO를 이용하기 때문에 댓글의 개수를 의미하는 replyCount도 추가한다.       

#### 게시물 등록         
게시물 등록은 BoardDTO 타입을 파라미터로 전달받고, 생성된 게시물의 번호를 반환하도록 작성한다.           
실제 처리 과정에서 BoardDTO를 Board 엔티티 타입으로 변환할 필요가 있는데       
이에 대한 처리는 BoardService 인터페이스에 dtoToEntity()를 작성해서 처리한다.          

```java
package org.zerock.board.service;

public interface BoardService {
    Long register(BoardDTO dto);
    default Board dtoToEntity(BoardDTO dto){
        Member member = Member.builder().email(dto.getWriterEmail()).build();

        Board board = Board.builder()
                .bno(dto.getBno())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(member)
                .build();
        return board;
    }
}
```

dtoToEntity()는 DTO가 연관관계를 가진 Board 엔티티 객체와 Member 엔티티 객체를 구성해야 하므로 내부적으로 Member 엔티티를 처리하는 과정을 거쳐야 한다.       
(이때 Member는 실제 데이터베이스에 있는 이메일 주소를 사용해야 한다.)         
작성된 dtoToEntity()는 실제로 게시물을 등록하는 BoardServiceImpl의 register()에서 사용한다.           

```java
package org.zerock.board.service;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardServiceImpl implements BoardService{
    private final BoardRepository repository;


    @Override
    public Long register(BoardDTO dto) {
        log.info(dto);
        
        Board board = dtoToEntity(dto);
        
        repository.save(board);
        
        return board.getBno();
    }
}
```

test 폴더에는 service 패키지를 구성하고, BoardServiceTests 클래스를 추가해서 작성한 BoardService를 테스트 한다.        
```java
package org.zerock.board.service;

@SpringBootTest
public class BoardServiceTests {
    @Autowired
    private BoardService boardService;

    @Test
    public void testRegister(){
        BoardDTO dto = BoardDTO.builder()
                .title("Test..")
                .content("Test....")
                .writerEmail("user55@aaa.com")
                .build();

        Long bno = boardService.register(dto);
    }
}

```

#### 게시물 목록 처리          
게시물의 목록을 처리하기 위해서는 이전 예제에서 파라미터를 수집하는데      
사용했던 PageRequestDTO와 PageResultDTO를 추가해서 사용해야 한다.          

추가된 두 개의 클래스는 모두 다양한 목록 처리를 위해서 작성된 것이므로          
패키지명을 수정하는 정도를 제외하면 별도의 처리가 필요하지 않다.          

###### Object[]을 DTO로 변환하기        
PageResultDTO의 핵심은 JPQL의 결과로 나오는 Object[]을 DTO 타입으로 변환하는 기능이다.         
이 기능은 java.util.Function을 이용해서 작성하는데 현재 예제의 경우 JPQL의 실행 결과로 나오는 Object[]를 BoardDTO로 처리해 주어야만 한다.        

Object[]의 내용은 Board와 Member, 댓글의 수는 Long 타입으로 나오게 되므로 이를 파라미터로 전달받아 BoardDTO를 구성하도록 작성해야 한다.        
이 기능은 BoardService 인터페이스에 entityToDTO()라는 메서드를 작성해서 처리한다.       

```java
    default BoardDTO entityDTO(Board board, Member member, Long replyCount){
        BoardDTO boardDTO = BoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .writerEmail(member.getEmail())
                .wrierName(member.getName())
                .replyCount(replyCount.intValue())//Long으로 나오므로 int로 형번환
                .build();

        return boardDTO;
    }
```

entityToDTO()는 총 3개의 파라미터를 처리할 수 있도록 구성한다.        
Board 엔티티 객체와 Member 엔티티 객체, 댓글의 수를 파라미터로 전달받도록 구성하고 이를 이용해서 BoardDTO 객체를 생성할 수 있도록 처리한다.         
게시물의 목록 처리를 의미하는 기능은 getList()라는 이름으로 작성한다.          

```java
package org.zerock.board.service;

public interface BoardService {
    Long register(BoardDTO dto);

    PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO);   //목록 처리
    default Board dtoToEntity(BoardDTO dto){
        Member member = Member.builder().email(dto.getWriterEmail()).build();

        Board board = Board.builder()
                .bno(dto.getBno())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(member)
                .build();
        return board;
    }
    default BoardDTO entityToDTO(Board board, Member member, Long replyCount){
        BoardDTO boardDTO = BoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .writerEmail(member.getEmail())
                .wrierName(member.getName())
                .replyCount(replyCount.intValue())//Long으로 나오므로 int로 형번환
                .build();

        return boardDTO;
    }
}



package org.zerock.board.service;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardServiceImpl implements BoardService {
    private final BoardRepository repository;


    @Override
    public Long register(BoardDTO dto) {
        log.info(dto);

        Board board = dtoToEntity(dto);

        repository.save(board);

        return board.getBno();
    }

    @Override
    public PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO) {
        log.info(pageRequestDTO);

        Function<Object[], BoardDTO> fn = (en -> entityToDTO((Board) en[0], (Member) en[1], (Long) en[2]));

        Page<Object[]> result = repository.getBoardWithReplyCount(pageRequestDTO.getPageable(Sort.by("bno").descending()));

        return new PageResultDTO<>(result, fn);
    }
}
```

getList()의 핵심은 entityToDTO()를 이용해서 PageResultDTO 객체를 구성하는 부분이다.          

###### 목록 처리 테스트         
BoardService의 getList() 역시 테스트 코드로 동작 여부를 확인한다.      

```java
    @Test
    public void testList(){
        PageRequestDTO pageRequestDTO = new PageRequestDTO();

        PageResultDTO<BoardDTO, Object[]> result = boardService.getList(pageRequestDTO);

        for(BoardDTO boardDTO : result.getDtoList()){
            System.out.println(boardDTO);
        }
    }
```

testList()는 1페이지에 해당하는 10개의 게시글, 회원, 댓글의 수를 처리한다.    
실행되는 쿼리와 결과는 아래와 같다.       

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    group by
        board0_.bno 
    order by
        board0_.bno desc limit ?
Hibernate: 
    select
        count(board0_.bno) as col_0_0_ 
    from
        board board0_
BoardDTO(bno=101, title=Test.., content=Test...., writerEmail=user55@aaa.com, wrierName=USER55, regDate=2022-06-23T20:45:03.943612, modDate=2022-06-23T20:45:03.943612, replyCount=0)
BoardDTO(bno=100, title=Title...100, content=Content...100, writerEmail=user100@aaa.com, wrierName=USER100, regDate=2022-06-22T23:43:12.653917, modDate=2022-06-22T23:43:12.653917, replyCount=4)
BoardDTO(bno=99, title=Title...99, content=Content...99, writerEmail=user99@aaa.com, wrierName=USER99, regDate=2022-06-22T23:43:12.650927, modDate=2022-06-22T23:43:12.650927, replyCount=1)
BoardDTO(bno=98, title=Title...98, content=Content...98, writerEmail=user98@aaa.com, wrierName=USER98, regDate=2022-06-22T23:43:12.647938, modDate=2022-06-22T23:43:12.647938, replyCount=2)
BoardDTO(bno=97, title=Title...97, content=Content...97, writerEmail=user97@aaa.com, wrierName=USER97, regDate=2022-06-22T23:43:12.645935, modDate=2022-06-22T23:43:12.645935, replyCount=1)
BoardDTO(bno=96, title=Title...96, content=Content...96, writerEmail=user96@aaa.com, wrierName=USER96, regDate=2022-06-22T23:43:12.643794, modDate=2022-06-22T23:43:12.643794, replyCount=5)
BoardDTO(bno=95, title=Title...95, content=Content...95, writerEmail=user95@aaa.com, wrierName=USER95, regDate=2022-06-22T23:43:12.641804, modDate=2022-06-22T23:43:12.641804, replyCount=1)
BoardDTO(bno=94, title=Title...94, content=Content...94, writerEmail=user94@aaa.com, wrierName=USER94, regDate=2022-06-22T23:43:12.639806, modDate=2022-06-22T23:43:12.639806, replyCount=1)
BoardDTO(bno=93, title=Title...93, content=Content...93, writerEmail=user93@aaa.com, wrierName=USER93, regDate=2022-06-22T23:43:12.636816, modDate=2022-06-22T23:43:12.636816, replyCount=3)
BoardDTO(bno=92, title=Title...92, content=Content...92, writerEmail=user92@aaa.com, wrierName=USER92, regDate=2022-06-22T23:43:12.634823, modDate=2022-06-22T23:43:12.634823, replyCount=4)
```

실행 결과를 보면 BoardDTO 객체 내에 목록 화면에 필요한 10개의 BoardDTO 객체가 만들어지고 필요한 모든 내용이 담겨져 있는 것을 확인할 수 있다.       

#### 게시물 조회 처리       
게시물의 조회는 파라미터로 게시물의 번호(bno)를 파라미터로 받아서 처리하도록 BoardService와 BoardServiceImpl 클래스에 get()메서드를 추가해서 작성한다.      

```java
BoardService 인터페이스의 일부

public interface BoardService {
    Long register(BoardDTO dto);

    PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO);   //목록 처리

    BoardDTO get(Long bno);
```

게시물 조회는 BoardRepository의 Board엔티티와 Member 엔티티, 댓글의 수(Long)를 가져오는 getBoardByBno()를 이용해서 처리한다.        
```java
BoardServiceImpl 클래스의 일부

    @Override
    public BoardDTO get(Long bno) {
        Object result = repository.getBoardByBno(bno);

        Object[] arr = (Object[])result;

        return entityToDTO((Board)arr[0], (Member)arr[1], (Long)arr[2]);
    }
```

###### 게시물 조회 테스트        
게시물 조회 테스트는 기존의 BoardServiceTests를 이용해서 작성할 수 있다.      

```java
    @Test
    public void testGet(){
        Long bno = 100L;

        BoardDTO boardDTO = boardService.get(bno);

        System.out.println(boardDTO);
    }
```

테스트 결과는 아래와 같이 출력된다.        

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on board0_.writer_email=member1_.email 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    where
        board0_.bno=?
BoardDTO(bno=100, title=Title...100, content=Content...100, writerEmail=user100@aaa.com, wrierName=USER100, regDate=2022-06-22T23:43:12.653917, modDate=2022-06-22T23:43:12.653917, replyCount=4)
```

#### 게시물 삭제 처리       
실제 개발에서 게시물의 삭제는 조금 고민의 여지가 있다.        
댓글이 없는 게시물은 게시물의 작성자가 삭제하면 문제가 없지만, 댓글이 있는 경우에는 다른 사용자들이 추가한 댓글이 아무런 동의 없이 삭제되는 문제가 발생하기 때문이다.     
그러므로 실제 개발에서는 게시물의 상태를 칼럼으로 지정하고, 이를 변경하는 형태로 처리하는 것이 일반적인 방식이라고 할 수 있다.          

예제에서는 이를 고민하지 않고 게시물을 삭제할 수 있다고 가정하고 진행한다.        
다만 게시물을 삭제하려면 FK로 게시물을 참조하고 있는 reply 테이블 역시 삭제해야만 한다.       
작업의 순서는 1) 해당 게시물의 모든 댓글을 삭제하고, 2) 해당 게시물을 삭제한다.            
가장 중요한 사실은 우의 두 작업이 하나의 트랜잭션으로 처리되어야 한다는 점이다.     
먼저 ReplyRepository에 특정 게시물 번호bno로 댓글을 삭제하는 기능을 추가한다.         

```java
package org.zerock.board.repository;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @Modifying
    @Query("delete from Reply r where r.board.bno = :bno")
    void deleteByBno(Long bno);
}
```

JPQL을 이용해서 update, delete를 실행하기 위해서는 @Modifying 어노ㅔ이션을 같이 추가해야 한다.        
BoardService에는 삭제 관련 기능을 선언한다.      

```java
void removeWithReplies(Long bno);
```

```java
BoardServiceImpl 추가분

    private final ReplyRepository replyRepository;

    @Transactional
    @Override
    public void removeWithReplies(Long bno) {
        //댓글 부터 삭제
        replyRepository.deleteByBno(bno);

        repository.deleteById(bno);
    }
```

BoardServiceImpl에서 가장 달라지는 점은 ReplyRepository를 주입받는다는 점과      
removeWithReplies()에 @Transactional이 추가된 부분이다.         

###### 삭제 테스트           
게시물의 삭제 역시 테스트 코드에서 확인하고 개발을 진행한다.        
우선 데이터베이스에 댓글이 있는 게시글의 번호를 하나 선택하고 이를 삭제한다.      

```java
    @Test
    public void testRemove(){
        Long bno = 100L;

        boardService.removeWithReplies(bno);
    }
```

테스트 코드를 실행하면 아래와 같이 reply 테이블이 먼저 삭제되고, board 테이블을 조회한 후에 삭제된다.           

```
Hibernate: 
    delete 
    from
        reply 
    where
        board_bno=?
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_ 
    from
        board board0_ 
    where
        board0_.bno=?
Hibernate: 
    delete 
    from
        board 
    where
        bno=?
```

#### 게시물 수정 처리         
게시물의 수정은 필요한 부분만을 변경하고 BoardRepository의 save()를 이용해서 처리한다.        
게시물의 수정은 제목title과 내용content에 한해서 수정이 가능하도록 설정한다.          
수정을 위해서 Board 클래스에 수정에 필요한 메서드를 추가한다.         

```java
package org.zerock.board.entity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "writer")//@ToString은 항상 exclude
public class Board extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    private String title;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)  //명시적으로 Lazy 로딩 지정
    private Member writer;  //연관관계 지정
    
    public void chageTitle(String Title){
        this.title = title;
    }
    
    public void changeContent(String content){
        this.content = content;
    }
}
```

BoardService에는 BoardDTO를 이용해서 수정하는 modify()를 선언하고 BoardSerivceImple 클래스에서 구현한다.      

```java
    BoardService 클래스 일부
    void modify(BoardDTO boardDTO);


    BoardServiceImpl 클래스 일부
    @Transactional  //에러떠서 넣으니 안뜸.. 무슨 에러인 지는 아직 실력 부족으로 판단 불가
    @Override
    public void modify(BoardDTO boardDTO){
        Board board = repository.getOne(boardDTO.getBno());

        board.chageTitle(boardDTO.getTitle());
        board.changeContent(boardDTO.getContent());

        repository.save(board);
    }
```
modify()는 findById()를 이용하는 대신에 필요한 순간까지 로딩을 지연하는 방식인 getOne()을 이용해서 처리한다.        

###### 수정 테스트         
게시물의 수정 역시 BoardServiceTests 테스트 클래스를 이용해서 처리한다.        

```java
BoardServiceTests 클래스 일부

@Test
public void testModify(){
        BoardDTO boardDTO = BoardDTO.builder()
        .bno(2L)
        .title("제목 변경합니다.")
        .content("내용 변경합니다.")
        .build();

        boardService.modify(boardDTO);
        }
```

테스트 코드를 실행하면 select를 이용해서 원래의 Board 객체를 조회하고 update문이 실행되는 것을 볼 수 있다.        

```
Hibernate: 
    select
        board0_.bno as bno1_0_0_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_ 
    from
        board board0_ 
    where
        board0_.bno=?
Hibernate: 
    update
        board 
    set
        mod_date=?,
        content=?,
        title=?,
        writer_email=? 
    where
        bno=?
```

## 컨트롤러와 화면 처리        

서비스 계층까지의 구현이 끝났다면 남은 작업은 컨트롤러와 화면을 처리하는 것이다.          
다행스러운 점은 PageRequestDTO나 PageResultDTO는 동일하게 처리할 수 있기 때문에 이전에 작성했던 코드를 그대로 사용할 수 있다는 것이다.        

```java
package org.zerock.board.controller;

@Controller
@RequestMapping("/board")
@Log4j2
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
}

```

결과 화면에 필요한 파일들은 이전 장의 내용을 참고하여 static 폴더에 동일하게 추가하고,          
template 폴더에는 layout 폴더를 그대로 사용하고 board 폴더를 만들어 둔다.       

#### 목록 컨트롤러/화면 처리        

작성된 BoardController에 list()를 아래와 같이 작성한다.         

```java
    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model) {
        log.info("list.........."+pageRequestDTO);

        model.addAttribute("result", boardService.getList(pageRequestDTO));
    }
```

templates 내에 board 폴더에 list.html을 아래와 같이 작성한다.         

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">

<th:block th:replace="~{/layout/basic::setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1 class="mt-4">Board List Page
            <span>
                <a th:href="@{/board/regist}">
                    <button type="button" class="btn btn-outline-primary">REGISTER</button>
                </a>
            </span>
        </h1>
    </th:block>
</th:block>
```

화면 목록을 처리하는 부분은 댓글의 개수, 작성자 이름/이메일을 출력하도록 수정한다.         

```html
    <table class="table table-striped">
        <thead>
        <tr>
            <th scope="col">#</th>
            <th scope="col">Title</th>
            <th scope="col">Writer</th>
            <th scope="col">Regdate</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="dto : ${result.dtoList}">
            <th scope="row">
                <a th:href="@{/board/read(bno=${dto.bno},
                            page=${result.page},
                            type=${pageResultDTO.type},
                            keyword=${pageRequestDTO.keyword})}">
                    [[${dto.bno}]]
                </a>
            </th>
            <td>[[${dto.title}]]------------[<b th:text="${dto.replyCount}"></b>]</td>
            <td>[[${dto.writerName}]]<small>[[${dto.writerEmail}]]</small></td>
            <td>[[${#temporals.format(dto.regDate, 'yyyy/MM/dd')}]]</td>
        </tr>
        </tbody>
    </table>
```

이렇게 처리된 list.html은 브라우저에서 /board/list와 같은 경로로 확인할 수 있다.             
화면 하단에 페이지 처리도 링크를 수정해서 처리한다.        

```html
 <ul class="pagination h-100 justify-content-center align-items-center">
     <li class="page-item" th:if="${result.prev}">
        <a class="page-link" th:href="@{/board/list(page=${result.start-1},
                                        type=${pageRequestDTO.type},
                                        keyword=${pageRequestDTO.keyword})}" tabindex="-1">Previous</a>
     </li>
            
     <li th:class="'page-item' + ${result.page == page?'active':''}"
         th:each="page: ${result.pageList}">
         <a class="page-link" th:href="@{/board/list(page=${page},
                                         type=${pageRequestDTO.type},
                                         keyword=${pageRequestDTO.keyword})}">
             [[${page}]]
         </a>
     </li>
     <li class="page-item" th:if="${result.next}">
         <a class="page-link" th:href="@{/board/list(page=${result.end+1},
                                         type=${pageRequestDTO.type},
                                         keyword=${pageRequestDTO.keyword})}">Next</a>
     </li>
</ul>
```

#### 게시물 등록 처리           

게시물의 등록 처리는 기존과 다른 점은 없지만 반드시 작성자 부분을 현재 존재하는 사용자의 이메일 주소로 지정할 필요가 있다.            
BoardController에서는 GET 방식으로 동작하는 링크와 POST 방식으로 실제 처리하는 메서드를 추가한다.       
정상적인 등록 후에는 다시 목록 페이지로 이동하도록 작성한다.           

```java
BoardCOntroller 클래스 일부

    @GetMapping("/register")
    public void register(){
        log.info("register get...");
    }

    @PostMapping("/register")
    public String registerPost(BoardDTO dto, RedirectAttributes redirectAttributes){
        log.info("dto..."+dto);
        //새로 추가된 엔티티의 번호
        Long bno = boardService.register(dto);

        log.info("BNO: "+bno);

        redirectAttributes.addFlashAttribute("msg", bno);

        return "redirect:/board/list";
    }
```

화면 처리는 templates 폴더 내 존재하는 board 폴더 내에 register.html 페이지를 추가한다.          
register.html은 이전 장의 예제와 거의 동일하고, < form > 태그의 action 속성값과 writerEmail 부분이 변경된다.        

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/basic::setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1 class="mt-4">Board Register Page</h1>

        <form th:action="@{/board/register}" th:method="post">
            <div class="form-group">
                <label>Title</label>
                <input type="text" class="form-control" name="title" placeholder="Enter Title">
            </div>
            <div class="form-group">
                <label>Content</label>
                <textarea class="form-control" rows="5" name="content"></textarea>
            </div>
            <div class="form-group">
                <label>Writer Email</label>
                <input type="email" class="form-control" name="writerEmail" placeholder="Writer Email">
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>
        </form>
    </th:block>
</th:block>
```

주의해야 할 점은 작성자 부분의 내용을 member 테이블에 존재하는 이메일 주소를 지정하는 것이다.      

#### 게시물 조회 처리        

게시물의 조회 처리 역시 BoardController를 처리하고 화면을 구성한다.         

```java
    @GetMapping("/read")
    public void read(@ModelAttribute("requestDTO") PageRequestDTO pageRequestDTO, Long bno, Model model){
        log.info("bno: "+bno);

        BoardDTO boardDto = boardService.get(bno);

        log.info(boardDto);

        model.addAttribute("dto", boardDto);
    }
```

templates 폴더 내의 board 폴더 안에 read.html을 작성한다.      

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="~{/layout/basic::setContent(~{this::content})}">
  <th:block th:fragment="content">
    <h1 class="mt-4">Board Read Page</h1>

    <div class="form-group">
      <label>Bno</label>
      <input type="text" class="form-control" name="gno" th:value="${dto.bno}" readonly>
    </div>
    <div class="form-group">
      <label>Title</label>
      <input type="text" class="form-control" name="title" th:value="${dto.title}" readonly>
    </div>
    <div class="form-group">
      <label>Content</label>
      <textarea class="form-control" rows="5" name="content" readonly>[[${dto.content}]]</textarea>
    </div>
    <div class="form-group">
      <label>Writer</label>
      <input type="text" class="form-control" name="writer" th:value="${dto.writerName}" readonly>
    </div>
    <div class="form-group">
      <label>RegDate</label>
      <input type="text" class="form-control" name="regDate"
             th:value="${#temporals.format(dto.regDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
    </div>
    <div class="form-group">
      <label>ModDate</label>
      <input type="text" class="form-control" name="modDate"
             th:value="${#temporals.format(dto.modDate, 'yyyy/MM/dd HH:mm:ss')}" readonly>
    </div>
  </th:block>
</th:block>
```

댓글과 관련된 작업은 모두 게시물의 조회 페이지에서 댓글의 숫자를 보여주거나 댓글을 입력하는 화면 등을 추가하게 된다.       

#### 게시물 수정/삭제 처리        

게시물의 수정은 /board/modify를 통해서 접근하는 페이지에서 이루어진다.         
우선 BoardController에 GET 방식과 POST 방식으로 수정/삭제 작업을 처리하는 메서드를 선언한다.         

```java
    @GetMapping({"/read", "/modify"})
    public void read(@ModelAttribute("requestDTO") PageRequestDTO pageRequestDTO, Long bno, Model model){
        log.info("bno: "+bno);

        BoardDTO boardDTO = boardService.get(bno);

        log.info(boardDTO);

        model.addAttribute("dto", boardDTO);
    }
    @PostMapping("/remove")
    public String remove(long bno, RedirectAttributes redirectAttributes){
        log.info("bno: "+bno);

        boardService.removeWithReplies(bno);

        redirectAttributes.addFlashAttribute("msg", bno);

        return "redirect:/board/list";
    }
    @PostMapping("/modify")
    public String modify(BoardDTO dto, @ModelAttribute("requestDTO") PageRequestDTO requestDTO, RedirectAttributes redirectAttributes){

        log.info("post modify...............");

        log.info("dto: "+dto);

        boardService.modify(dto);

        redirectAttributes.addAttribute("page", requestDTO.getPage());
        redirectAttributes.addAttribute("type", requestDTO.getType());
        redirectAttributes.addAttribute("keyword", requestDTO.getKeyword());

        redirectAttributes.addAttribute("bno", dto.getBno());

        return "redirect:/board/read";
    }

```

GET 방식으로 들어오는 수정 작업을 위한 화면은 조회와 동일하므로 read() 메서드를 그대로 이용하고,        
POST 방식의 처리는 boardService의 modify()/remove()를 호출해서 처리한다.        
화면은 templates 폴더 밑에 modify.html 파일을 작성해서 처리한다.          

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">

<th:block th:replace="~{/layout/basic :: setContent(~{this::content})}">
    <th:block th:fragment="content">
        <h1 class="mt-4">Board Modify Page</h1>

        <form action="/board/modify" method="post">
            <input type="hidden" name="page" th:value="${requestDTO.page}">
            <input type="hidden" name="type" th:value="${requestDTO.type}">
            <input type="hidden" name="keyword" th:value="${requestDTO.keyword}">
            <div class="form-group">
                <label>Gno</label>
                <input type="text" class="form-control" name="bno" th:value="${dto.bno}" readonly>
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
                <input type="text" class="form-control" name="writer" th:value="${dto.writerName}" readonly>
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
<script th:inline="javascript">
        var actionForm = $("form");

        $(".removeBtn").click(function(){

            actionForm.attr("action", "/board/remove")
                    .attr("method", "post");

            actionForm.submit();
        });

        $(".modifyBtn").click(function(){
            if(!confirm("수정하시겠습니까?")){
                return;
            }
            actionForm.attr("action", "/board/modify")
                    .attr("method", "post")
                    .submit();
        });

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

            actionForm.attr("action", "/board/list")
                        .attr("method", "get");

            console.log(actionForm.html());
            actionForm.submit();
        });
</script>

```

## JPQL로 검색       

FK를 이용해서 @ManyToOne과 같은 연관관계를 작성했을 때 가장 어려운 작업은 검색에 필요한 JPQL을 구성하는 것이다.           
이전에는 하나의 엔티티 타입만을 이용했기 때문에 비교적 간단하게 동적 쿼리를 생성할 수 있었지만,           
여러 엔티티 타입을 JPQL로 직접 처리하는 경우에는 Object[] 타입으로 나오기 때문에(흔히 Tuple이라고 한다.) 작성하는 방법 자체가 다르고 복잡하다.          
반면에 어떤 상황에서도 사용할 수 있는 가장 강력한 JPQL을 구성할 수 있는 방식이기도 하다.         

#### 프로젝트 변경          

현재까지 작성된 프로젝트는 Querydsl 설정이 없는 상태에서 작성되었으므로 build.gradle 파일의 설정을 변경하는 작업부터 시작한다.           

```
build.gradle

buildscript {
	ext {
		queryDslVersion = "5.0.0"
	}
}


plugins {
	id 'org.springframework.boot' version '2.6.4'
	id 'io.spring.dependency-management' version '1.0.11.RELEASE'
	id 'java'
	id 'com.ewerk.gradle.plugins.querydsl' version '1.0.10'
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

	implementation 'com.querydsl:querydsl-jpa'
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

def querydslDir = "$buildDir/generated/querydsl"

querydsl{
	jpa=true
	querydslSourcesDir=querydslDir
}
sourceSets{
	main.java.srcDir querydslDir
}
configurations{
	querydsl.extendsFrom compileClasspath
}
compileQuerydsl{
	options.annotationProcessorPath = configurations.querydsl
}

버전 문제로 기존 내용이 책이랑 달라서 고민했으나 그냥 똑같이 추가했다. 문제 생기면 머리아파진다.
문제가 생겨서 그냥 기존 그대로 했다.
Q도메인 클래스들이 생성은 됐으나 위치가 다르다.
```

#### Repository를 확장하는 법            

Spring Data JPA의 Repository를 확장하기 위해서는 다음과 같은 단계로 처리된다.           
- 쿼리 메서드나 @Query 등으로 처리할 수 없는 기능은 별도의 인터페이스로 설계          
- 별도의 인터페이스에 대한 구현 클래스를 작성한다. 이때 QueryRepositorySupport라는 클래스를 부모 클래스로 사용           
- 구현 클래스에 인터페이스의 기능을 Q도메인 클래스와 JPQLQuery를 이용해 구현      

###### QuerydslRepositorySupport 동작 확인          

개발자가 원하는 대로 동작하는 Repository를 작성하는데 있어서 가장 중요한 클래스는 QuerydslRepositorySupport라는 클래스다.         
QuerydslRepositorySupport 클래스는 Spring Data JPA에 포함된 클래스로 Qeurydsl 라이브러리를 이용해서 직접 무언가를 구현할 때 사용한다.         

기존의 Repository 패키지 내에 search라는 패키지를 추가한다.             
추가된 패키지에는 확장하고 싶은 기능을 인터페이스로 선언한다.          
이때 인터페이스 내의 메서드 이름은 가능하면 쿼리 메서드와 구별이 가능하도록 작성한다.        
Search 패키지에는 SearchBoardRepository 인터페이스를 구현하고, SearchBoardRepositoryImpl 클래스를 선언한다.        

SearchBoardRepository에는 단순하게 Board 타입 객체를 반환하는 메서드를 하나 선언한다.       

```java
package org.zerock.board.repository.search;

public interface SearchBoardRepository {
    Board search1();
}
```

SearchBoardRepositoryImpl 클래스는 인터페이스에 선언된 메서드를 구현한다. 이때 로그를 같이 사용해서 동작 여부를 확인한다.        

```java
package org.zerock.board.repository.search;

@Log4j2
public class SearchBoardRepositoryImpl extends QuerydslRepositorySupport implements SearchBoardRepository{

    public SearchBoardRepositoryImpl(){
        super(Board.class);
    }

    @Override
    public Board search1() {
        log.info("search1............");
        
        return null;
    }
}

```

SearchBoardRepositoryImpl 클래스에서 가장 중요한 점은 QuerydslRepositorySupport 클래스를 상속해야 한다는 점이다.        
QuerydslRepositorySupport는 생성자가 존재하므로 클래스 내에서 super()를 이용해서 호출해야 한다.        
이때 도메인 클래스를 지정하는데 null값을 넣을 수 없다.        
실제 구현은 조금 뒤로 미루고 log.info()를 이용해서 동작하는지를 확인하는 목적으로 사용한다.        
BoardRepository는 선언한 SearchBoardRepository 인터페이스를 상속하는 형태로 변경한다.           

```java
public interface BoardRepository extends JpaRepository<Board, Long>, SearchBoardRepository {
```

정상적으로 동작하는지는 test폴더의 BoardRepositoryTests를 이용해서 확인한다.          

```java
    @Test
    public void testSearch1(){
        boardRepository.search1();
    }
```

테스트 코드를 실행하면 아직은 SQL을 실행하지는 않지만 log.info()부분은 동작 여부를 확인할 수 있다.       

```
2022-06-25 16:29:22.425  INFO 5936 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : search1............
```

###### JPQLQuery 객체              

위와 같이 로그가 기록되면서 동작하는 것을 확인했다면 실제 JPQL을 작성하고 실행해보는 단계로 넘어간다.        
이 과정에서 Querydsl 라이브러리 내에는 JPQLQuery라는 인터페이스를 활용하게 된다.        
SearchBoardRepositoryImpl 클래스를 아래와 같이 수정해서 Querydsl을 실행할 수 있다.        

```java
    @Override
    public Board search1() {
        log.info("search1............");

        QBoard board = QBoard.board;

        JPQLQuery<Board> jpqlQuery = from(board);

        jpqlQuery.select(board).where(board.bno.eq(1L));

        log.info("-------------------");
        log.info(jpqlQuery);
        log.info("-------------------");

        List<Board> result = jpqlQuery.fetch();

        return null;
    }
```

이전의 테스트 코드를 실행해서 search1()을 실행하면 기존과 달리 실제 SQL이 실행되는 것을 확인할 수 있다.       

```
2022-06-25 16:38:09.648  INFO 15748 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : search1............
2022-06-25 16:38:09.674  INFO 15748 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : -------------------
2022-06-25 16:38:09.677  INFO 15748 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : select board
from Board board
where board.bno = ?1
2022-06-25 16:38:09.677  INFO 15748 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : -------------------
Hibernate: 
    select
        board0_.bno as bno1_0_,
        board0_.mod_date as mod_date2_0_,
        board0_.reg_date as reg_date3_0_,
        board0_.content as content4_0_,
        board0_.title as title5_0_,
        board0_.writer_email as writer_e6_0_ 
    from
        board board0_ 
    where
        board0_.bno=?

```

실행되는 로그를 보면 중간에 ---------을 이용해서 구분해둔 부분에서 출력되는 내용이 JPQL의 문자열과 동일하다는 것을 볼 수 있고,           
아래쪽에는 실제 동작하는 SQL을 볼 수 있다.         

###### JPQLQuery의 leftJoin()/on()          
JPQLQuery로 다른 엔티티와 조인을 처리하기 위해서는 join() 혹은 leftjoin(), rightJoin() 등을 이용하고             
필요한 경우 on()을 이용해서 조인에 필요한 부분을 완성할 수 있다.         
Board는 Reply와 left(outer) join을 이용해야 하는 상황이면 다음과 같은 코드를 작성할 수 있다.

```java
QBoard board = QBoard.board;
QReply reply = QReply.reply;

JPQLQuery<Board> jpqlQuery = from(board);
jpqlQuery.leftJoin(reply).on(reply.board.eq(board));

여기서 from이 자꾸 에러가 뜬다... 같은 코드를 serviceImpl에서 쓸 때는 에러가 안나는데
테스트에서만 에러가 난다..
```

위와 같이 수정된 search1()의 실행 결과에는 아래와 같은 JPQL이 출력된다.       
(JPA 2.1부터는 left out join 처리에 on 구문이 추가될 수 있지만, 과거에는 with를 이용했다.)              
만들어지는 JPQL은 아래와 같은 형태가 된다.      

###### Tuple 객체         

JPQLQuery의 leftJoin(), join()을 이용해서 Board, Member, Reply를 처리할 수 있고, groupBy() 등을              
이용해서 집합 함수를 처리하는 것도 가능하다. 기존의 코드를 아래와 같이 수정한다.        

```java
SearchBoardRepositoryImpl 클래스 수정

    @Override
    public Board search1() {
        log.info("search1............");

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);
        jpqlQuery.leftJoin(member).on(board.writer.eq(member));
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board));

        jpqlQuery.select(board, member.email, reply.count()).groupBy(board);

        log.info("-------------------");
        log.info(jpqlQuery);
        log.info("-------------------");

        List<Board> result = jpqlQuery.fetch();

        return null;
    }
```

변경된 부분은 Member에 대한 leftjoin()과 select() 뒤의 groupBy()를 적용한 부분이다.        
select() 내에도 여러 객체를 가져오는 형태로 변경되었다.                  
이렇게 정해진 엔티티 객체 단위가 아니라 각각의 데이터를 추출하는 경우에도 Tuple 이라는 객체를 이용한다.         
위의 코드를 Tuple을 이용하도록 수정하면 다음과 같은 형태가 된다.          

```java

    @Override
    public Board search1() {
        log.info("search1............");

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);
        jpqlQuery.leftJoin(member).on(board.writer.eq(member));
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board));

        JPQLQuery<Tuple> tuple = jpqlQuery.select(board, member.email, reply.count());
        tuple.groupBy(board);
        
        log.info("-------------------");
        log.info(jpqlQuery);
        log.info("-------------------");

        List<Tuple> result = tuple.fetch();

        log.info(result);
        
        return null;
    }
```

select()의 결과를 JPQLQuery< Tuple >을 이용해서 처리하도록 변경하고 result 변수의 타입도 List< Tuple > 타입으로 변경되었다.        
위의 코드의 실행 결과로 출력되는 로그는 다음과 같이 나누어 볼 수 있다.

#### JPQLQuery로 Page< Object[] > 처리          

위 Tuple의 마지막 결과를 보면 Board 객체와 작성자의 이메일, 댓글의 개수가 출력되는 것을 볼 수 있다.        
마지막 남은 작업은 원하는 파라미터(Pageable)를 전송하고, Page< Object[] >를 만들어서 반환하는 것이다.        
SearchBoardRepository에 파라미터와 리턴타입을 반영하는 searchList()를 다음과 같이 설계한다.        

```java

package org.zerock.board.repository.search;

public interface SearchBoardRepository {
    Board search1();

    Page<Object[]> searchPage(String type, String keyword, Pageable pageable);
}
```

새롭게 추가된 searchPage()는 검색 타입type과 키워드keyword, 페이지 정보Pageable를 파라미터로 추가한다.         
(PageRequestDTO 자체를 파라미터로 처리하지 않는 이유는 DTO를 가능하면 Repository 영역에서 다루지 않기 위함이다.)           
SearchBoardRepositoryImpl 클래스에는 아래와 같이 로그만을 실행하도록 작성한다.           

```java
    @Override
    public Page<Object[]> searchPage(String type, String keyword, Pageable pageable) {
        log.info("searchPage..................");

        return null;
    }
```

테스트 코드를 BoardRepositoryTests에 추가한다.        

```java
    @Test
    public void testSearchPage(){
        Pageable pageable = PageRequest.of(0,10,Sort.by("bno").descending());
        Page<Object[]> result = boardRepository.searchPage("t", "1", pageable);
    }
```

테스트 작업에는 제목(t)으로 1이라는 단어가 있는 데이터를 검색하도록 한다.          
테스트 코드를 실행하면 아직 구현된 내용이 없기 때문에 단순히 로그만 출력된다.        

```
2022-06-25 17:34:19.019  INFO 23304 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : searchPage..................
2022-06-25 17:34:19.038  INFO 23304 --- [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'

```

###### 검색 조건의 처리 BooleanExpression           
파라미터로 전달되는 type 값은 제목t, 내용c, 작성자w를 하나 혹은 조합으로 tcw와 같은 형태이다.             
이 조건에 대한 BooleanExpression 처리는 이전에 다룬 적이 있으므로 이를 이용해서 searchPage()를 다음과 같이 구현한다.       

```java
    @Override
    public Page<Object[]> searchPage(String type, String keyword, Pageable pageable) {
        log.info("searchPage..................");

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);
        jpqlQuery.leftJoin(member).on(board.writer.eq(member));
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board));

        //SELECT b, w, count(r) FROM Board b
        //LEFT JOIN b.writer w LEFT JOIN Reply r on r.board = b
        JPQLQuery<Tuple> tuple = jpqlQuery.select(board, member, reply.count());

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        BooleanExpression expression = board.bno.gt(0L);

        booleanBuilder.and(expression);

        if(type!=null){
            String[] typeArr = type.split("");
            BooleanBuilder conditionBuilder = new BooleanBuilder();

            for(String t: typeArr){
                switch(t){
                    case "t":
                        conditionBuilder.or(board.title.contains(keyword));
                        break;
                    case "w":
                        conditionBuilder.or(member.email.contains(keyword));
                        break;
                    case "c":
                        conditionBuilder.or(board.content.contains(keyword));
                        break;
                }
            }
            booleanBuilder.and(conditionBuilder);
        }
        tuple.where(booleanBuilder);
        tuple.groupBy(board);
        List<Tuple> result = tuple.fetch();
        log.info(result);
        return null;
    }
```

변경된 부분은 파라미터에 따라서 검색 조건을 추가할 수 있도록 BooleanBuilder와 BooleanExpression들이 추가된 것이다.          
tuple.groupBy()의 경우 메서드의 하단에서 처리한다. 테스트 코드를 실행해서 검색 조건이 제대로 작성되는지 확인한다.         

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on (
                board0_.writer_email=member1_.email
            ) 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    where
        board0_.bno>? 
        and (
            board0_.title like ? escape '!'         //제목으로 검색되는 조건이 추가되었다.
        ) 
    group by
        board0_.bno
```

검색 조건이 변경되면 위의 where 조건절도 같이 변경된다.     
남은 작업은 orderBy()에 대한 처리와 Page 타입의 객체를 구성하는 것이다.        

###### sort처리/count처리           

Pageable의 Sort 객체는 JPQLQuery의 orderBy()의 파라미터로 전달되어야 하지만 JPQL에서는 Sort 객체를 지원하지 않기 때문에            
orderBy()의 경우 OrderSpecifier< T extends Comparable >을 파라미터로 처리해야 한다.       

OrderSpecifier에 Order는 com.querydsl.core.types.Order 타입이고, Expression은 com.querydsl.core.types.Expression 이다.        
이를 처리하기 위해서는 다음과 같이 복잡한 방법으로 해야 한다.     

```java
//order by
Sort sort = pageable.getSort();

//tuple.orderBy(board.bno.desc());  //직접 코드로 처리하면
sort.stream().forEach(order -> {
    Order direction = order.isAscending()? Order.ASC:Order.DESC;
    String prop = order.getProperty();
    
    PathBuilder orderByExpression = new PathBuilder(Board.class, "board");
    tuple.orderBy(new OrderSpecifier(direction, orderByExpression.get(prop)));
        });
```

org.springframework.data.domain.Sort는 내부적으로 여러 개의 Sort 객체를 연결할 수 있기 때문에 forEach()를 이용해서 처리한다.      
OrderSpecifier에는 정렬이 필요하므로 Sort 객체의 정렬 관련 정보를 com.querydsl.core.types.Order 타입으로 처리하고,          
Sort 객체에 속성(bno, title) 등은 PathBuilder라는 것을 이용해서 처리한다.          
PathBuilder를 생성할 때 문자열로 된 이름은 JPQLQuery를 생성할 때 이용하는 변수명과 동일해야 한다.         

JPQLQuery를 이용해서 동적으로 검색 조건을 처리해보면 상당히 복잡하고 어렵다는 생각을 하게 된다.           
대신에 얻는 장점은 한 번의 개발만으로 count 쿼리도 같이 처리할 수 있다는 점이다.      
count를 얻는 방법은 fetchCount()를 이용하면 된다.          

```java
long count = tuple.fetchCount();
log.info("count: "+count);
```

###### PageImpl 클래스         
Pageable을 파라미터로 전달받은 이유는 JPQLQuery의 offset()과 limit()을 이용해서 페이지 처리를 진행하기 위해서다.           

```java
//page 처리
tuple.offset(pageable.getOffset());
tuple.limit(pageable.getPageSize());
```

SearchBoardRepositoryImpl 클래스의 searchPage()의 리턴 타입은 Page< Object[] >타입이므로 메서드의 내부에서 Page 타입의 객체를 생성해야 한다.        
Page는 인터페이스 타입이므로 실제 객체는 org.springframework.data.domain.PageImpl 클래스를 이용해서 생성한다.      
PageImple 클래스의 생성자에는 Pageable과 long 값을 이용하는 생성자가 존재한다.         

최정적으로 완성된 코드는 아래와 같다.       

```java
SearchBoardRepositoryImpl 클래스 일부

    @Override
    public PageImpl searchPage(String type, String keyword, Pageable pageable) {
        log.info("searchPage..................");

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        QMember member = QMember.member;

        JPQLQuery<Board> jpqlQuery = from(board);
        jpqlQuery.leftJoin(member).on(board.writer.eq(member));
        jpqlQuery.leftJoin(reply).on(reply.board.eq(board));

        //SELECT b, w, count(r) FROM Board b
        //LEFT JOIN b.writer w LEFT JOIN Reply r on r.board = b
        JPQLQuery<Tuple> tuple = jpqlQuery.select(board, member, reply.count());

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        BooleanExpression expression = board.bno.gt(0L);

        booleanBuilder.and(expression);

        if (type != null) {
            String[] typeArr = type.split("");
            BooleanBuilder conditionBuilder = new BooleanBuilder();

            for (String t : typeArr) {
                switch (t) {
                    case "t":
                        conditionBuilder.or(board.title.contains(keyword));
                        break;
                    case "w":
                        conditionBuilder.or(member.email.contains(keyword));
                        break;
                    case "c":
                        conditionBuilder.or(board.content.contains(keyword));
                        break;
                }
            }
            booleanBuilder.and(conditionBuilder);
        }
        tuple.where(booleanBuilder);

        //order by
        Sort sort = pageable.getSort();

        //tuple.orderBy(board.bno.desc());

        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();

            PathBuilder orderByExpression = new PathBuilder(Board.class, "board");

            tuple.orderBy(new OrderSpecifier(direction, orderByExpression.get(prop)));
        });

        tuple.groupBy(board);

        //page 처리
        tuple.offset(pageable.getOffset());
        tuple.limit(pageable.getPageSize());

        List<Tuple> result = tuple.fetch();
        log.info(result);

        long count = tuple.fetchCount();

        log.info("COUNT: " + count);

        return new PageImpl<Object[]>(
                result.stream().map(t->t.toArray()).collect(Collectors.toList()),
                pageable,
                count
        );
    }
```

searchPage()의 테스트 코드는 아래와 같다.      

```java
    @Test
    public void testSearchPage(){
        Pageable pageable = PageRequest.of(0,10,Sort.by("bno").descending()
                .and(Sort.by("title").ascending()));
        
        Page<Object[]> result = boardRepository.searchPage("t", "1", pageable);
    }
```

테스트 코드에서는 고의적으로 중첩되는 Sort 조건을 만들어서 추가하였다.        
실행 결과를 보면 order by 조건이 만들어진 것과, 목록을 위한 SQL과 count 처리를 위한 SQL이 실행되는 것을 확인할 수 있다.       

```
Hibernate: 
    select
        board0_.bno as col_0_0_,
        member1_.email as col_1_0_,
        count(reply2_.rno) as col_2_0_,
        board0_.bno as bno1_0_0_,
        member1_.email as email1_1_1_,
        board0_.mod_date as mod_date2_0_0_,
        board0_.reg_date as reg_date3_0_0_,
        board0_.content as content4_0_0_,
        board0_.title as title5_0_0_,
        board0_.writer_email as writer_e6_0_0_,
        member1_.mod_date as mod_date2_1_1_,
        member1_.reg_date as reg_date3_1_1_,
        member1_.name as name4_1_1_,
        member1_.password as password5_1_1_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on (
                board0_.writer_email=member1_.email
            ) 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    where
        board0_.bno>? 
        and (
            board0_.title like ? escape '!'
        ) 
    group by
        board0_.bno 
    order by
        board0_.bno desc,
        board0_.title asc limit ?
2022-06-25 22:17:52.195  INFO 20228 --- [    Test worker] o.z.b.r.s.SearchBoardRepositoryImpl      : [[Board(bno=102, title=한글테스트11, content=한글테스트11), Member(email=user1@aaa.com, password=1111, name=USER1), 0], [Board(bno=91, title=Title...91, content=Content...91), Member(email=user91@aaa.com, password=1111, name=USER91), 3], [Board(bno=81, title=Title...81, content=Content...81), Member(email=user81@aaa.com, password=1111, name=USER81), 4], [Board(bno=71, title=Title...71, content=Content...71), Member(email=user71@aaa.com, password=1111, name=USER71), 6], [Board(bno=61, title=Title...61, content=Content...61), Member(email=user61@aaa.com, password=1111, name=USER61), 5], [Board(bno=51, title=Title...51, content=Content...51), Member(email=user51@aaa.com, password=1111, name=USER51), 5], [Board(bno=41, title=Title...41, content=Content...41), Member(email=user41@aaa.com, password=1111, name=USER41), 4], [Board(bno=31, title=Title...31, content=Content...31), Member(email=user31@aaa.com, password=1111, name=USER31), 2], [Board(bno=21, title=Title...21, content=Content...21), Member(email=user21@aaa.com, password=1111, name=USER21), 2], [Board(bno=19, title=Title...19, content=Content...19), Member(email=user19@aaa.com, password=1111, name=USER19), 1]]
Hibernate: 
    select
        count(distinct board0_.bno) as col_0_0_ 
    from
        board board0_ 
    left outer join
        member member1_ 
            on (
                board0_.writer_email=member1_.email
            ) 
    left outer join
        reply reply2_ 
            on (
                reply2_.board_bno=board0_.bno
            ) 
    where
        board0_.bno>? 
        and (
            board0_.title like ? escape '!'
        )
```

#### 목록 화면에서 검색 처리              

화면에서의 검색 처리는 PageRequestDTO를 이용하기 때문에 이전 예제에서 검색에 사용했던 < form > 태그의 action 속성을          
/board/list로 변경하는 것을 제외하면 추가적인 작업은 필요하지 않다.         

```html
        <form action="/board/list" method="get" id="searchForm">
            <div class="input-group">
                <input type="hidden" name="page" value="1">
                <div class="input-group-prepend">
                    <select class="custom-select" name="type">
                        <option th:selected="${pageRequestDTO.type == null}">----------</option>
                        <option value="t" th:selected="${pageRequestDTO.type=='t'}">제목</option>
                        <option value="c" th:selected="${pageRequestDTO.type=='c'}">내용</option>
                        <option value="w" th:selected="${pageRequestDTO.type=='w'}">작성자</option>
                        <option value="tc" th:selected="${pageRequestDTO.type=='tc'}">제목+내용</option>
                        <option value="tcw" th:selected="${pageRequestDTO.type=='tcw'}">제목+내용+작성자</option>
                    </select>
                </div>
                <input class="form-control" name="keyword" th:value="${pageRequestDTO.keyword}">
                <div class="input-group-append" id="button-addon4">
                    <button class="btn btn-outline-secondary btn-search" type="button">Search</button>
                    <button class="btn btn-outline-secondary btn-clear" type="button">Clear</button>
                </div>
            </div>
        </form>

하단
<script th:inline="javascript">
    var msg = [[${msg}]];
    console.log(msg);

    if(msg){
        $(".modal").modal();
    }

    var searchForm = $("#searchForm");

    $('.btn-search').click(function(e){
        searchForm.submit();
    });

    $('.btn-clear').click(function(e){
        searchForm.empty().submit();
    });
</script>
```

BoardController 역시 기존과 동일하고 실제 코드의 변경은 BoardServiceImpl 클래스만 변경하면 된다.        

```java
    @Override
    public PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO) {
        log.info(pageRequestDTO);

        Function<Object[], BoardDTO> fn = (en -> entityToDTO((Board)en[0], (Member)en[1], (Long)en[2]));

        //Page<Object[]> result = repository.getBoardWithReplyCount(
        //pageRequestDTO.getPageable(Sort.by("bno").descending()));
        
        Page<Object[]> result = repository.searchPage(
                pageRequestDTO.getType(),
                pageRequestDTO.getKeyword(),
                pageRequestDTO.getPageable(Sort.by("bno").descending())
        );
        
        return new PageResultDTO<>(result, fn);
    }
```



