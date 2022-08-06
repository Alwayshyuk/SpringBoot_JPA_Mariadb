# M:N(다대다) 관계와 파일 업로드 처리       

이번에는 영화와 회원이 존재하고 회원이 영화에 대한 평점과 감상을 기록하는 시나리오를 기반으로 프로젝트를 구성한다.          

영화와 회원 사이에는 다음과 같은 명제가 성립한다.         
- 한 편의 영화는 여러 회원의 평가가 행해질 수 있다.         
- 한 명의 회원은 여러 영화에 대해서 평점을 줄 수 있다.          

## M:N(다대다) 관계의 특징

M:N(다대다) 관계는 논리적인 설계와 실제 테이블의 설계가 다르게 된다.          
가장 먼저 생각할 수 있는 Entity는 영화와 회원이다. 영화와 회원은 양쪽 모두 독립적인 엔티티로 설계가 가능하다.         

사람에 해당하는 회원의 입장에서 보면 '여러 편의 영화를 평가한다'는 구조가 되고         
영화의 입장에서 보면 '한편의 영화는 여러 회원이 존재한다'는 관계가 성립하므로 M:N 다대다의 형태가 된다.      

실제적으로 M:N을 사용하는 경우는 상당히 많다.         
문제는 M:N의 관계를 실제 테이블로 설계할 수가 없다는 것이다.       
테이블은 고정된 개수의 column을 가지고 있기 때문이다.        

예를 들어 여러 개의 상품이 있고 여러 개의 카테고리가 있다고 생각해 본다면          
특정한 상품에 대해 카테고리 정보를 추가한다면 상품 하나가 가전인 동시에 주방, 계절 가전, 신혼과 같이 여러 개의 칼럼에 속하게 된다.          
이를 처리하려고 한다면 고정된 수의 칼럼으로는 처리할 수 없다는 문제가 생긴다.       

대부분의 관계형 데이터베이스는 테이블이라는 정형화된 구조를 가지는 방식으로 만들어지기 때문에           
column을 지정하면서 최대 크기를 지정하므로 수평적 확장은 불가능하다.        

반면에 테이블은 Row라는 개념을 이용해서 수직으로는 확장이 가능하다.         
M:N을 해결하기 위해서는 실제 테이블 설계에서 mapping 테이블을 사용한다.         
매핑 테이블은 흔히 '연결 테이블'이라고 부르기도 하는데, 말 그대로 두 테이블의 중간에서 필요한 정보를 양쪽에서 끌어서 쓰는 구조이다.         

수직으로 데이터를 늘릴 수 있다는 점에 착안해서 N개의 데이터가 들어가야 하는 경우에는 별도의 테이블로 분리한다고 생각하면 된다.      
이런 방식을 적용하면 M:N 관계 설정은 실제로는 중간에 매핑 테이블이 하나 추가되는 형태가 된다.      

매핑 테이블의 특징은 다음과 같다.        
- 매핑 테이블의 작성 이전에 다른 테이블들이 먼저 존재해야 한다.          
- 매핑 테이블은 주로 명사가 아닌 동사나 히스토리에 대한 데이터를 보관하는 용도이다.         
- 매핑 테이블은 중간에서 양쪽의 PK를 참조하는 형태로 사용된다.          

#### JPA에서 M:N(다대다) 처리

JPA에서 M:N을 처리하는 방식은 크게 보면 두 가지가 있다.      

- @ManyToMany를 이용해서 처리하는 방식          
- 별도의 엔티티를 설계하고, @ManyToOne을 이용해서 처리하는 방식         

JPA에서는 @ManyToMany라는 어노테이션을 이용해서 각 엔티티와의 매핑 테이블이 자동으로 생성되는 방식의 처리가 가능하다.         
다만 @ManyToMany의 경우 작성하려는 예제에서 적용하기 어려운 점이 있다.              
데이터 구조를 보면 영화의 평점 정보는 리뷰 테이블에 있어야 하는데 @ManyToMany에서는 이 구조를 만들 수 없기 때문이다.         

@ManyToMany의 경우 주로 양방향 참조를 이용하는데 양방향 참조를 이용할 때는 상당한 주의가 필요하다.          
JPA의 실행에서 가장 중요한 것이 현재 메모리상(정확히는 Context)의 Entity 객체들의 상태와           
데이터베이스의 상태를 동기화시키는 것이라는 점을 생각해 보면 하나의 객체를 수정하는 경우에 다른 객체의 상태를 매번 일치하도록 변경하는 작업이 간단하지 않기 때문이다.       
실무에서도 가능하면 단방향 참조를 위주로 프로젝트를 진행하는 이유가 이러한 연관된 객체들이 많은 경우에 상태를 정확히 유지하는 것이 어렵기 때문이다.         

#### 엔티티 클래스의 설계          

여기서 사용할 프로젝트는 앞의 설명을 구현하기 위해서 영화 데이터를 활용한다.         
다만 영화 데이터에 파일 업로드를 이용해서 영화의 이미지 파일 등을 업로드해 처리할 수 있도록 설계한다.           
프로젝트 내에 entity 패키지를 추가하고 이전에 사용했던 BaseEntity 클래스를 추가한다.            

