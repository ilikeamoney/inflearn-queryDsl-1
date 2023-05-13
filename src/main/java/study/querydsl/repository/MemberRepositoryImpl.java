package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }


    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
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
                .where(memberCond(condition.getUsername(),
                        condition.getTeamName(),
                        condition.getAgeGoe(),
                        condition.getAgeLoe()))
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .join(member.team, team)
                .where(memberCond(condition.getUsername(),
                        condition.getTeamName(),
                        condition.getAgeGoe(),
                        condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> contents = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .join(member.team, team)
                .where(memberCond(condition.getUsername(),
                        condition.getTeamName(),
                        condition.getAgeGoe(),
                        condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(memberCond(condition.getUsername(),
                        condition.getTeamName(),
                        condition.getAgeGoe(),
                        condition.getAgeLoe())
                );

//        return new PageImpl<>(contents, pageable, total);
        return PageableExecutionUtils.getPage(contents, pageable, () -> countQuery.fetchCount());
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
