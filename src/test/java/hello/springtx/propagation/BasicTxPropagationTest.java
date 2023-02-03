package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;


@Slf4j
@SpringBootTest
public class BasicTxPropagationTest {

    @Autowired
    PlatformTransactionManager transactionManager;

    @TestConfiguration
    static class Config {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

    }

    @Test
    void separateCommit() {
        log.info("트랜잭션1 시작");
        TransactionStatus status1 = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션1 커밋");
        transactionManager.commit(status1);

        // 트랜잭션1을 커밋한 후에 또다른 트랜잭션2를 시작했으므로 둘은 완전히 별개의 트랜잭션임

        log.info("트랜잭션2 시작");
        TransactionStatus status2 = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션2 커밋");
        transactionManager.commit(status2);
    }

    @Test
    void separateCommitAndRollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus status1 = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션1 커밋");
        transactionManager.commit(status1);

        log.info("트랜잭션2 시작");
        TransactionStatus status2 = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션2 롤백");
        transactionManager.rollback(status2);
    }

    @DisplayName("외부 커밋 & 내부 커밋")
    @Test
    void outerCommitAndInnerCommit() {
        // 논리 구조 상 외부라고 한 것이지, 그냥 먼저 실행된 트랜잭션이라고 보면 됨
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute());
        // isNewTransaction: 해당 트랜잭션이 먼저 실행된 다른 트랜잭션에 참여하는 트랜잭션이 아닌, 새로 생성된 트랜잭션인지 여부
        log.info("outer: 새로 시작된 트랜잭션인가?={}", outerStatus.isNewTransaction());

        // 이전에 시작한 트랜잭션을 아직 커밋이나 롤백하지 않았는데, 또다른 트랜잭션을 시작함 (=내부 트랜잭션)
        log.info("내부 트랜잭션 시작");
        TransactionStatus innerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute()); // 새로 트랜잭션을 생성하지 않고, 기존 트랜잭션에 편승함
        log.info("inner: 새로 시작된 트랜잭션인가?={}", innerStatus.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        transactionManager.commit(innerStatus); // 로그로 확인해보면 아무일도 일어나지 않음

        log.info("외부 트랜잭션 커밋");
        transactionManager.commit(outerStatus); // 이 때 비로소 커밋함

        /*
        트랜잭션 참여?
        내부 트랜잭션이 먼저 시작된 외부 트랜잭션과 하나의 물리 트랜잭션으로 묶이는 것을 의미함.
        외부 트랜잭션이 내부 트랜잭션의 범위까지 커버한다는 뜻

        내부 트랜잭션은 시작할 때, 외부 트랜잭션에 참여한다는 로그만 남기고, 커밋을 호출하면 아무 로그도 남기지 않음
        만약 내부 트랜잭션이 커밋이나 롤백을 했을 때 반영이 된다면 바로 트랜잭션이 종료되어버리기 때문에, 외부 트랜잭션의 커밋이나 롤백이 호출되기 전까지는 아무일도 일어나지 않음
        오직 외부 트랜잭션만이 트랜잭션을 시작하고 최종적으로 커밋/롤백함 (물리 트랜잭션을 관리하는 주체 = 외부 트랜잭션)

        묶인 하나의 물리 트랜잭션은 그 안에 존재하는 논리 트랜잭션이 하나라도 롤백되면 전체가 롤백됨
        이 경우에는 둘 다 커밋을 했으므로 전체 물리 트랜잭션이 커밋됨
         */
    }

    @DisplayName("외부 롤백 & 내부 커밋")
    @Test
    void outerRollbackAndInnerCommit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus innerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 커밋");
        transactionManager.commit(innerStatus);

        log.info("외부 트랜잭션 롤백");
        transactionManager.rollback(outerStatus);
    }

    @DisplayName("외부 커밋 & 내부 롤백")
    @Test
    void outerCommitAndInnerRollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus innerStatus = transactionManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 롤백");
        transactionManager.rollback(innerStatus);
        /*
        참여 중인 트랜잭션에 rollbackOnly 라고 마킹함
        정확하게는 트랜잭션 매니저가 트랜잭션 동기화 매니저에게 현재 트랜잭션을 진행중인 커넥션이 rollbackOnly=true 라고 지정해주는 것
         */

        log.info("외부 트랜잭션 커밋");
        assertThatThrownBy(() -> transactionManager.commit(outerStatus))
                .isInstanceOf(UnexpectedRollbackException.class);
        /*
         트랜잭션 매니저는 DB 커넥션에 물리 커밋을 호출하기 전에 트랜잭션 동기화 매니저가 가진 rollbackOnly 옵션을 체크함. 이 경우에는 내부 트랜잭션에 의해 이미 true로 마킹되었으므로 커밋 대신 롤백함.
         그런데 이 경우, 커밋을 기대했는데도 실제로는 롤백되었다는 것을 명확히 고지해줄 필요가 있어 스프링이 UnexpectedRollbackException을 던져줌.
         */
    }
}
