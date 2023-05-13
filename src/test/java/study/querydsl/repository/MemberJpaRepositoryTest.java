package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;
    
    @Test
    public void repositoryTest() throws Exception {
        //given
        Member member = new Member("member0");
        memberJpaRepository.save(member);

        //when
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        List<Member> findMembers = memberJpaRepository.findAll_Querydsl();
        List<Member> byUsername = memberJpaRepository.findByUsername_QueryDsl(member.getUsername());

        //then
        Assertions.assertThat(member).isEqualTo(findMember);
        Assertions.assertThat(findMembers).contains(member);
        Assertions.assertThat(byUsername).contains(member);
    }
    
    @Test
    public void searchTest() throws Exception {
        //given
        Team teamWow = new Team("teamWow");
        em.persist(teamWow);
        Member user1 = new Member("user1", 23);
        user1.changeTeam(teamWow);
        em.persist(user1);

        em.flush();
        em.clear();

        MemberSearchCondition searchCond = new MemberSearchCondition();

        searchCond.setAgeGoe(20);
        searchCond.setAgeLoe(30);
        searchCond.setTeamName("Team");

        //when
        List<MemberTeamDto> result1 = memberJpaRepository.searchNotNullEx(searchCond);
        List<MemberTeamDto> result2 = memberJpaRepository.searchNullEx(searchCond);

        //then
        // 필드 속성을 추출해서 검증하는 방법
        Assertions.assertThat(result1).extracting("username").contains("member m");
        Assertions.assertThat(result2).extracting("username").contains("member m");
    }

}