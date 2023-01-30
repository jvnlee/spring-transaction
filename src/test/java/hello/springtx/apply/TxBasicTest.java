package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
public class TxBasicTest {

    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {
        log.info("aop class={}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxBasicConfig {

        /*
        실제로는 BasicService가 아닌, BasicService를 상속 받아 생성된 프록시 객체가 스프링 컨테이너에 빈으로 등록됨.
        클래스 혹은 메서드 레벨에 하나라도 @Transactional이 붙어있으면 스프링 트랜잭션 AOP가 읽어들여 프록시 객체를 생성하기 때문.
        이렇게 컨테이너에 등록된 프록시 객체는 트랜잭션 처리 로직을 수행하다가 실제 비즈니스 로직이 처리되어야할 부분에서는 원본 클래스의 메서드를 호출함.
         */
        @Bean
        BasicService basicService() {
            return new BasicService();
        }

    }

    @Slf4j
    static class BasicService {

        /*
        클라이언트에서 tx()가 호출되면, 프록시 객체의 tx()가 호출되는데 이 메서드는 @Transactional이 있으므로
        트랜잭션 로직이 실행되고, 이어서 원본 객체의 nonTx()가 호출됨.
         */
        @Transactional
        public void tx() {
            log.info("call tx()");
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive={}", isTxActive);
        }

        /*
        클라이언트에서 nonTx()가 호출되면, 프록시 객체의 nonTx()가 호출되는데 이 메서드는 @Transactional이 없으므로
        트랜잭션 로직이 실행되지 않고 곧장 원본 객체의 nonTx()가 호출됨.
         */
        public void nonTx() {
            log.info("call nonTx()");
            boolean isTxActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isTxActive={}", isTxActive);
        }

    }
}
