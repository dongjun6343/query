package com.study.query.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 기본적으로 롤백을 해준다.
//@Commit 테스트할때 값이 있어서 꼬일수있음.
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new  Member("member1", 10, teamA);
        Member member2 = new  Member("member2", 10, teamA);
        Member member3 = new  Member("member3", 10, teamB);
        Member member4 = new  Member("member4", 10, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        em.flush(); // 영속성 컨텍스트에 있는 object를 디비에 날림.
        em.clear();

        //확인.
        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();

    }
}