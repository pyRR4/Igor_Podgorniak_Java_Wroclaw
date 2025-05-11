package org.example.io;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class OutputWriter {

    public void printResultsToConsole(Map<String, BigDecimal> summary) {

        if (summary == null || summary.isEmpty()) {
            System.out.println("Brak danych do wyświetlenia podsumowania wydatków.");
        } else {
            summary.forEach((method, amount) ->
                    System.out.println(method + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString())
            );
        }
    }
}