package study.querydsl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

@Component
@Profile("local")
public class InitData {
    @Autowired
    HelloInitData initData;

    @PostConstruct
    public void init() {
        initData.dataSave();
    }

    @Component
    @Transactional
    static class HelloInitData {
        @Autowired
        EntityManager em;

        public void dataSave() {
            int startCount = 97;
            Member member = new Member(null);

            for (int i = 0; i < 2 ; i++) {
                changeAlphabet(member, startCount);
            }
        }

        public <T> void changeAlphabet(T param, int startCount) {
            if (param instanceof Member) {
                for (int i = 0; i <= 51; i++) {
                    char alphabet = (char) (startCount + i);
                    if (alphabet > 122) {
                        alphabet = 0;
                        alphabet += 39 + i;
                    }
                    Member member = new Member("member " + alphabet);
                    Team team = new Team("Team " + alphabet);
                    em.persist(team);
                    member.setAge(18 + i);
                    member.setTeam(team);
                    em.persist(member);
                }
            }
        }


    }
}
