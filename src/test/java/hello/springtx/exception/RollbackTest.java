package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService rollbackService;

    @Test
    void uncheckedExTest() {
        assertThatThrownBy(() -> rollbackService.uncheckedEx())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedExTest() {
        assertThatThrownBy(() -> rollbackService.checkedEx())
                .isInstanceOf(MyException.class);
    }

    @Test
    void checkedExRollbackForTest() {
        assertThatThrownBy(() -> rollbackService.checkedExRollbackFor())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackConfig {

        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }

    }

    @Slf4j
    static class RollbackService {

        // 언체크 예외(런타임 예외) 발생 -> 롤백
        @Transactional
        public void uncheckedEx() {
            log.info("call uncheckedEx()");
            throw new RuntimeException();
        }

        // 체크 예외 발생 -> 커밋
        @Transactional
        public void checkedEx() throws MyException {
            log.info("call checkedEx()");
            throw new MyException();
        }

        // 체크 예외 발생했지만 롤백 지정
        @Transactional(rollbackFor = MyException.class)
        public void checkedExRollbackFor() throws MyException {
            log.info("call checkedExRollbackFor()");
            throw new MyException();
        }

    }

    static class MyException extends Exception {
    }

}
