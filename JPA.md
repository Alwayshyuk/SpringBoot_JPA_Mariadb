## Spring Data JPA의 소개        
JPA(Java Persistence API)는 Java 언어를 통해서 데이터베이스와 같은 영속 계층을 처리하고자 하는 스펙이다.      
JPA를 이해하기 위해서는 ORM(Object-Relational-Mapping)이라는 기술에 대해서 먼저 알아야 한다.     

#### ORM과 JPA         
ORM(Object Relational Mapping)은 단어에서 보듯이 객체지향과 관련이 있다.     
ORM은 간단히 말하자면 '객체지향 패러다임을 관계형 데이터베이스에 보존하는 기술'이라고 할 수 있다.       
패러다임 입장에서 생각하자면 '객체지향 패러다임을 관계형 패러다임으로 매핑해 주는 개념'이라고 볼 수 있다.     

ORM은 '객체지향'의 구조가 '관계형 데이터베이스'와 유사하다는 점에서 시작한다.     
새로운 데이터 베이스 테이블에서 칼럼을 정의하고 칼럼에 맞는 데이터 타입을 지정해서 데이터를 보관하는 틀을 만든다는 의미에서      
클래스와 상당히 유사하다.     

클래스와 테이블이 유사하듯이 '인스턴스'와 'Row(레코드 혹은 튜플)'도 상당히 유사하다.     
객체지향에서는 클래스에서 인스턴스를 생성해서 인스턴스라는 '공간'에 데이터를 보관하는데,     
테이블에서는 하나의 'Row'에 데이터를 저장하게 된다.     
여기서 유일한 차이는 '객체'라는 단어가 '데이터 + 행위(메서드)'라는 의미라면,     
'Row'는 '데이터'만을 의미한다는 점이다.(데이터베이스에서는 개체(entity)라는 용어를 사용한다.)

'관계'와 '참조(reference)'라는 의미도 유사하다. 관계형 데이터베이스는 테이블 사이의 관계를 통해서 구조적인 데이터를 표현한다면,     
객체지향에서는 '참조'를 통해서 어떤 객체가 다른 객체들과 어떤 관계를 맺고 있는 지를 표현한다.     

위와 같은 특징에 기초해서 '객체지향을 자동으로 관계형 데이터베이스에 맞게'처리해 주는 기법에 대해서 아이디어를 내기 시작했고,      
그것이 ORM의 시작이었습니다.

ORM은 완전히 새로운 패러다임을 주장하는 것이 아니라 '객체지향'과 '관계형' 사이의 변환 기법을 의마하는 것입니다.     
따라서 특정 언어에 국한되는 개념이 아니고, 관계형 패러다임을 가지고 있다면 데이터베이스의 종류를 구분하지 않는다.     
현실적으로 이미 여러 객체지향을 지원하는 언어에서 ORM을 위한 여러 프레임워크들이 존재하고 있다.    

JPA는 'Java Persistence API'의 약어로 ORM을 Java 언어에 맞게 사용하는 '스펙'이다.    
따라서 ORM이 좀 더 상위 개념이 되고, JPA는 Java라는 언어에 국한된 개념으로 불 수 있다.     


#### Spring Data JPA와 JPA       
스프링 부트는 JPA의 구현체 중에서 Hibernate라는 구현체를 이용한다.     
Hibernate는 오픈소스로 ORM을 지원하는 프레임워크이다.    
다른 프레임워크도 그러하지만, Hibernate는 단독으로 프로젝트에 적용이 가능한 독립 프레임워크이다.     
때문에 스프링 부트가 아닌 스프링만 이용한다고 해도 Hibernate와 연동해서 JPA를 사용할 수 있다.     

프로젝트 생성 시에 추가한 Spring Data JPA는 Hibernate를 스프링 부트에서 쉽게 사용할 수 있는 추가적인 API들을 제공한다.     
스프링 프레임워크 자체가 대부분의 다른 프레임워크와의 호환성을 위한 라이브러리를 제공하는 경우가 많은데      
'Spring Data JPA' 역시 이러한 예이다.     


## 엔티티 클래스와 JpaRepository        
Spring Data JPA가 개발에 필요한 것은 단지 두 종류의 코드만으로 가능하다.    

> JPA를 통해서 관리하게 되는 객체(Entity Object)를 위한 엔티티 클래스      
> 엔티티 객체들을 처리하는 기능을 가진 Repository     

이 중에서 Repository는 Spring Data JPA에서 제공하는 인터페이스로 설계하는데     
스프링 내부에서 자동으로 객체를 생성하고 실행하는 구조라 개발자 입장에서는     
단순히 인터페이스를 하나 정의하는 작업만으로도 충분하다.     
(기존의 Hibernate는 모든 코드를 직접 구현하고 트랜잭션 처리가 필요했지만, Spring Data JPA는     
자동으로 생성되는 코드를 이용하므로 단순 CRUD나 페이지 처리 등의 개발에 코드를 개발하지 않아도 된다.)     

