package org.example.service;

import org.example.model.Order;
import org.example.model.PaymentMethod;
import org.example.model.PaymentPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PaymentCalculator {

    private static final String POINTS_METHOD_ID = "PUNKTY";
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TEN_PERCENT = BigDecimal.valueOf(0.1);

    private PaymentMethod findPaymentMethodById(String id, List<PaymentMethod> methods) {
        return methods.stream()
                .filter(pm -> pm.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<PaymentPlan> generatePossiblePlans(
            Order order,
            List<PaymentMethod> allPaymentMethods,
            Map<String, BigDecimal> currentLimits
    ) {
        List<PaymentPlan> possiblePlans = new ArrayList<>();

        PaymentMethod pointsMethod = findPaymentMethodById(POINTS_METHOD_ID, allPaymentMethods);
        BigDecimal availablePoints = currentLimits.getOrDefault(POINTS_METHOD_ID, BigDecimal.ZERO);

        //FULL PAYMENT WITH CARD
        if(order.getPromotions() != null) {
            for (String promotedPaymentMethod : order.getPromotions()) {
                PaymentMethod actualPaymentMethod = findPaymentMethodById(promotedPaymentMethod, allPaymentMethods);
                if(actualPaymentMethod != null && !actualPaymentMethod.getId().equals(POINTS_METHOD_ID)) {
                    BigDecimal cardDiscountPercent = actualPaymentMethod.getDiscount();
                    BigDecimal cardDiscountValue = order.getTotalOrderValue()
                            .multiply(cardDiscountPercent)
                            .divide(ONE_HUNDRED, SCALE, ROUNDING_MODE);
                    BigDecimal cashAmountNeeded = order.getTotalOrderValue().subtract(cardDiscountValue);

                    if (currentLimits.getOrDefault(actualPaymentMethod.getId(), BigDecimal.ZERO)
                            .compareTo(cashAmountNeeded) >= 0) {
                        possiblePlans.add(new PaymentPlan(
                                order.getOrderId(),
                                actualPaymentMethod.getId(),
                                order.getTotalOrderValue(),
                                BigDecimal.ZERO,
                                cashAmountNeeded,
                                cardDiscountValue,
                                cashAmountNeeded
                        ));
                    }
                }

            }
        }

        //FULL PAYMENT WITH POINTS
        if(pointsMethod != null) {
            BigDecimal pointsDiscountPercent = pointsMethod.getDiscount();
            BigDecimal pointsDiscountValue = order.getTotalOrderValue()
                    .multiply(pointsDiscountPercent)
                    .divide(ONE_HUNDRED, SCALE, ROUNDING_MODE);
            BigDecimal pointsAmountNeeded = order.getTotalOrderValue().subtract(pointsDiscountValue);

            if(availablePoints.compareTo(pointsDiscountValue) >= 0) {
                possiblePlans.add(new PaymentPlan(
                        order.getOrderId(),
                        POINTS_METHOD_ID,
                        order.getTotalOrderValue(),
                        pointsAmountNeeded,
                        BigDecimal.ZERO,
                        pointsDiscountValue,
                        pointsAmountNeeded
                ));
            }
        }

        //PARTIAL PAYMENT WITH POINTS
        BigDecimal tenPercentForOrder = order.getTotalOrderValue()
                .multiply(TEN_PERCENT)
                .setScale(SCALE, ROUNDING_MODE);

        if(availablePoints.compareTo(tenPercentForOrder) >= 0) {
            BigDecimal amountDueAfterDiscount = order.getTotalOrderValue().subtract(tenPercentForOrder);

            BigDecimal pointsToCommit = availablePoints.min(amountDueAfterDiscount);
            if(pointsToCommit.compareTo(tenPercentForOrder) < 0) {
                pointsToCommit = tenPercentForOrder;
            }
            pointsToCommit = pointsToCommit.min(availablePoints);
            pointsToCommit = pointsToCommit.min(amountDueAfterDiscount.max(tenPercentForOrder));

            if(pointsToCommit.compareTo(BigDecimal.ZERO) > 0 && availablePoints.compareTo(pointsToCommit) >= 0) {
                BigDecimal cashAmountNeeded = amountDueAfterDiscount.subtract(pointsToCommit);

                if(cashAmountNeeded.compareTo(BigDecimal.ZERO) < 0)
                    cashAmountNeeded = BigDecimal.ZERO;

                if(pointsToCommit.compareTo(BigDecimal.ZERO) < 0)
                    pointsToCommit = BigDecimal.ZERO;

                if(cashAmountNeeded.compareTo(BigDecimal.ZERO) == 0) {
                    if(pointsToCommit.compareTo(tenPercentForOrder) >= 0 && availablePoints.compareTo(pointsToCommit) >= 0) {
                        possiblePlans.add(new PaymentPlan(
                                order.getOrderId(),
                                POINTS_METHOD_ID,
                                order.getTotalOrderValue(),
                                pointsToCommit,
                                BigDecimal.ZERO,
                                tenPercentForOrder,
                                pointsToCommit
                        ));
                    }
                }
            }
        }

        //NO DISCOUNT
        for(PaymentMethod method : allPaymentMethods) {
            if(!POINTS_METHOD_ID.equals(method.getId())) {
                boolean coveredByFullCard = possiblePlans.stream().anyMatch(p ->
                        p.getPaymentMethodId().equals(method.getId()) &&
                        p.getDiscount().compareTo(BigDecimal.ZERO) > 0 &&
                        p.getPointsAmount().compareTo(BigDecimal.ZERO) == 0);

                if(!coveredByFullCard) {
                    if(currentLimits.getOrDefault(method.getId(), BigDecimal.ZERO)
                            .compareTo(order.getTotalOrderValue()) >= 0) {
                        possiblePlans.add(new PaymentPlan(
                                order.getOrderId(),
                                method.getId(),
                                order.getTotalOrderValue(),
                                BigDecimal.ZERO,
                                order.getTotalOrderValue(),
                                BigDecimal.ZERO,
                                order.getTotalOrderValue()
                        ));
                    }
                }
            }
        }

        //POINTS WITHOUT 10% DISCOUNT (extra points left)
        if(availablePoints.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxPointsForCase = tenPercentForOrder.subtract(BigDecimal.valueOf(0.01)).max(BigDecimal.ZERO);
            BigDecimal pointsToAttempt = availablePoints.min(maxPointsForCase);
            pointsToAttempt = pointsToAttempt.min(order.getTotalOrderValue());

            if(pointsToAttempt.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cashAmountNeeded = order.getTotalOrderValue().subtract(pointsToAttempt);
                if(cashAmountNeeded.compareTo(BigDecimal.ZERO) < 0)
                    cashAmountNeeded = BigDecimal.ZERO;

                if(cashAmountNeeded.compareTo(BigDecimal.ZERO) == 0) {
                    possiblePlans.add(new PaymentPlan(
                            order.getOrderId(),
                            POINTS_METHOD_ID,
                            order.getTotalOrderValue(),
                            pointsToAttempt,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            pointsToAttempt
                    ));
                } else {
                    for (PaymentMethod method : allPaymentMethods) {
                        if(!POINTS_METHOD_ID.equals(method.getId())) {
                            if(currentLimits.getOrDefault(method.getId(), BigDecimal.ZERO)
                                    .compareTo(cashAmountNeeded) >= 0) {
                                possiblePlans.add(new PaymentPlan(
                                        order.getOrderId(),
                                        method.getId(),
                                        order.getTotalOrderValue(),
                                        pointsToAttempt,
                                        cashAmountNeeded,
                                        BigDecimal.ZERO,
                                        pointsToAttempt.add(cashAmountNeeded)
                                ));
                            }
                        }
                    }
                }
            }
        }

        return possiblePlans.stream()
                .filter(Objects::nonNull)
                .filter(PaymentPlan::isFullyPaid)
                .distinct()
                .toList();
    }
}
