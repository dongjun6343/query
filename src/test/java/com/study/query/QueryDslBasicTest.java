package com.study.query;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.query.entity.Member;
import com.study.query.entity.QMember;
import com.study.query.entity.QTeam;
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
import static com.study.query.entity.QTeam.team;

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

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory.selectFrom(member).fetch();

        Member member = queryFactory.selectFrom(QMember.member).fetchOne();

        Member fetchFirst = queryFactory.selectFrom(QMember.member).fetchFirst();

        // fetch권장으로 변경.
        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(QMember.member).fetchResults();

        memberQueryResults.getTotal();
    }

    /**
     * 정렬
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 3. 단, 2에서 회원이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> fetch = queryFactory.selectFrom(member).where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member memberNull = fetch.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(zero index)
                .limit(2) //최대 2건 조회
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple team1 = result.get(0);
        Tuple team2 = result.get(1);

        Assertions.assertThat(team1.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(team1.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

        Assertions.assertThat(team2.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(team2.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
    }

    /**
     * 팀 A에 소속된 모든 회원을 조회하라.
     */
    @Test
    public void team(){
        List<Member> result = queryFactory.selectFrom(member)
                // 멤버랑 팀을 조인.
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }

    /**
     * 연관관계가 없는 필드로 조인. (cross join? 조인 후 where절로 정리.)
     *  ==> 세타조인
     *
     *  회원의 이름이 팀 이름과 같은 회원 조회.
     */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     * join on절
     * 1. 조인 대상 필터링.
     * 2. 연관관계 없는 엔티티 외부 조인. (이떄 많이 쓰임)
     *
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회.
     * JPQL : select m, t from member m left join m.team t on t.name = 'teamA';
     */
    @Test
    public void join_on(){

        // select가 여러가지 타입이라서 tuple로 나옴.
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
    }
}