프로젝트 생성 시에 만들어진 MreviewApplication에는 @EnableJpaAuditing을 추가한다.         

##### Movie 엔티티 클래스 설계          
M:N(다대다) 관계를 처리할 때는 반드시 매핑 테이블의 설계는 마지막 단계에서 처리하고,        
명사에 해당하는 클래스를 먼저 설계한다. 여기서는 영화와 회원의 존재가 명사에 해당하므로 이들을 먼저 엔티티 클래스로 설계한다.         

프로젝트에 entity 패키지 내에 Movie 클래스와 MovieImage 클래스를 추가한다. Movie 클래스는 BaseEntity 클래스를 상속해서 작성한다.        

```java
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Movie extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mno;

    private String title;
}
```

Movie 클래스는 단순하게 영화 제목(title)만을 가지는 구조로 최대한 단순하게 작성한다.         

##### MovieImage 클래스          

MovieImage 클래스는 이전에 작성했던 댓글과 거의 동일하다.         
단방향 참조로 처리할 것이고, @Query로 left join 등을 사용하게 된다. 이러한 작업을 할 때는 JPQL에서 엔티티 클래스인 경우에 사용이 자유롭다.        

> @ElementCollection이나 @Embeddable과 같이 엔티티가 아닌 값 객체Value Object를 이용하는 방법도 있다.        
> 다만 지금의 경우 페이지 처리가 조인 처리가 많으므로 Entity 타입으로 사용한다.         

```java
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = "movie")//연관 관계 주의
public class MovieImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inum;

    private String uuid;

    private String imgName;

    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    private Movie movie;
```

MovieImage 클래스에는 나중에 사용할 이미지에 대한 정보를 기록한다.      
java.util.UUID를 이용해서 고유한 번호를 생성해서 사용할 것이고, 이미지의 저장 경로(path)는 '년/월/일' 폴더 구조를 의미하게 된다.       
잠시 뒤에 테이블로 생성될 때는 movie 테이블이 PK를 가지고, movie_image 테이블은 FK를 가지게 되므로 @ManyToOne을 적용해서 이를 표시한다.

여기까지 작성된 상태에서 프로젝트를 실행하여 생성되는 테이블을 살펴본다.       

##### Member 클래스      
Member 클래스는 기존의 회원과 같은 역할을 하지만 회원과 로그인에 대한 처리는 다음 파트에서 스프링 시큐리티로 작성하고        
여기서는 고유한 번호, 이메일, 아이디와 패스워드, 닉네임을 의미하도록 클래스로 설계한다.        

```java
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Table(name="m_member")
public class Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mid;

    private String email;

    private String pw;

    private String nickname;
}
```
##### 매핑 테이블을 위한 Review 클래스 설계         
매핑 테이블은 주로 동사나 히스토리를 의미하는 테이블이다.       
여기서는 회원이 영화에 대해서 평점을 준다를 구성할 때 평점을 준다는 행위가 매핑 테이블이 필요한 부분이다.            
회원이라는 주어와 영화라는 목적어가 있지만 이에 대한 평점을 준다는 부분은 중간에서 주어와 목적어를 연결하는 매핑 테이블이 담당하게 된다.     
이 부분이 @ManyToMany를 이용하는 방식과의 차이이다. @ManyToMany의 경우 관계를 설정할 수 있지만 두 Entity 간의 추가적인 데이터를 기록할 수는 없다.          

매핑 테이블은 두 테이블 사이에서 양쪽의 PK를 참조하는 형태로 구성되기 때문에 movie_review는 @ManyToOne을 이용해서 양쪽을 참조하는 구조가 된다.          
단방향 참조의 경우 기본은 항상 외래키FK를 가지고 있는 테이블을 기준으로 설계한다.        

```java
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = {"movie", "member"})
public class Review extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewnum;

    @ManyToOne(fetch = FetchType.LAZY)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private int grade;

    private String text;
}
```

Review 클래스는 Movie와 Member를 양쪽으로 참조하는 구조이므로 @ManyToOne으로 설계한다.       
Fetch 모드는 모두 LAZY 설정으로 이용하고, toString() 호출 시에 다른 엔티티를 사용하지 않도록 @ToString에 exclude 속성을 지정한다.     

## M:N(다대다) Repository와 테스트          

테이블의 구성이 위와 같이 의도한 대로 생성되었다면 다음 단계로는 Repository를 구성하고, 필요한 데이터를 어떻게 구할 수 있는지 테스트 코드를 수행한다.     

#### Repository 작성과 테스트 데이터 추가하기       

프로젝트 내에 repository 패키지를 작성하고, MovieRepository, MovieImageRepository 인터페이스를 구성한다.       

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
}

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
}
```

test 폴더 내에 repository 패키지를 추가하고 MovieRepositoryTests 클래스를 추가한다.         

```java

@SpringBootTest
public class MovieRepositoryTests {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieImageRepository imageRepository;

