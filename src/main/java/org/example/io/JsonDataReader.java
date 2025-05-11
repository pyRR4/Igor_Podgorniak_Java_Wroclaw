package org.example.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Order;
import org.example.model.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonDataReader {

    private final ObjectMapper objectMapper;

    public JsonDataReader() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Order> readOrders(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Orders file not found: " + filePath);
        }
        return objectMapper.readValue(file, new TypeReference<List<Order>>() {});
    }

    public List<PaymentMethod> readPaymentMethods(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Payment methods file not found: " + filePath);
        }
        return objectMapper.readValue(file, new TypeReference<List<PaymentMethod>>() {});
    }
}