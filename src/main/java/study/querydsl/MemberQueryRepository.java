package study.querydsl;


import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberQueryRepository {

    private final JPAQueryFactory query;

    @Autowired
    public MemberQueryRepository(EntityManager em) {
        query = new JPAQueryFactory(em);
    }

    // 조회
    public List<Member> ageAvgGT(int age) {
        return query
                .selectFrom(member)
                .where(member.age.avg().gt(age))
                .fetch();
    }

    public List<Member> ageAvgLt(int age) {
        return query
                .selectFrom(member)
                .where(member.age.avg().lt(age))
                .fetch();
    }

    public Member joinTeam(String teamName) {
        return query
                .selectFrom(member)
                .join(member.team, team)
                .where(member.team.name.eq(teamName))
                .fetchOne();
    }
}
