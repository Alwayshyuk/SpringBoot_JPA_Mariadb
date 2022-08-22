package org.zerock.ex2.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.zerock.ex2.entity.Memo;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@SpringBootTest
public class MemoRepositoryTests {
    @Autowired
    MemoRepository memoRepository;

    @Test
    public void testClass(){
        System.out.println(memoRepository.getClass().getName());
    }
    @Test
    public void testInsertDummies(){

        IntStream.rangeClosed(1,100).forEach(i -> {
            Memo memo = Memo.builder().memoText("Sample..."+i).build();
            memoRepository.save(memo);
        });
    }
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
    @Transactional
    @Test
    public void testSelect2(){

        //데이터베이스에 존재하는 mno
        Long mno = 100L;

        Memo memo = memoRepository.getOne(mno);
        System.out.println("=========================");

        System.out.println(memo);
    }
    @Test
    public void testUpdate(){
        Memo memo = Memo.builder().mno(100L).memoText("update Text").build();

        System.out.println(memoRepository.save(memo));
    }
    @Test
    public void testDelete(){
        Long mno = 100L;
        memoRepository.deleteById(mno);
    }

    @Test
    public void testPageDefault(){
        Pageable pageable = PageRequest.of(0,10);
        Page<Memo> result = memoRepository.findAll(pageable);

        System.out.println(result);
        System.out.println("-------------------------------");
        System.out.println("Total Pages:"+result.getTotalPages());
        System.out.println("Total count:"+result.getTotalElements());
        System.out.println("Page Number:"+result.getNumber());
        System.out.println("Page Size:"+result.getSize());
        System.out.println("has next page:"+result.hasNext());
        System.out.println("first page?:"+result.isFirst());

        for(Memo memo:result.getContent()){
            System.out.println(memo);
        }
    }
    @Test
    public void testSort(){
        Sort sort1 = Sort.by("mno").descending();

        Pageable pageable = PageRequest.of(0,10,sort1);
        Page<Memo> result = memoRepository.findAll(pageable);

        result.get().forEach(memo ->{
            System.out.println(memo);
        });

        Sort sort2 = Sort.by("mno").descending();
        Sort sort3 = Sort.by("memoText").ascending();
        Sort sortAll = sort2.and(sort3);

        Pageable pageable1 = PageRequest.of(0,10,sortAll);
    }
    @Test
    public void testQueryMethods(){
        List<Memo> list = memoRepository.findByMnoBetweenOrderByMnoDesc(70L, 80L);
        for(Memo memo : list){
            System.out.println(memo);
        }
    }
    @Test
    public void testQueryMethodWithPageable(){
        Pageable pageable = PageRequest.of(0,10,Sort.by("mno").descending());
        Page<Memo> result = memoRepository.findByMnoBetween(10L, 50L, pageable);
        result.get().forEach(memo -> System.out.println(memo));
    }
    @Commit
    @Transactional
    @Test
    public void testDeleteQueryMethods(){
        memoRepository.deleteMemoByMnoLessThan(10L);
    }
}
