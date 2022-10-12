package com.study.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.query.entity.Member;
import com.study.query.entity.QMember;
import com.study.query.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static com.study.query.entity.QMember.member;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    // 테스트를 들어가기전에 세팅.
    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라
        Member findByMember = em.createQuery("select m from Member.m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findByMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl(){
        // JPAQueryFactory queryFactory = new JPAQueryFactory(em); 필드레벨로 가져가도 된다.(동시성 문제 걱정X - 설계가 되어있음)

        // 기본 Q-Type 활용
        // 1. QMember m = new QMember("m");
        // 2. QMember m = QMember.member
        // 3. QMember.member static import사용 (alt+enter)


        // querydsl은 결국 JPQL빌더 역할이다.(콘솔창에서 JPQL을 확인하고 싶다면 yml파일에 추가)

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리를 해줌.
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                // 위에 and랑 동일.
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
