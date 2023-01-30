package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService levelService;

    @Test
    void priorityTest() {
        levelService.write();
        levelService.read();
    }

    @TestConfiguration
    static class TxLevelConfig {

        @Bean
        LevelService levelService() {
            return new LevelService();
        }

    }

    @Slf4j
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 (쓰기 불가)
    static class LevelService {

        @Transactional(readOnly = false)
        public void write() {
            log.info("call write()");
            printTxInfo();
        }

        public void read() {
            log.info("call read()");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive={}", isTxActive);

            boolean isTxReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isTxReadOnly={}", isTxReadOnly);
        }

    }
}
