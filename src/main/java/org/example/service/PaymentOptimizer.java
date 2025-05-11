package org.example.service;

import lombok.Getter;
import org.example.model.Order;
import org.example.model.PaymentMethod;
import org.example.model.PaymentPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentOptimizer {

    private static final String POINTS_METHOD_ID = "PUNKTY";
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaymentCalculator paymentCalculator;
    private final List<PaymentMethod> allPaymentMethods;

    public PaymentOptimizer(List<PaymentMethod> allPaymentMethodsDefinition) {
        this.allPaymentMethods = List.copyOf(new ArrayList<>(allPaymentMethodsDefinition));
        this.paymentCalculator = new PaymentCalculator();
    }

    private PaymentMethod findPaymentMethodById(String id) {
        return allPaymentMethods.stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst()
                .orElse(null);
    }

    //helper classes to assess promotions and point payments
    @Getter
    static class PotentialCardPromotion {
        Order order;
        PaymentMethod card;
        BigDecimal absoluteDiscountValue;
        BigDecimal costAfterDiscount;

        PotentialCardPromotion(Order order, PaymentMethod card, BigDecimal absoluteDiscountValue, BigDecimal costAfterDiscount) {
            this.order = order;
            this.card = card;
            this.absoluteDiscountValue = absoluteDiscountValue;
            this.costAfterDiscount = costAfterDiscount;
        }
    }

    @Getter
    static class PotentialPointsPayment {
        Order order;
        BigDecimal pointsCost;
        BigDecimal discountValue;

        PotentialPointsPayment(Order order, BigDecimal pointsCost, BigDecimal discountValue) {
            this.order = order;
            this.pointsCost = pointsCost;
            this.discountValue = discountValue;
        }
    }


    public List<PaymentPlan> optimizePayments(List<Order> orders) {
        List<PaymentPlan> chosenPlans = new ArrayList<>();
        Map<String, BigDecimal> currentLimits = new HashMap<>();

        for (PaymentMethod method : allPaymentMethods) {
            currentLimits.put(method.getId(), method.getLimit().setScale(SCALE, ROUNDING_MODE));
        }

        Set<String> paidOrderIds = new HashSet<>();

        //Find best card promotion
        List<PotentialCardPromotion> cardPromotions = new ArrayList<>();
        for (Order order : orders) {
            if (order.getPromotions() != null) {
                for (String promoInfo : order.getPromotions()) {
                    PaymentMethod actualCard = findPaymentMethodById(promoInfo);
                    if (actualCard != null && !POINTS_METHOD_ID.equals(actualCard.getId())) {
                        BigDecimal discountPercent = actualCard.getDiscount();
                        BigDecimal discount = order.getTotalOrderValue().multiply(discountPercent)
                                .divide(ONE_HUNDRED, SCALE, ROUNDING_MODE);
                        BigDecimal cost = order.getTotalOrderValue().subtract(discount);
                        cardPromotions.add(new PotentialCardPromotion(order, actualCard, discount, cost));
                    }
                }
            }
        }
        cardPromotions.sort(Comparator.comparing(PotentialCardPromotion::getAbsoluteDiscountValue, Comparator.reverseOrder())
                .thenComparing(p -> p.order.getTotalOrderValue())); // Prefer higher discount, then smaller orders

        for (PotentialCardPromotion promo : cardPromotions) {
            if (paidOrderIds.contains(promo.order.getOrderId())) continue;

            if (currentLimits.getOrDefault(promo.card.getId(), BigDecimal.ZERO).compareTo(promo.costAfterDiscount) >= 0) {
                PaymentPlan plan = new PaymentPlan(promo.order.getOrderId(), promo.card.getId(),
                        promo.order.getTotalOrderValue(), BigDecimal.ZERO, promo.costAfterDiscount,
                        promo.absoluteDiscountValue, promo.costAfterDiscount);
                chosenPlans.add(plan);
                paidOrderIds.add(promo.order.getOrderId());
                currentLimits.compute(promo.card.getId(), (id, limit) -> limit.subtract(promo.costAfterDiscount));
            }
        }

        //Find best full points payment
        PaymentMethod pointsMethodDef = findPaymentMethodById(POINTS_METHOD_ID);
        if (pointsMethodDef != null) {
            List<PotentialPointsPayment> pointsPayments = new ArrayList<>();
            for (Order order : orders) {
                if (paidOrderIds.contains(order.getOrderId())) continue;

                BigDecimal discountPercent = pointsMethodDef.getDiscount();
                BigDecimal discount = order.getTotalOrderValue().multiply(discountPercent)
                        .divide(ONE_HUNDRED, SCALE, ROUNDING_MODE);
                BigDecimal cost = order.getTotalOrderValue().subtract(discount);
                pointsPayments.add(new PotentialPointsPayment(order, cost, discount));
            }
            // Prioritize: smaller orders first to clear them with points, or higher discount % (already fixed by pointsMethodDef)
            pointsPayments.sort(Comparator.comparing((PotentialPointsPayment p) -> p.order.getTotalOrderValue())
                    .thenComparing(PotentialPointsPayment::getDiscountValue, Comparator.reverseOrder()));


            for (PotentialPointsPayment pp : pointsPayments) {
                if (paidOrderIds.contains(pp.order.getOrderId())) continue;

                if (currentLimits.getOrDefault(POINTS_METHOD_ID, BigDecimal.ZERO).compareTo(pp.pointsCost) >= 0) {
                    PaymentPlan plan = new PaymentPlan(pp.order.getOrderId(), POINTS_METHOD_ID,
                            pp.order.getTotalOrderValue(), pp.pointsCost, BigDecimal.ZERO,
                            pp.discountValue, pp.pointsCost);
                    chosenPlans.add(plan);
                    paidOrderIds.add(pp.order.getOrderId());
                    currentLimits.compute(POINTS_METHOD_ID, (id, limit) -> limit.subtract(pp.pointsCost));
                }
            }
        }

        //process remaining orders
        List<Order> remainingOrders = orders.stream()
                .filter(o -> !paidOrderIds.contains(o.getOrderId()))
                .sorted(Comparator.comparing(Order::getTotalOrderValue, Comparator.reverseOrder())) // Process larger remaining orders first
                .toList();

        for (Order order : remainingOrders) {
            if (paidOrderIds.contains(order.getOrderId())) continue;

            List<PaymentPlan> possiblePlans = paymentCalculator.generatePossiblePlans(order, allPaymentMethods, currentLimits);

            if (possiblePlans.isEmpty()) {
                System.err.println("Warning: Could not find any payment plan for order: " + order.getOrderId() + " with current limits.");
                continue;
            }

            Optional<PaymentPlan> bestPlanOpt = possiblePlans.stream()
                    .filter(Objects::nonNull)
                    .min(Comparator.comparing(PaymentPlan::getDiscount, Comparator.reverseOrder())
                            .thenComparing(PaymentPlan::getPointsAmount, Comparator.reverseOrder())
                            .thenComparing(PaymentPlan::getFinalAmount));

            if (bestPlanOpt.isPresent()) {
                PaymentPlan bestPlan = bestPlanOpt.get();
                chosenPlans.add(bestPlan);
                paidOrderIds.add(order.getOrderId());

                // Update limits
                if (bestPlan.getPointsAmount().compareTo(BigDecimal.ZERO) > 0) {
                    currentLimits.compute(POINTS_METHOD_ID, (id, limit) -> Optional.ofNullable(limit).orElse(BigDecimal.ZERO).subtract(bestPlan.getPointsAmount()));
                }
                if (bestPlan.getCashAmount().compareTo(BigDecimal.ZERO) > 0 && !POINTS_METHOD_ID.equals(bestPlan.getPaymentMethodId())) {
                    currentLimits.compute(bestPlan.getPaymentMethodId(), (id, limit) -> Optional.ofNullable(limit).orElse(BigDecimal.ZERO).subtract(bestPlan.getCashAmount()));
                }
            } else {
                System.err.println("Warning: No viable plan selected for order: " + order.getOrderId() + " from generated plans.");
            }
        }

        if (paidOrderIds.size() != orders.size()) {
            System.err.println("Warning: Not all orders were processed. Unpaid order IDs: " +
                    orders.stream().map(Order::getOrderId).filter(id -> !paidOrderIds.contains(id)).collect(Collectors.joining(", ")));
        }

        return chosenPlans;
    }

    public Map<String, BigDecimal> calculateSpendingSummary(List<PaymentPlan> chosenPlans) {
        Map<String, BigDecimal> spendingSummary = new HashMap<>();
        for (PaymentPlan plan : chosenPlans) {
            if (plan.getPointsAmount() != null && plan.getPointsAmount().compareTo(BigDecimal.ZERO) > 0) {
                spendingSummary.merge(POINTS_METHOD_ID, plan.getPointsAmount(), BigDecimal::add);
            }
            if (plan.getCashAmount() != null && plan.getCashAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (!POINTS_METHOD_ID.equals(plan.getPaymentMethodId()) ||
                        (POINTS_METHOD_ID.equals(plan.getPaymentMethodId()) && plan.getCashAmount().compareTo(BigDecimal.ZERO) > 0)) {
                    spendingSummary.merge(plan.getPaymentMethodId(), plan.getCashAmount(), BigDecimal::add);
                }
            }
        }
        return spendingSummary;
    }
}
