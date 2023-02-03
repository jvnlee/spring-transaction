package hello.springtx.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final LogRepository logRepository;

    @Transactional
    public void joinV1(String username) {
        Member member = new Member(username);
        Log logEntity = new Log(username);

        log.info("=== MemberRepository 호출 ===");
        memberRepository.save(member);

        log.info("=== LogRepository 호출 ===");
        logRepository.save(logEntity);
    }

    @Transactional
    public void joinV2(String username) {
        Member member = new Member(username);
        Log logEntity = new Log(username);

        log.info("=== MemberRepository 호출 ===");
        memberRepository.save(member);

        // 로그 저장이 회원 저장에 지장을 주지 않게 하기 위해 런타임 예외를 잡아서 처리
        try {
            log.info("=== LogRepository 호출 ===");
            logRepository.save(logEntity);
        } catch (RuntimeException e) {
            log.info("log 저장에 실패했습니다. logMessage={}", logEntity.getMessage());
        }
    }

}