    @Commit
    @Transactional
    @Test
    public void insertMovie(){
        IntStream.rangeClosed(1,100).forEach(i->{
            Movie movie = Movie.builder().title("Movie..."+i).build();
            System.out.println("-----------------");
            movieRepository.save(movie);
            int count = (int)(Math.random()*5)+1;

            for(int j=0; j<count; j++){
                MovieImage movieImage = MovieImage.builder()
                        .uuid(UUID.randomUUID().toString())
                        .movie(movie)
                        .imgName("test"+j+".jpg").build();

                imageRepository.save(movieImage);
            }
            System.out.println("========================");
        });
    }
}
```

영화와 영화의 이미지들은 같은 시점에 insert 처리가 되어야 한다. 때문에 Movie 객체를 우선 save() 해준다.      
save()가 실행된 뒤에는 Movie 객체는 PK에 해당하는 mno값이 할당되므로, 이를 이용해서 영화의 이미지들을 추가한다.          
영화의 이미지들은 최대 5개까지 임의로 저장된다. 특정한 영화는 이미지가 많을 수 있으므로 임의의 수로 처리한다.         

리뷰를 처리하기 위해서는 회원Member도 필요하다. 회원Member의 처리도 동일하게 MemberRepository를 구성한다.         
```java
public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

test 폴더에는 MemberRepositoryTests 클래스를 구성해서 100명의 회원Reviewer을 등록한다.        

```java

@SpringBootTest
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void insertMembers(){
        IntStream.rangeClosed(1,100).forEach(i->{
            Member member = Member.builder()
                    .email("r"+i+"@zerock.org")
                    .pw("1111")
                    .nickname("reviewer"+i).build();
            memberRepository.save(member);
        });
    }
}
```

##### 매핑 테이블 데이터 추가하기
영화Movie와 회원Member 양쪽 엔티티의 설계와 필요한 더미 데이터가 준비되었다면 매핑 테이블에 데이터를 추가할 수 있는 테스트 코드를 작성한다.          
ReviewRepository 인터페이스를 repository 패키지에 추가한다.        

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
```

앞의 테스트 코드 결과로 데이터베이스 내에는 100개의 영화와 100명의 회원이 존재하므로, 이를 이용해서 1~5점까지의 평점과 리뷰 내용을 등록하는 테스트 코드를 작성한다.           
test 폴더에는 이전과 동일하게 ReviewRepositoryTests 클래스를 작성한다.          

```java
@SpringBootTest
public class ReviewRepositoryTests {
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    public void insertMovieReviews(){
        IntStream.rangeClosed(1,200).forEach(i->{
            Long mno = (long)(Math.random()*100)+1;

            Long mid = ((long)(Math.random()*100)+1);
            Member member = Member.builder().mid(mid).build();

            Review movieReview = Review.builder()
                    .member(member)
                    .movie(Movie.builder().mno(mno).build())
                    .grade((int)(Math.random()*5)+1)
                    .text("이 영화에 대한 느낌.."+i)
                    .build();

            reviewRepository.save(movieReview);
        });
    }
}
```

insertMovieReviews()의 내용은 200개의 MovieReview를 저장한다. 영화의 번호와 회원은 임의의 값으로 현재 데이터베이스에 존재하는 값으로 생성해서 처리하고,        
영화의 평점과 리뷰의 내용을 작성해서 MovieReview 객체를 생성해서 저장한다.       
임의로 만들어진 데이터 중에는 중간에 리뷰가 없는 영화도 있다.     

#### 필요한 데이터 처리          
테스트 코드가 정상적으로 실행되었다면 현재 데이터베이스에는 100개의 영화와 해당 영화의 이미지 파일들, 100명의 회원, 200개의 영화 리뷰가 존재하게 된다.          
이 데이터를 이용해서 화면에서 필요한 데이터를 생각해보면 다음과 같은 내용이 필요하다.          

- 목록 화면에서 영화의 제목과 이미지 하나, 영화 리뷰의 평점/리뷰 개수를 출력         
- 영화 조회 화면에서 영화와 영화의 이미지들, 리뷰의 평균점수/리뷰 개수를 같이 출력         
- 리뷰에 대한 정보에는 회원의 이메일이나 닉네임과 같은 정보를 같이 출력           

데이터의 처리 방식은 이전과 유사하게 @Query를 주로 이용해서 처리한다. 다만 이번에는 @EntityGraph나 서브쿼리를 활용하는 예제를 학습한다.          

##### 페이지 처리되는 영화별 평균 점수/리뷰 개수 구하기          
영화의 목록 화며에서는 영화와 영화 이미지, 리뷰의 수, 평점 평균을 화면에 출력하고자 한다.        

현재 테이블 관계로 보면 영화와 영화 이미지는 일대다의 관계가 된다.          
JPQL에서 group by를 적용한다고 가정하면 리뷰의 개수와 리뷰의 평균 평점을 구할 수는 있다.         
우선 영화와 리뷰를 이용해서 페이징 처리를 하면 다음과 같이 구성할 수 있다.         

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("select m, avg(coalesce(r.grade, 0)), count(distinct r) from Movie m " +
            "left outer join Review r on r.movie=m group by m")
    Page<Object[]> getListPage(Pageable pageable);
}
```

