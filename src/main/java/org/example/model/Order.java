package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
@ToString
public class Order {
    private String orderId;

    private BigDecimal totalOrderValue;

    private List<String> promotions;

    @JsonCreator
    public Order(@JsonProperty("orderId") String orderId,
                 @JsonProperty("totalOrderValue") BigDecimal totalOrderValue,
                 @JsonProperty("promotions") List<String> promotions) {
        this.orderId = orderId;
        this.totalOrderValue = totalOrderValue;
        this.promotions = promotions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