#### 엔티티 클래스의 작성      

###### @Entity      
엔티티 클래스는 Spring Data JPA에서는 반드시 @Entity라는 어노테이션을 추가해야만 한다.     
@Entity는 해당 클래스가 엔티티를 위한 클래스이며, 해당 클래스의 인스턴스들이 JPA로 관리되는 엔티티 객체라는 것을 의미한다.     
또한 @Entity가 붙은 클래스는 옵션에 따라서 자동으로 테이블을 생성할 수도 있다.      
이 경우 @Entity가 있는 클래스의 멤버 변수에 따라서 자동으로 칼럼들도 생성된다.     

###### @Table     
@Entity 어노테이션과 같이 사용할 수 있는 어노테이션으로 말 그대로 데이터베이스 상에서 엔티티 클래스를      
어떠한 테이블로 생성할 것인지에 대한 정보를 담기 위한 어노테이션이다.    
예를 들어 @Table(name="t_memo")와 같이 지정하는 경우에는 생성되는 테이블의 이름이 't_memo'테이블로 생성된다.     
단순히 테이블의 이름뿐만 아니라 인덱스등을 생성하는 설정도 가능하다.     

###### @Id와 @GeneratedValue    
@Entity가 붙은 클래스는 Primary Key에 해당하는 특정 필드를 @Id로 지정해야한 한다.    
@Id가 사용자가 입력하는 값을 사용하는 경우가 아니면 자동으로 생성되는      
번호를 사용하기 위해서 @GeneratedValue라는 어노테이션을 활용한다.     

@GeneratedValue(stratgy=GenerationType.IDENTITY)부분은 PK를 자동으로 생성하고자 할 때 사용한다.(키 생성 전략이라고 한다)     
만일 연결되는 데이터베이스가 오라클이면 별도의 변호를 위한 별도의 테이블을 생성하고,     
MySQL이나 MariaDB면 'auto increment'를 기본으로 사용해서 새로운 레코드가 기록될 때 마다 다른 번호를 가질 수 있도록 처리된다.    

키 생성 전략은 크게 다음과 같다.     
> AUTO(default) - JPA 구현체(스프링 부트에서는 Hibernate)가 생성 방식을 결정      
> IDENTITY - 사용하는 데이터베이스가 키 생성을 결정 MySQL이나 MariaDB의 경우 auto increment 방식을 이용      
> SEQUENCE - 데이터베이스의 sequence를 이용해서 키를 생성. @SequenceGenerator와 같이 사용      
> TABLE - 키 생성 전용 테이블을 생성해서 키 생성. @TableGenerator와 함께 사용     

###### @Column     
만일 추가적인 필드(칼럼)가 필요한 경우에도 마찬가지로 어노테이션을 활용한다.       
이 때는 @Column을 이용해서 다양한 속성을 지정할 수 있다.      
주로 nullable, name, length 등을 이용해서 데이터베이스의 칼럼에 필요한 정보를 제공한다.       
속성 중에 columnDefinition을 이용하면 기본값을 지정할 수도 있습니다.     

#### JpaRepository 인터페이스
앞에서 언급했듯이 Spring Data JPA는 JPA의 구현체인 Hibernate를 이용하기 위한 여러 API를 제공한다.        
그 중 개발자가 가장 많이 사용할 것은 JpaRespository라는 인터페이스이다.       
Spring Data JPA에는 여러 종류의 인터페이스의 기능을 통해서 JPA관련 작업을 별도의 코드없이 처리할 수 있게 지원한다.     
예를 들어 CRUD작업이나 페이징, 정렬 등의 처리도 인터페이스의 메서드를 호출하는 형태로 처리하는데       
기능에 따라서 상속 구조로 추가적인 기능을 제공한다.    

일반적인 기능만을 사용할 때는 CrudRepository를 사용하는 것이 좋고,     
모든 JPA관련 기능을 사용하고 싶을 때는 JpaRepository를 이용하지만 특별한 경우가 아니라면     
JpaRepository를 이용하는 것이 가장 무난한 선택이다.

###### JpaRepository 사용하기
JpaRepository는 인터페이스이고, Spring Data JPA는 이를 상속하는 인터페이스를 선언하는 것만으로도    
모든 처리가 끝난다. 실제 동작 시에는 스프링이 내부적으로 해당 인터페이스에 맞는 코드를 생성하는 방식으로 이용한다.     

JpaRepository의 경우 다음과 같은 메서드를 활용한다.      
> insert 작업: save(엔티티 객체)     
> select 작업: findById(키 타입), getOne(키 타입)     
> update 작업: save(엔티티 객체)      
> delete 작업: deleteById(키 타입), delete(엔티티 객체)      