MovieRepositoryTests 클래스에 테스트할 수 있는 메서드를 작성한다.        
```java
MovieRepositoryTests 클래스 일부

    @Test
    public void testListPage(){
        PageRequest pageRequest = PageRequest.of(0,10, Sort.by(Sort.Direction.DESC, "mno"));

        Page<Object[]> result = movieRepository.getListPage(pageRequest);

        for(Object[] objects : result.getContent()){
        System.out.println(Arrays.toString(objects));
        }
    }
```

테스트 코드가 실행될 때는 반드시 만들어지는 쿼리에 문제가 없는지 확인해야 한다.         
특히 전체 데이터에 대해서 처리하지 않는 페이징 관련된 부분이 실행되는지가 중요하다.       

```
Hibernate: 
    select
        movie0_.mno as col_0_0_,
        avg(coalesce(review1_.grade,
        0)) as col_1_0_,
        count(distinct review1_.reviewnum) as col_2_0_,
        movie0_.mno as mno1_1_,
        movie0_.moddate as moddate2_1_,
        movie0_.regdate as regdate3_1_,
        movie0_.title as title4_1_ 
    from
        movie movie0_ 
    left outer join
        review review1_ 
            on (
                review1_.movie_mno=movie0_.mno
            ) 
    group by
        movie0_.mno 
    order by
        movie0_.mno desc limit ?
Hibernate: 
    select
        count(movie0_.mno) as col_0_0_ 
    from
        movie movie0_ 
    left outer join
        review review1_ 
            on (
                review1_.movie_mno=movie0_.mno
            ) 
    group by
        movie0_.mno
[Movie(mno=100, title=Movie...100), 3.6667, 3]
[Movie(mno=99, title=Movie...99), 5.0, 1]
[Movie(mno=98, title=Movie...98), 5.0, 1]
[Movie(mno=97, title=Movie...97), 4.0, 1]
[Movie(mno=96, title=Movie...96), 2.6, 5]
[Movie(mno=95, title=Movie...95), 2.0, 2]
[Movie(mno=94, title=Movie...94), 3.6667, 3]
[Movie(mno=93, title=Movie...93), 2.6667, 3]
[Movie(mno=92, title=Movie...92), 0.0, 0]
[Movie(mno=91, title=Movie...91), 2.0, 3]
```

이와 같은 방식으로 중간에 영화 이미지도 같이 결합하면 될 것이라고 예상하고 아래와 같이 수정한다.      

```java
    @Query("select m, max(mi), avg(coalesce(r.grade, 0)), count(distinct r) from Movie m " +
        "left outer join MovieImage mi on mi.movie = m "+
        "left outer join Review r on r.movie=m group by m")
    Page<Object[]> getListPage(Pageable pageable);
```

위 코드를 실행하면 예상과 달리 각 영화마다 이미지를 찾는 쿼리가 실행되면서 비효율적으로 여러 번 실행되는 것을 볼 수 있다.     
중간에 10번 실행되는 쿼리는 아래와 같다.

```
Hibernate: 
    select
        movieimage0_.inum as inum1_2_0_,
        movieimage0_.img_name as img_name2_2_0_,
        movieimage0_.movie_mno as movie_mn5_2_0_,
        movieimage0_.path as path3_2_0_,
        movieimage0_.uuid as uuid4_2_0_ 
    from
        movie_image movieimage0_ 
    where
        movieimage0_.inum=?
```

쿼리의 내용을 보면 movie_image 테이블에서 해당하는 모든 영화의 이미지를 다 가져오는 쿼리이다.        
이러한 상황이 발생하는 이유는 목록을 가져오는 쿼리는 문제가 없지만, max()를 이용하는 부분이 들어가면서 해당 영화의 모든 이미지를 가져오는 쿼리가 실행된다.(N+1 문제)          

> N+1 문제는 다음과 같은 상황을 의미한다.      
> 1번의 쿼리로 N개의 데이터를 가져왔는데 N개의 데이터를 처리하기 위해서 필요한 추가적인 쿼리가 각 N개에 대해서 수행되는 상황이다.       
> 위의 경우 1페이지에 해당하는 10개의 데이터를 가져오는 쿼리 1번과 각 영화의 모든 이미지를 가져오기 위한 10번의 추가적인 쿼리가 실행되는 것이다.      
> 이렇게 되면 한 페이지를 볼 때마다 11번의 쿼리를 실행하기 때문에 성능에 문제가 생기므로 반드시 해결해야 한다.          

이 문제를 해결하는 간단한 방법은 중간의 이미지를 1개로 줄여서 처리하는 것이다.     
JPQL은 별도의 처리 없이 위의 구조를 작성할 수 있다.         

```html
    @Query("select m, mi, avg(coalesce(r.grade, 0)), count(distinct r) from Movie m " +
            "left outer join MovieImage mi on mi.movie = m "+
            "left outer join Review r on r.movie=m group by m")
    Page<Object[]> getListPage(Pageable pageable);
```

달라진 부분은 중간에 max() 처리를 없애고 MovieImage를 출력한다.        
변경된 코드를 다시 실행하면 중간에 반복적으로 실행되는 부분 없이, 목록을 구하는 쿼리와 개수를 구하는 쿼리만 실행된다.         
MariaDB에서 목록을 구하는 쿼리는 limit 구문이 정상 동작하는 것을 볼 수 있다.(항상 페이징 처리가 되는 부분에서는 limit 등이 정상적으로 실행되는지 확인한다.)       

