package hello.springtx.apply;

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
public class InternalCallTest {

    @Autowired
    CallService callService;

    @Test
    void internalCall() {
        callService.internal();
    }

    @Test
    void externalCall() {
        callService.external();
        /*
        문제 상황:
        external()을 호출하면 메서드 내부에 internal()을 호출하는 부분이 있는데 internal()에는 @Transactional이 붙어있는데도
        트랜잭션 관련 로그도 찍히지 않고, isTxActive도 false로 나옴. 실무에서 이런 구조의 코드를 작성하면 트랜잭션 처리를 의도했는데도
        아무일도 일어나지 않는 치명적인 결과가 나올 수 있으므로 주의해야함.

        동작 흐름:
        1. 클라이언트에서 external() 호출
        2. 실제로는 CallService의 프록시 객체가 가진 external()이 호출됨
        3. external()은 트랜잭션 적용 대상이 아니라서(@Transactional 없음) 곧바로 원본 CallService 객체의 external()이 호출됨
        4. external() 내부에서 로직이 수행되다가 internal()이 호출됨 (= this.internal())
        5. 프록시 객체의 internal()이 호출된 것이 아닌 현재 객체(원본)의 internal()이 호출되었으므로 트랜잭션 관련 코드는 아무것도 없음
        6. 트랜잭션 처리 없이 internal()의 로직이 수행되고 종료
         */
    }

    @TestConfiguration
    static class InternalCallConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }

    }

    @Slf4j
    static class CallService {

        public void external() {
            log.info("call external()");
            printTxInfo();
            internal(); // 주의!!!
        }

        @Transactional
        public void internal() {
            log.info("call internal()");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive={}", isTxActive);
        }

    }

}
