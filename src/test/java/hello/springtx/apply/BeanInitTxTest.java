package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

@SpringBootTest
public class BeanInitTxTest {

    @Autowired
    SomeBean someBean;

    @Test
    void postConstructTx() {
        /*
        실행 흐름
        1. 빈으로 등록하기 위한 SomeBean 인스턴스 생성 및 초기화
        2. @PostConstruct가 붙은 initV1() 실행 (트랜잭션 AOP 적용 X)
        3. 스프링 컨테이너 세팅 완료
        4. ApplicationReadyEvent에 반응하는 initV2() 실행 (트랜잭션 AOP 적용 O)

        빈의 초기화 코드가 먼저 호출되고 나서 트랜잭션 AOP가 적용되기 때문에 initV1()에서는 트랜잭션을 사용할 수 없음.
        모든 빈과 컨테이너의 초기화가 완료된 상태에서는 트랜잭션 적용이 가능하기 때문에 initV2()처럼 ApplicationReadyEvent 발생 시 호출되도록 하면 됨.
         */
    }

    @TestConfiguration
    static class BeanInitTxConfig {

        @Bean
        SomeBean someBean() {
            return new SomeBean();
        }

    }

    @Slf4j
    static class SomeBean {

        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("initV1 isTxActive={}", isTxActive);
        }

        // ApplicationReadyEvent: 스프링 컨테이너 및 모든 스프링 빈 초기화가 완료된 상태를 의미
        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("initV2 isTxActive={}", isTxActive);
        }

    }

}
