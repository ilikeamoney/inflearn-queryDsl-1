package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_QueryDsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername())); // 문자열과 같은
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.contains(condition.getTeamName())); // 문자열을 포함하는
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> searchNotNullEx(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .join(member.team, team)
                .where(usernameCond(condition.getUsername()),
                        teamNameCond(condition.getTeamName()),
                        ageGoeCond(condition.getAgeGoe()),
                        ageLoeCond(condition.getAgeLoe()))
                .fetch();
    }

    public List<MemberTeamDto> searchNullEx(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .join(member.team, team)
                .where(
                        memberCond(condition.getUsername(),
                                condition.getTeamName(),
                                condition.getAgeGoe(),
                                condition.getAgeLoe()))
                .fetch();
    }

    public BooleanExpression usernameCond(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    public BooleanExpression teamNameCond(String teamName) {
        return hasText(teamName) ? team.name.contains(teamName) : null;
    }

    public BooleanExpression ageGoeCond(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    public BooleanExpression ageLoeCond(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    public BooleanExpression memberCond(String username, String teamName, Integer ageGoe, Integer ageLoe) {
        BooleanExpression usernameCond = usernameCond(username);
        BooleanExpression teamNameCond = teamNameCond(teamName);
        BooleanExpression ageGoeCond = ageGoeCond(ageGoe);
        BooleanExpression ageLoeCond = ageLoeCond(ageLoe);

        List<BooleanExpression> expressions = new ArrayList<>();
        if (usernameCond != null) {
            expressions.add(usernameCond);
        }
        if (teamNameCond != null) {
            expressions.add(teamNameCond);
        }
        if (ageGoeCond != null) {
            expressions.add(ageGoeCond);
        }
        if (ageLoeCond != null) {
            expressions.add(ageLoeCond);
        }

        return Expressions.allOf(expressions.toArray(new BooleanExpression[0]));
    }

}
