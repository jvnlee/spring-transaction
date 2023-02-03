package hello.springtx.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogRepository {

    private final EntityManager em;

//    @Transactional
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Transactional(noRollbackFor = {RuntimeException.class})
    public void save(Log logEntity) {
        log.info("log 저장");

        if (logEntity.getMessage().contains("로그예외")) {
            log.info("log 저장 도중 예외 발생");
            throw new RuntimeException("런타임 예외 발생"); // 롤백 유도
//            throw new Exception("체크 예외 발생");
        }

        em.persist(logEntity);
    }

    public Optional<Log> findByMessage(String message) {
        return em.createQuery("select l from Log l where l.message = :message", Log.class)
                .setParameter("message", message)
                .getResultList()
                .stream()
                .findFirst();
    }

    public void clear() {
        em.clear();
    }

}
