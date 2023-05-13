package study.querydsl;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberQueryRepositoryTest {

    @Autowired
    MemberQueryRepository repository;

    @Autowired
    EntityManager em;

    @Test
    public void testRepository() throws Exception {
        //given

        //when
        Member memberA = repository.joinTeam("teamA");

        //then
        Assertions.assertThat(memberA).isNotNull();

    }

}