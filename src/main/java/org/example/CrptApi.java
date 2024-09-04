package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ObjectMapper mapper;
    private final OkHttpClient okHttpClient;
    private Date date;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        okHttpClient = new OkHttpClient();
        mapper = new ObjectMapper();
    }

    public void createDocument(Document document) {

        synchronized (this) {
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - date.getTime();
                if (timePassed >= timeUnit.toMillis(1)) {
                    requestCount.set(0);
                    date = new Date(currentTime);
                }
                while (requestCount.get() >= requestLimit) {
                    try {
                        wait(timeUnit.toMillis(1) - timePassed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentTime = System.currentTimeMillis();
                    timePassed = currentTime - date.getTime();

                    if (timePassed >= timeUnit.toMillis(1)) {
                        requestCount.set(0);
                        date = new Date(currentTime);
                    }
                }
        }

        try {
            String json = mapper.writeValueAsString(document);
            RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .post(body)
                    .build();

            Response response = okHttpClient.newCall(request).execute();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestCount.incrementAndGet();
    }

    @AllArgsConstructor
    @Setter
    @Getter
    public static class Description {
        private String participantInn;
    }

    @AllArgsConstructor
    @Setter
    @Getter
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }


    @AllArgsConstructor
    @Setter
    @Getter
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private ArrayList<Product> products;
        private String reg_date;
        private String reg_number;
    }


    public static void main(String[] args) {
        ArrayList<Product> products = new ArrayList<>();

        Product product = new Product(
                "657r4uchui9",
                "2020-01-23",
                "745649",
                "543",
                "541",
                "5432",
                "49893",
                "2039",
                "03994"
        );

        products.add(product);
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 3);
        Description description = new Description("1234567890");

        Document document = new Document(
                description,
                "1",
                "some status",
                "LP_INTRODUCE_GOODS" + 109,
                true,
                "234",
                "6543",
                "097",
                "2020-01-23",
                "some type",
                products,
                "2020-01-23",
                "657595739"
        );

        crptApi.createDocument(document);
    }
}