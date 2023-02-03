package hello.springtx.propagation;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Log {

    @Id @GeneratedValue
    private Long id;

    private String message;

    public Log() {
    }

    public Log(String message) {
        this.message = message;
    }

}
