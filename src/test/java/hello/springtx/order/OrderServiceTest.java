package hello.springtx.order;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @DisplayName("정상 결제")
    @Test
    void complete() throws NotEnoughMoneyException {
        Order order = new Order();
        order.setUsername("정상");

        orderService.order(order);

        Order foundOrder = orderRepository.findById(order.getId()).get();
        assertThat(foundOrder.getPayStatus()).isEqualTo("완료");
    }

    @DisplayName("시스템 예외 발생")
    @Test
    void runtimeException() {
        Order order = new Order();
        order.setUsername("예외");

        assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        // 저장하려고 했던 order 데이터가 롤백되어서 존재하지 않기 때문에 조회한 Optional 객체가 비어있게 됨
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }

    @DisplayName("잔고 부족 예외 발생")
    @Test
    void bizException() {
        Order order = new Order();
        order.setUsername("잔고부족");

        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }

        // 예외가 발생했지만 커밋을 하고, 결제 상태를 대기로 변경해서 저장
        Order foundOrder = orderRepository.findById(order.getId()).get();
        assertThat(foundOrder.getPayStatus()).isEqualTo("대기");
    }

}