```
Hibernate: 
    select
        movie0_.mno as col_0_0_,
        movieimage1_.inum as col_1_0_,
        avg(coalesce(review2_.grade,
        0)) as col_2_0_,
        count(distinct review2_.reviewnum) as col_3_0_,
        movie0_.mno as mno1_1_0_,
        movieimage1_.inum as inum1_2_1_,
        movie0_.moddate as moddate2_1_0_,
        movie0_.regdate as regdate3_1_0_,
        movie0_.title as title4_1_0_,
        movieimage1_.img_name as img_name2_2_1_,
        movieimage1_.movie_mno as movie_mn5_2_1_,
        movieimage1_.path as path3_2_1_,
        movieimage1_.uuid as uuid4_2_1_ 
    from
        movie movie0_ 
    left outer join
        movie_image movieimage1_ 
            on (
                movieimage1_.movie_mno=movie0_.mno
            ) 
    left outer join
        review review2_ 
            on (
                review2_.movie_mno=movie0_.mno
            ) 
    group by
        movie0_.mno 
    order by
        movie0_.mno desc limit ?
Hibernate: 
    select
        count(movie0_.mno) as col_0_0_ 
    from
        movie movie0_ 
    left outer join
        movie_image movieimage1_ 
            on (
                movieimage1_.movie_mno=movie0_.mno
            ) 
    left outer join
        review review2_ 
            on (
                review2_.movie_mno=movie0_.mno
            ) 
    group by
        movie0_.mno
[Movie(mno=100, title=Movie...100), MovieImage(inum=313, uuid=df9baf6d-63d9-4402-912d-446c3b57fb1d, imgName=test0.jpg, path=null), 3.6667, 3]
[Movie(mno=99, title=Movie...99), MovieImage(inum=312, uuid=d08320e1-f1fc-4011-aade-9d035dba93d5, imgName=test0.jpg, path=null), 5.0, 1]
[Movie(mno=98, title=Movie...98), MovieImage(inum=310, uuid=e4b767f0-01c2-47a8-bf58-f099a6b0b107, imgName=test0.jpg, path=null), 5.0, 1]
[Movie(mno=97, title=Movie...97), MovieImage(inum=307, uuid=691e55e9-877b-4998-afef-0ad9311713ad, imgName=test0.jpg, path=null), 4.0, 1]
[Movie(mno=96, title=Movie...96), MovieImage(inum=304, uuid=067727d0-da2f-4306-bdc8-3f88020affdc, imgName=test0.jpg, path=null), 2.6, 5]
[Movie(mno=95, title=Movie...95), MovieImage(inum=300, uuid=7adfc5fa-1615-4447-bdf5-ee1a41520298, imgName=test0.jpg, path=null), 2.0, 2]
[Movie(mno=94, title=Movie...94), MovieImage(inum=299, uuid=0f225c8a-25ea-4040-9a78-00e273ff786a, imgName=test0.jpg, path=null), 3.6667, 3]
[Movie(mno=93, title=Movie...93), MovieImage(inum=298, uuid=4a31a545-a365-4821-95c8-9cc34fcc89cc, imgName=test0.jpg, path=null), 2.6667, 3]
[Movie(mno=92, title=Movie...92), MovieImage(inum=294, uuid=c46060d9-d95d-4245-af90-1192dd23c465, imgName=test0.jpg, path=null), 0.0, 0]
[Movie(mno=91, title=Movie...91), MovieImage(inum=293, uuid=ab1e3609-8e0d-4ef9-ac3e-8be0f6e9535c, imgName=test0.jpg, path=null), 2.0, 3]
```

실행 결과의 Movie와 MovieImage는 가장 먼저 입력된 이미지 번호와 연결된다.        

##### 특정 영화의 모든 이미지와 평균 평점/리뷰 개수         
영화를 조회할 때는 영화뿐 아니라 해당 영화의 평균 평점/리뷰 개수를 화면에서 사용할 일이 있으므로 MovieRepository에 해당 기능을 추가한다.          

```
    @Query("select m, mi " +
            "from Movie m left outer join MovieImage mi on mi.movie = m " +
            "where m.mno = :mno")
    List<Object[]> getMovieWithAll(Long mno);
```

데이터베이스 내에 존재하는 review 테이블에서 리뷰의 수가 많은 영화를 선정한다.         
71번 영화는 5개의 리뷰와 5개의 이미지가 존재한다.         

MovieRepositoryTests에서는 getMovieWithAll()을 이용해서 테스트 코드를 작성한다.       

```java
    @Test
    public void testGetMovieWithAll(){
        List<Object[]> result = movieRepository.getMovieWithAll(71L);

        System.out.println(result);

        for(Object[] arr : result){
            System.out.println(Arrays.toString(arr));
        }
    }
```

위의 테스트 코드를 실행하면 다음과 같은 쿼리의 결과를 확인할 수 있다.       

