package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@NoArgsConstructor
@Getter
@ToString
public class PaymentMethod {

    private String id;

    private BigDecimal discount;

    private BigDecimal limit;

    @JsonCreator
    public PaymentMethod(@JsonProperty("id") String id,
                         @JsonProperty("discount") BigDecimal discount,
                         @JsonProperty("limit") BigDecimal limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
