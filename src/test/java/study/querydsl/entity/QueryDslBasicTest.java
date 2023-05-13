package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory query;

    @BeforeEach
    public void before() {
        //given
        query = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 20, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJpql() throws Exception {
        // member1 찾기
        String qlString = "select m from Member m" +
                " where m.username = :username";

        Member findByJpql = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJpql.getUsername()).isEqualTo("member1");
    }

    @Test
    public void select() throws Exception {
        Member findMember = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() throws Exception {
        Member findMember = query
                .selectFrom(member)
                // and 쓴 경우
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam() throws Exception {

        // 조건을 넣어 검색할때는 eq like 사용

        Member findMember = query
                .selectFrom(member)
                // and 를 쓰지 않은 경우
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetch() throws Exception {

//        List<Member> members = query
//                .selectFrom(member)
//                .fetch();
//
//        Member member1 = query
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member member2 = query
//                .selectFrom(QMember.member)
//                .fetchFirst();

        // Page query 처럼 사용
//        QueryResults<Member> results = query
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults();

        // select query 를 count 로 변환
        long count = query
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
     */
    @Test
    public void sort() throws Exception {

        // 정렬 조건이 필요할때는 order by 를 사용한다

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));


        List<Member> findMember = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = findMember.get(0);
        Member member6 = findMember.get(1);
        Member nullMember = findMember.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(nullMember.getUsername()).isNull();
    }

    @Test
    public void paging() throws Exception {
        List<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() throws Exception {
        QueryResults<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(60);
        assertThat(tuple.get(member.age.avg())).isEqualTo(15);
        assertThat(tuple.get(member.age.max())).isEqualTo(20);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * join 문 사용하는 방법
     */
    @Test
    public void groupBy() throws Exception {
        List<Tuple> result = query
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(15);

    }

    /**
     * teamA에 소속된 모든 회원
     * 연관관계로 인해 member 에서 team 을 참조 할 수 있음
     * 그래서 join 으로 팀명이 teamA 데이터인 member 데이터를 가져옴
     */
    @Test
    public void join() throws Exception {
        //given
        List<Member> findMember = query
                .selectFrom(member)
                .join(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();
        //when
        Member member1 = findMember.get(0);
        Member member2 = findMember.get(1);

        //then
        assertThat(member1.getUsername()).isEqualTo("member1");
        assertThat(member2.getUsername()).isEqualTo("member2");
        assertThat(findMember.size()).isEqualTo(2);
    }

    /**
     * 세타 조인
     * 즉 member, team 의 데이터를 모두 조회해서
     * 값이 맞으면 그 데이터를 가져온다.
     * 이것은 from 절에서 두 엔티티가 서로 조인하지 않고 사용한다.
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = query.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result.get(0).getUsername()).isEqualTo("teamA");
        assertThat(result.get(1).getUsername()).isEqualTo("teamB");
        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL select m, t from Member m left join m.team t on t.name = "teamA"
     */
    @Test
    public void join_on_filter() throws Exception {
        //given
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //when
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        
        //then
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        //given
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //when
        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).isFalse();

        //then
    }

    @Test
    public void fetchJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();

        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //when
        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).isTrue();

        //then
    }

    /**
     * 나이가 가장 많은 회원 조회
     * 서브 쿼리 사용하는 방법
     */
    @Test
    public void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();


        //when
        assertThat(result.get(0).getAge()).isEqualTo(20);
        
        //then
    }

    /**
     * 나이가 평균 이상인 회원
     * 서브 쿼리 사용하는 방법
     */
    @Test
    public void subQueryGOE() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        //when
        assertThat(result.get(0).getAge()).isGreaterThan(15);

        //then
    }

    /**
     * 나이가 평균 이상인 회원
     * 서브 쿼리 사용하는 방법
     */
    @Test
    public void subQueryIN() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        //when
        assertThat(result.get(0).getAge()).isGreaterThanOrEqualTo(20);

        //then
    }
    
    @Test
    public void selectSubQuery() throws Exception {
        //given

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = query
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        //when

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        
        //then
    }
    
    @Test
    public void basicCase() throws Exception {
        //given
        List<String> result = query.select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("늙은이"))
                .from(member)
                .fetch();
        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }
        
        //then
    }
    
    @Test
    public void complexCase() throws Exception {
        //given
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20 살")
                        .when(member.age.between(21, 30)).then("아저씨")
                        .otherwise("늙은이"))
                .from(member)
                .fetch();

        //when
        for (String s : result) {
            System.out.println("s = " + s);
        }
        
        //then
    }
    
    @Test
    public void constant() throws Exception {
        //given
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        //when
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        
        //then
    }
    
    @Test
    public void concat() throws Exception {
        //given
        List<String> findMember = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        //when
        for (String s : findMember) {
            System.out.println("s = " + s);
        }
        //then
    }

    @Test
    public void simpleProjection() throws Exception {
        //given
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        for (String username : result) {
            System.out.println("username = " + username);
        }

        //when
        
        //then
    }

    /**
     * 테이블에 원하는 데이터를 찝어 오는 것을 Projection 이라고 한다.
     */
    @Test
    public void tupleProjection() throws Exception {
        //given
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        //when

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

        //then
    }


    /**
     * JPQL에서 지원하는 new Operation 문법
     * @throws Exception
     */
    @Test
    public void findDtoJPQL() throws Exception {
        //given
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        //when

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto.getUsername() = " + memberDto.getUsername());
            System.out.println("memberDto.getAge() = " + memberDto.getAge());
        }
        
        //then
    }

    /**
     * queryDsl properties projection 방식
     * 이 방식은 Setter 가 있어야 가능한 방식이다.
     */
    @Test
    public void findDtoQuerydslSetter() throws Exception {
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class, // class Type, select values
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl Field projections 방식
     * 이 방식은 Setter 가 없어도 필드에 접근해서 값을 셋팅해서 반환한다.
     */
    @Test
    public void findDtoQueryDslFields() throws Exception {
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl Constructor projection 방식
     * 생성자에 넣을 값의 타입이 맞아야 호출된다.
     */
    @Test
    public void findDtoQueryDslConstructor() throws Exception {
        List<MemberDto> result = query.select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * queryDsl Constructor projection 방식을 사용 할때 주의점
     * 만약에 Dto 필드 변수와 엔티티의 필드 변수 값의 이름이 다르다면
     * as("Dto 필드 이름") 으로 바꿔야 정상적인 조회가 된다.
     * 그렇지 않으면 이름이 맞지않는 필드는 null로 들어간다.
     */
    @Test
    public void findUserDtoError() throws Exception {

        List<UserDto> result = query.select(Projections.fields(UserDto.class,
//                        member.username.as("name"), // null
                        member.username.as("name"), // ok
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * @QueryProjection 사용 방법
     * Dto 생성자에 @QueryProjection 을 붙이고
     * Gradle 에서 compileQuerydsl 을 누르면 generated 파일에 Q 파일로 Dto class 가 생성된다.
     * select 절에서 바로 new QDto() 생성해서 가져오면 된다.
     * 이 방식의 장점은 compile 시점에 오류를 잡을 수 있는 것이 장점이다.
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String username = "member b";
        Integer ageParam = null;

        List<Member> result = searchMember1(username, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    /**
     * BooleanBuilder 사용하는 방법
     * new BooleanBuilder 생성해서
     * and() 에 조건을 추가한다.
     * 모든 조건을 다 넣었으면 where 문에 변수 builder 를 넣는다.
     */
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query.
                selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * Builder 를 생성하는 것이 아닌
     * 참 거짓 조건을 넣어 where 문을 완성하는 로직
     * 조건을 만드는 메서드 반환타입을 BooleanExpression 을 사용하면 원하는 조건으로 조립을 할 수 있다.
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameCond = "member c";
        Integer ageCond = 25;

        List<Member> result = searchMember2(null, ageCond);
        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * 메서드를 많이 정의해서 나름 복잡해 보이고 귀찮아 보이지만
     * 메인 메서드에서 쉽게 그 로직의 의도를 바로 파악할 수 있으므로 가독성이 좋다.
     */
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
                .where(memberValid(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        // 조건이 간단하면 삼항 연산자를 사용
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression memberValid(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    
    @Test
    public void bulkUpdate() throws Exception {
        String usernameCond = "비회원";

        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(40))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = usernameEq1(usernameCond);

        for (Member findMember : result) {
            if (findMember.getUsername().equals("비회원")) {
                System.out.println("findMember = " + findMember);
            }
        }
        assertThat(count).isEqualTo(result.size());
    }

    private List<Member> usernameEq1(String usernameCond) {
        return query.selectFrom(member)
                .where(eqCondUsername(usernameCond))
                .fetch();
    }

    private BooleanExpression eqCondUsername(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    
    @Test
    public void bulkAdd() throws Exception {
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

    }

    @Test
    public void bulkDelete() throws Exception {
        long count = query
                .delete(member)
                .where(member.age.goe(25))
                .execute();
    }

    @Test
    public void sqlFunction() throws Exception {
        List<String> result = query
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFuction2() throws Exception {
        List<String> result = query
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate(
//                        "function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