```
Hibernate: 
    select
        movie0_.mno as mno1_1_0_,
        movieimage1_.inum as inum1_2_1_,
        movie0_.moddate as moddate2_1_0_,
        movie0_.regdate as regdate3_1_0_,
        movie0_.title as title4_1_0_,
        movieimage1_.img_name as img_name2_2_1_,
        movieimage1_.movie_mno as movie_mn5_2_1_,
        movieimage1_.path as path3_2_1_,
        movieimage1_.uuid as uuid4_2_1_ 
    from
        movie movie0_ 
    left outer join
        movie_image movieimage1_ 
            on (
                movieimage1_.movie_mno=movie0_.mno
            ) 
    where
        movie0_.mno=?
[[Ljava.lang.Object;@51a73873, [Ljava.lang.Object;@342a5b57, [Ljava.lang.Object;@5e781b4f, [Ljava.lang.Object;@1b89debc, [Ljava.lang.Object;@7305191e]
[Movie(mno=71, title=Movie...71), MovieImage(inum=224, uuid=e853069d-5f47-45c7-88ba-aa1f8d8b90c2, imgName=test0.jpg, path=null)]
[Movie(mno=71, title=Movie...71), MovieImage(inum=225, uuid=bb647eda-5468-42f1-995b-c6953cceb08b, imgName=test1.jpg, path=null)]
[Movie(mno=71, title=Movie...71), MovieImage(inum=226, uuid=f2ac1b04-7a30-4113-86b5-960876ef7d3b, imgName=test2.jpg, path=null)]
[Movie(mno=71, title=Movie...71), MovieImage(inum=227, uuid=9c64950b-90a0-459d-9a04-10fdef5a9d5b, imgName=test3.jpg, path=null)]
[Movie(mno=71, title=Movie...71), MovieImage(inum=228, uuid=30741b66-5759-4a23-9b6d-c8fba0aabcd1, imgName=test4.jpg, path=null)]
```

실행 결과를 보면 71번 영화에 5개의 이미지가 존재하므로, 위 결과는 정상이다.        
리뷰와 관련된 내용 처리는 left join을 이용하면 된다. 리뷰와 조인한 후에 count(), avg() 등의 함수를 이용하게 되는데 이때 영화 이미지별로 group by를 실행해야만 한다.        

```java
MovieRepository의 getMovieWithAll() 수정

    @Query("select m, mi, avg(coalesce(r.grade, 0)), count(r) " +
        "from Movie m left outer join MovieImage mi on mi.movie = m " +
        "left outer join Review r on r.movie = m " +
        "where m.mno = :mno group by mi")
    List<Object[]> getMovieWithAll(Long mno);
```

변경된 부분은 left outer join이 추가되었고 마지막의 group by 부분에 영화 이미지별로 그룹을 만들어서 영화 이미지들의 개수만큼 데이터를 만들어 낼 수 있게 된다.       
변경된 코드를 테스트하면 다음과 같은 결과를 얻을 수 있다.          

```
Hibernate: 
    select
        movie0_.mno as col_0_0_,
        movieimage1_.inum as col_1_0_,
        avg(coalesce(review2_.grade,
        0)) as col_2_0_,
        count(review2_.reviewnum) as col_3_0_,
        movie0_.mno as mno1_1_0_,
        movieimage1_.inum as inum1_2_1_,
        movie0_.moddate as moddate2_1_0_,
        movie0_.regdate as regdate3_1_0_,
        movie0_.title as title4_1_0_,
        movieimage1_.img_name as img_name2_2_1_,
        movieimage1_.movie_mno as movie_mn5_2_1_,
        movieimage1_.path as path3_2_1_,
        movieimage1_.uuid as uuid4_2_1_ 
    from
        movie movie0_ 
    left outer join
        movie_image movieimage1_ 
            on (
                movieimage1_.movie_mno=movie0_.mno
            ) 
    left outer join
        review review2_ 
            on (
                review2_.movie_mno=movie0_.mno
            ) 
    where
        movie0_.mno=? 
    group by
        movieimage1_.inum
[[Ljava.lang.Object;@7db63a01, [Ljava.lang.Object;@423791, [Ljava.lang.Object;@3aac31b7, [Ljava.lang.Object;@57def953, [Ljava.lang.Object;@1bf4a79b]
[Movie(mno=71, title=Movie...71), MovieImage(inum=224, uuid=e853069d-5f47-45c7-88ba-aa1f8d8b90c2, imgName=test0.jpg, path=null), 3.8, 5]
[Movie(mno=71, title=Movie...71), MovieImage(inum=225, uuid=bb647eda-5468-42f1-995b-c6953cceb08b, imgName=test1.jpg, path=null), 3.8, 5]
[Movie(mno=71, title=Movie...71), MovieImage(inum=226, uuid=f2ac1b04-7a30-4113-86b5-960876ef7d3b, imgName=test2.jpg, path=null), 3.8, 5]
[Movie(mno=71, title=Movie...71), MovieImage(inum=227, uuid=9c64950b-90a0-459d-9a04-10fdef5a9d5b, imgName=test3.jpg, path=null), 3.8, 5]
[Movie(mno=71, title=Movie...71), MovieImage(inum=228, uuid=30741b66-5759-4a23-9b6d-c8fba0aabcd1, imgName=test4.jpg, path=null), 3.8, 5]
```

