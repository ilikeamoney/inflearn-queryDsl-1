package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity() throws Exception {
        //given
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

        // 초기화
        em.flush();
        em.clear();
        //when
        List<Member> findMembers = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        //then
        for (Member findMember : findMembers) {
            System.out.println("findMember.getUsername() = " + findMember.getUsername());
        }
    }

    @Test
    public void aa() throws Exception {
        //given

        int a = 97;
        for (int i = 0; i < 27; i++) {
            System.out.println((char) a + i);
        }

        //when

        //then
    }

}