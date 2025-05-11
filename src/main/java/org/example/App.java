package org.example;

import org.example.io.JsonDataReader;
import org.example.io.OutputWriter;
import org.example.service.PaymentOptimizer;
import org.example.model.Order;
import org.example.model.PaymentMethod;
import org.example.model.PaymentPlan;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Błąd: Należy podać dwie ścieżki do plików jako argumenty.");
            System.err.println("Użycie: java -jar <nazwa_pliku_jar> <ścieżka_do_orders.json> <ścieżka_do_paymentmethods.json>");
            System.exit(1);
        }

        String ordersFilePath = args[0];
        String paymentMethodsFilePath = args[1];

        JsonDataReader dataReader = new JsonDataReader();
        OutputWriter outputWriter = new OutputWriter();

        try {
            System.out.println("Reading payment methods from: " + paymentMethodsFilePath);
            List<PaymentMethod> allPaymentMethods = dataReader.readPaymentMethods(paymentMethodsFilePath);
            System.out.println("Successfully read " + allPaymentMethods.size() + " payment methods.");

            System.out.println("\nReading orders from: " + ordersFilePath);
            List<Order> ordersToProcess = dataReader.readOrders(ordersFilePath);
            System.out.println("Successfully read " + ordersToProcess.size() + " orders.");


            PaymentOptimizer optimizer = new PaymentOptimizer(allPaymentMethods);

            System.out.println("\nStarting payment optimization...");
            List<PaymentPlan> chosenPlans = optimizer.optimizePayments(ordersToProcess);
            System.out.println("Payment optimization completed.");

            Map<String, BigDecimal> spendingSummary = optimizer.calculateSpendingSummary(chosenPlans);

            outputWriter.printResultsToConsole(spendingSummary);

            System.out.println("\nProcess finished successfully.");

        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}