실행 결과의 마지막을 보면 해당 영화의 리뷰 점수 평균이 3.8이고, 리뷰 개수는 5개라는 것을 알 수 있다.         

##### 특정 영화의 모든 리뷰와 회원의 닉네임         

영화를 조회하는 화면에 들어가면 당연히 영화에 대한 영화 리뷰를 조회할 수 있고, 자신이 영화에 대한 영화 리뷰를 등록하거나 수정/삭제할 수 있어야 한다.         
이를 위해서 우선적으로 필요한 데이터는 특정한 영화 번호를 이용해서 해당 영화를 리뷰한 정보이다.        
특정 영화에 대한 영화 리뷰는 ReviewRepository에 다음과 같이 작성할 수 있다.         

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovie(Movie movie);
}
```

findByMovie()에 대한 테스트 코드를 작성해서 Review에서 필요한 데이터를 추출합니다.(Review가 존재하는 영화 번호로 테스트한다.)          

```java
    @Test
    public void testGetMovieReviews(){
        Movie movie = Movie.builder().mno(71L).build();

        List<Review> result = reviewRepository.findByMovie(movie);

        result.forEach(movieReview->{
            System.out.println(movieReview.getReviewnum());
            System.out.println("\t"+movieReview.getGrade());
            System.out.println("\t"+movieReview.getText());
            System.out.println("\t"+movieReview.getMember().getEmail());
            System.out.println("---------------------------");
        });
    }