특이하게 insert와 update 작업에 사용하는 메서드가 동일하게 save()를 이용하는데      
이는 JPA의 구현체가 메모리상(Entity Manager라는 존재가 엔티티들을 관리하는 방식)에서     
객체를 비교하고 없다면 insert, 존재한다면 update를 동작시키는 방식으로 동작하기 때문이다.

###### 등록 작업 테스트        

```java
    @Test
    public void testInsertDummies(){

        IntStream.rangeClosed(1,100).forEach(i -> {
            Memo memo = Memo.builder().memoText("Sample..."+i).build();
            memoRepository.save(memo);
        });
    }
```

testInsertDummies()의 내용은 100개의 새로운 Memo 객체를 생성하고 MemoRepository를 이용해서 이를 insert하는 것이다.      
테스트가 실행되는 과정에는 JPA의 구현체인 Hibernate가 발생하는 insert 구문을 확인할 수 있다.     

###### 조회 작업 테스트        
조회 작업의 테스트는 findById()나 getOne()을 이용해서 엔티티 객체를 조회할 수 있다.     
findById()와 getOne()은 동작하는 방식이 조금 다른데,      
데이터베이스를 먼저 이용하는지 나중에 필요한 순간까지 미루는 방식을 이용하는지에 대한 차이가 있다.      

```java
    @Test
    public void testSelect(){
        Long mno = 100L;

        Optional<Memo> result = memoRepository.findById(mno);
        System.out.println("=========================");
        if(result.isPresent()){
            Memo memo = result.get();
            System.out.println(memo);
        }
    }
```
findById()의 경우 java.util 패키지의 Optional 타입으로 변환되기 때문에     
한번 더 결과가 존재하는지를 체크하는 형태로 작성하게 된다.    
실행 결과를 보면 findById()를 실행한 순간에 이미 SQL은 처리가 되었고 '==='부분은 SQL 처리 후에 실행된 것을 볼 수 있다.     


getOne()의 경우는 조금 다른 방식으로 동작하는데 @Transactioal 어노테이션이 추가로 필요하다.    
> @Transactional은 트랜잭션 처리를 위해 사용하는 어노테이션이다.      

```java
    @Transactional
    @Test
    public void testSelect2(){

        //데이터베이스에 존재하는 mno
        Long mno = 100L;

        Memo memo = memoRepository.getOne(mno);
        System.out.println("=========================");

        System.out.println(memo);

    }
```

getOne()의 경우 리턴 값은 해당 객체이지만, 실제 객체가 필요한 순간까지 SQL을 실행하지는 않는다.     

###### 수정 작업 테스트      

수정 작업은 등록 작업과 동일하게 save()를 이용해서 처리한다.     
내부적으로 해당 엔티티의 @Id값이 일치하는 지를 확인해서 insert 혹은 update 작업을 처리한다.     

```java
    @Test
    public void testUpdate(){
        Memo memo = Memo.builder().mno(100L).memoText("update Text").build();

        System.out.println(memoRepository.save(memo));
    }
```

JPA는 엔티티 객체들을 메모리상에 보관하려고 하기 때문에 특정한 엔티티 객체가 존재하는지 확인하는 select가 먼저 실행되고       
해당 @Id를 가진 엔티티 객체가 있다면 update, 그렇지 않다면 insert를 실행하게 됩니다.      

###### 삭제 작업 테스트
삭제하려는 번호의 엔티티 객체가 있는지 먼저 확인하고, 이를 삭제한다.

```java
    @Test
    public void testDelete(){
        Long mno = 100L;
        memoRepository.deleteById(mno);
    }
```

deleteByid()의 리턴 타입은 void이고 만일 해당 데이터가 존재하지 않으면       
org.springframework.dao.EmptyResultDataAccessException 예외가 발생한다.     

#### 페이징/정렬 처리하기
JPA는 오라클의 인라인 뷰와 같은 처리를 내부적으로 Dialect라는 존재를 이용해서 처리한다.
JPA가 실제 데이터베이스에서 사용하는 SQL의 처리를 자동으로 하기 때문에 개발자들은 SQL이 아닌      
API의 객체와 메서드를 사용하는 형태로 페이징 처리를 할 수 있게 된다.    

Spring Data JPA에서 페이징 처리와 정렬은 findAll()이라는 메서드를 사용한다.     
findAll()은 JpaRepository 인터페이스의 상위인 PagingAndSortRepository의 메서드로     
파라미터로 전달되는 Pageable이라는 타입의 객체에 의해서 실행되는 쿼리를 결정하게 된다.     
단 한가지 주의할 점은 리턴 타입을 Page<T> 타입으로 지정하는 경우에는 반드시 파라미터를 Pageable 타입을 이용해야 한다는 점이다.     

###### pageable 인터페이스
