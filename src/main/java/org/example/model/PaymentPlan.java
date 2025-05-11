package org.example.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@AllArgsConstructor
@Getter
@ToString
public class PaymentPlan {
    private String orderId;

    private String paymentMethodId;

    private BigDecimal totalOrderValue;

    private BigDecimal pointsAmount;

    private BigDecimal cashAmount;

    private BigDecimal discount;

    private BigDecimal finalAmount;

    public boolean isFullyPaid() {
        return pointsAmount.add(cashAmount).compareTo(totalOrderValue) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentPlan that = (PaymentPlan) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(paymentMethodId, that.paymentMethodId) &&
                (totalOrderValue != null && that.totalOrderValue != null && totalOrderValue.compareTo(that.totalOrderValue) == 0) &&
                (pointsAmount != null && that.pointsAmount != null && pointsAmount.compareTo(that.pointsAmount) == 0) &&
                (cashAmount != null && that.cashAmount != null && cashAmount.compareTo(that.cashAmount) == 0) &&
                (discount != null && that.discount != null && discount.compareTo(that.discount) == 0) &&
                (finalAmount != null && that.finalAmount != null && finalAmount.compareTo(that.finalAmount) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, paymentMethodId, totalOrderValue, pointsAmount, cashAmount, discount, finalAmount);
    }
}
