package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import study.querydsl.entity.TeamRepository;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;

    @BeforeEach
    public void saveMember() {
        for (int i = 0; i < 40; i++) {
            Team team = new Team("team" + i);
            teamRepository.save(team);
            Member member = new Member("member" + i, 20 + i, team);
            memberRepository.save(member);
        }
    }

    @Test
    public void repositoryTest() throws Exception {
        //given
        Member member = new Member("member0");
        memberRepository.save(member);

        //when
        Member findMember = memberRepository.findById(member.getId()).get();
        List<Member> findMembers = memberRepository.findAll();
        List<Member> byUsername = memberRepository.findByUsername(member.getUsername());

        //then
        Assertions.assertThat(member).isEqualTo(findMember);
        Assertions.assertThat(findMembers).contains(member);
        Assertions.assertThat(byUsername).contains(member);
    }

    @Test
    public void dynamicQueryTest() throws Exception {
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20);
        condition.setAgeLoe(30);

        //when
        List<MemberTeamDto> result = memberRepository.search(condition);

        //then
        Assertions.assertThat(result).extracting("username").contains("member10");
    }
    
    @Test
    public void searchPageSimpleTest() throws Exception {
        //given
        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 10);
        
        //when
        Page<MemberTeamDto> result = memberRepository.searchSimple(condition, pageRequest);

        //then
        Assertions.assertThat(result.getSize()).isEqualTo(10);
        Assertions.assertThat(result.getContent()).extracting("username").contains("member1", "member2", "member3");
    }

    @Test
    public void queryDslPredicateExcuteTest() throws Exception {
        //given
        Iterable<Member> findMembers = memberRepository.findAll(
                member.age.between(20, 40)
                .and(member.username.contains("member")));

        //when
        for (Member findMember : findMembers) {
            System.out.println("findMember = " + findMember);
        }

        //then
    }

}