```

testGetMovieReviews()를 테스트해 보면 문제가 발생하는 것을 확인할 수 있다.       
```
could not initialize proxy [org.zerock.mreview.entity.Member#71] - no Session
org.hibernate.LazyInitializationException: could not initialize proxy [org.zerock.mreview.entity.Member#71] - no Session
	at app//org.hibernate.proxy.AbstractLazyInitializer.initialize(AbstractLazyInitializer.java:176)
	at app//org.hibernate.proxy.AbstractLazyInitializer.getImplementation(AbstractLazyInitializer.java:322)
	at app//org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor.intercept(ByteBuddyInterceptor.java:45)
	at app//org.hibernate.proxy.ProxyConfiguration$InterceptorDispatcher.intercept(ProxyConfiguration.java:95)
	at app//org.zerock.mreview.entity.Member$HibernateProxy$WNGp3VSM.getEmail(Unknown Source)
```

이것은 Review 클래스의 Member에 대한 Fetch 방식이 LAZY 이기 때문에 한 번에 Review객체와 Member 객체를 조회할 수 없어 발생하는 문제이다.           
@Transactional을 적용한다 해도 각 Review 객체의 getMember(), getEmail()을 처리할 때마다 Member객체를 로딩해야 하는 문제가 있다.        
이 문제를 해결할 수 있는 방법으로는 크게 1) @Query를 이용해서 조인 처리를 하거나, 2) @EntityGraph를 이용해서 Review 객체를 가져올 때 Member 객체를 로딩하는 방법이 있다.          

@EntityGraph는 Entity의 특정한 속성을 같이 로딩하도록 표시하는 어노테이션이다.      
기본적으로 JPA를 이용하는 경우에는 연관 관계의 FATCH 속성값은 LAZY로 지정하는 것이 일반적이다.          
@EntityGraph는 이러한 상황에서 특정 기능을 수행할 때만 EAGER 로딩을 하도록 지정할 수 있다.         

@EntityGraph는 attributePaths 속성과 type 속성을 가지고 있다.          
- attributePaths: 로딩 설정을 변경하고 싶은 속성의 이름을 배열로 명시한다.           
- type: @EntityGraph를 어떤 방식으로 적용할 것인지를 설정한다.            
- FATCH 속성값은 attributePaths에 명시한 속성은 EAGER로 처리하고, 나머지는 LAZY로 처리한다.           
- LOAD 속성값은 attributePaths에 명시한 속성은 EAGER로 처리하고, 나머지는 Entity 클래스에 명시되거나 기본 방식으로 처리한다.           

Review를 처리할 때 @EntityGraph를 적용해서 Member도 같이 로딩할 수 있도록 변경한다.          

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Review> findByMovie(Movie movie);
}
```

테스트 코드를 실행해서 결과를 보면 자동으로 조인 처리가 된 것을 확인할 수 있다.           

```
Hibernate: 
    select
        review0_.reviewnum as reviewnu1_3_0_,
        member1_.mid as mid1_0_1_,
        review0_.moddate as moddate2_3_0_,
        review0_.regdate as regdate3_3_0_,
        review0_.grade as grade4_3_0_,
        review0_.member_mid as member_m6_3_0_,
        review0_.movie_mno as movie_mn7_3_0_,
        review0_.text as text5_3_0_,
        member1_.moddate as moddate2_0_1_,
        member1_.regdate as regdate3_0_1_,
        member1_.email as email4_0_1_,
        member1_.nickname as nickname5_0_1_,
        member1_.pw as pw6_0_1_ 
    from
        review review0_ 
    left outer join
        m_member member1_ 
            on review0_.member_mid=member1_.mid 
    where
        review0_.movie_mno=?
10	5	이 영화에 대한 느낌..10	r71@zerock.org---------------------------
95	4	이 영화에 대한 느낌..95	r86@zerock.org---------------------------
130	4	이 영화에 대한 느낌..130	r63@zerock.org---------------------------
135	4	이 영화에 대한 느낌..135	r25@zerock.org---------------------------
161	2	이 영화에 대한 느낌..161	r47@zerock.org---------------------------

```

위의  경우 영화 71번에 대한 리뷰가 5건인 상황이고 별도의 추가적인 쿼리가 수행되지 않는 것을 볼 수 있다.           

##### 회원 삭제 문제와 트랜잭션 처리            
M:N(다대다)의 관계를 현재와 같이 별도의 매핑 테이블을 구성하고 이를 엔티티로 처리하는 경우에 주의해야 한다.         
왜냐하면 명사에 해당하는 데이터를 삭제하는 경우 중간에 매핑 테이블에서도 삭제를 해야 하기 때문이다.          
특정한 회원을 삭제하는 경우 해당 회원이 등록한 모든 영화 리뷰 역시 삭제되어야 한다.          
그러므로 m_member 테이블에서 특정 회원을 삭제하려면 우선은 review 테이블에서 먼저 삭제하고, m_member 테이블에서 삭제해야 한다.            
이 2개의 작업은 하나의 트랜잭션으로 관리될 필요가 있다. ReviewRepository에는 회원을 이용해서 삭제하는 메서드를 추가한다.           

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Review> findByMovie(Movie movie);

    void deleteByMember(Member member);
}
```

MemberRepository는 JpaRepository의 기능만으로 삭제가 가능하므로 추가할 메서드는 없다.            
MemberRepositoryTests에는 ReviewRepository를 추가로 주입하고 테스트 코드를 작성한다.           

```java
    @Test
    public void testDeleteMember(){
        Long mid = 1L;

        Member member = Member.builder().mid(mid).build();

        memberRepository.deleteById(mid);
        reviewRepository.deleteByMember(member);
    }
```

위의 테스트 메서드를 실행하면 1번 회원이 작성한 Review가 있는 경우에는 다음과 같은 에러가 나는 것을 볼 수 있다.        
FK로 참조하고 있는 상태이기 때문에 PK쪽을 먼저 삭제할 수 없다.          

```
could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement
org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement
	at app//org.springframework.orm.jpa.vendor.HibernateJpaDialect.convertHibernateAccessException(HibernateJpaDialect.java:276)
	at app//org.springframework.orm.jpa.vendor.HibernateJpaDialect.translateExceptionIfPossible(HibernateJpaDialect.java:233)
```

에러의 원인은 1)FK를 가지는 Review 쪽을 먼저 삭제하지 않았고, 2) 트랜잭션 관련 처리가 없기 때문이다.           
따라서 삭제하는 단계를 FK 쪽을 먼저 삭제하도록 수정하고 메서드의 선언부에 @Transactional과 @Commit을 추가한다.          

```java
    @Commit
    @Transactional
    @Test
    public void testDeleteMember(){
        Long mid = 1L;

        Member member = Member.builder().mid(mid).build();

        reviewRepository.deleteByMember(member);
        memberRepository.deleteById(mid);
    }
```

위의 코드는 정상적으로 동작하기는 하지만 조금 비효율적인 SQL들이 실행되는 것을 볼 수 있다.          
예를 들어 review 테이블에 1번 회원이 작성한 영화 리뷰가 총 2개가 있다면 review 테이블에서 2번 반복적으로 실행된 후에 m_member 테이블을 삭제한다.     

``` 
Hibernate: 
    select
        review0_.reviewnum as reviewnu1_3_,
        review0_.moddate as moddate2_3_,
        review0_.regdate as regdate3_3_,
        review0_.grade as grade4_3_,
        review0_.member_mid as member_m6_3_,
        review0_.movie_mno as movie_mn7_3_,
        review0_.text as text5_3_ 
    from
        review review0_ 
    where
        review0_.member_mid=?
Hibernate: 
    select
        member0_.mid as mid1_0_0_,
        member0_.moddate as moddate2_0_0_,
        member0_.regdate as regdate3_0_0_,
        member0_.email as email4_0_0_,
        member0_.nickname as nickname5_0_0_,
        member0_.pw as pw6_0_0_ 
    from
        m_member member0_ 
    where
        member0_.mid=?
Hibernate: 
    delete 
    from
        review 
    where
        reviewnum=?
Hibernate: 
    delete 
    from
        m_member 
    where
        mid=?

```

이러한 비효율을 막기 위해서는 ReviewRepository에서 @Query를 이용해서 where 절을 지정하는 것이 더 나은 방법이다.       
update나 delete를 이용하기 위해서는 @Modifying 어노테이션이 반드시 필요하다.          

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Review> findByMovie(Movie movie);

    @Modifying
    @Query("delete from Review mr where mr.member = :member")
    void deleteByMember(Member member);
}
```

실행된 결과는 동일하지만 위와 같이 @Query를 적용한 후에는 한 번에 review 테이블에서 삭제가 된다.       
