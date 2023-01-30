package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallTestV2 {

    @Autowired
    CallService callService;

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallConfig {

        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }

    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        public void external() {
            log.info("call external()");
            TxUtil.printTxInfo();
            internalService.internal();
        }

    }

    static class InternalService {

        @Transactional
        public void internal() {
            log.info("call internal()");
            TxUtil.printTxInfo();
        }

    }

    static class TxUtil {

        public static void printTxInfo() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive={}", isTxActive);
        }

    }

}
