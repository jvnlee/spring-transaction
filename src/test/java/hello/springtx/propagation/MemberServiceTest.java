package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.UnexpectedRollbackException;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    @Autowired
    MemberService memberService;

    @BeforeEach
    void beforeEach() {
        memberRepository.clear();
        logRepository.clear();
    }

    // Service의 joinV1()에 @Transactional 사용 X
    @Test
    void nonTx_joinV1() {
        String username = "nonTx_joinV1";

        memberService.joinV1(username);

        assertThat(memberRepository.findByUsername(username)).isPresent();
        assertThat(logRepository.findByMessage(username)).isPresent();
    }

    // Service의 joinV1()에 @Transactional 사용 X
    @Test
    void nonTx_joinV1_ex() {
        String username = "로그예외_nonTx_joinV1_ex";

        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        assertThat(memberRepository.findByUsername(username)).isPresent();
        assertThat(logRepository.findByMessage(username)).isEmpty();
        // 회원 저장은 커밋되었지만, 로그 저장은 롤백됨 = 데이터 정합성 문제 발생
    }

    // Service의 joinV1()에 @Transactional 사용 O
    @Test
    void tx_joinV1_ex() {
        String username = "로그예외_tx_joinV1_ex";

        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        assertThat(memberRepository.findByUsername(username)).isEmpty();
        assertThat(logRepository.findByMessage(username)).isEmpty();
        // joinV1()의 내부 트랜잭션인 로그 트랜잭션이 롤백되면서 회원 트랜잭션도 함께 전체 롤백됨 = 데이터 정합성 문제 해소
    }

    // Service의 joinV2()에 @Transactional 사용 O
    @Test
    void tx_joinV2_ex() {
        String username = "로그예외_tx_joinV2_ex";

        // LogRepository에서 올라온 런타임 예외를 catch 해서 복구한 로직이 담긴 joinV2() 사용
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        assertThat(memberRepository.findByUsername(username)).isEmpty();
        assertThat(logRepository.findByMessage(username)).isEmpty();
        /*
        joinV2()는 예외를 복구해서 정상흐름으로 되돌려놓았으니까 물리 트랜잭션도 커밋되어야 할 것 같지만,
        실제로는 이미 LogRepository의 save 트랜잭션에서 rollbackOnly가 마킹되어 전체 트랜잭션도 롤백이 됨
        rollbackOnly가 true인 상태에서 물리 트랜잭션 커밋을 시도하면 UnexpectedRollbackException이 발생함
         */
    }

    /*
    Service의 joinV2()에 @Transactional 사용 O
    LogRepository의 save()에 @Transactional(propagation = Propagation.REQUIRES_NEW) 옵션 사용
     */
    @Test
    void tx_joinV2_ex_requiresNew() {
        String username = "로그예외_tx_joinV2_ex_requiresNew";

        memberService.joinV2(username);

        assertThat(memberRepository.findByUsername(username)).isPresent();
        assertThat(logRepository.findByMessage(username)).isEmpty();
        /*
        로그 트랜잭션은 이제 REQUIRES_NEW 옵션에 의해 별개의 신규 트랜잭션으로 취급되므로 거기서 예외가 발생하면 해당 트랜잭션만 롤백됨.
        joinV2()로 넘어온 예외는 catch 되어 정상 흐름으로 전환됨. 그러면 joinV2()가 정상 종료되면서 커밋이 호출되고, 회원 트랜잭션은 정상적으로 커밋이 완료됨.
        즉, 서비스 트랜잭션에서 회원과 로그 저장을 모두 호출할 때, 로그가 롤백되더라도 회원은 저장되게 하려면 이렇게 REQUIRES_NEW 옵션을 사용하면 됨.

        (주의)
        그러나 REQUIRES_NEW는 하나의 HTTP 요청(쓰레드)이 2개의 트랜잭션을 생성하기 위해 커넥션도 2개를 사용하게 되므로, 커넥션 풀의 자원을
        평소보다 2배로 소모하는 결과를 초래할 수 있음. 따라서 성능 이슈가 따르는 기능이라면 이 부분을 유념하고 있어야 함.

        REQUIRES_NEW를 사용하는 방법 외에도,
        1. MemberService의 앞단에 MemberFacade 클래스를 두고, MemberFacade가 LogRepository는 직접 따로 호출하게 변경하기 (순차적 트랜잭션)
        2. LogRepository의 save()에서 체크 예외를 던지기
        3. LogRepository의 save()에 noRollbackFor 옵션을 추가해 언체크 예외가 발생해도 커밋되도록 설정하기
         */
    }

}