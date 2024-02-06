package crpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {

    private final int requestLimit;
    private final int timeUnit;
    private int countRequest = 0;
    private long lastUpdateTimeMillis = System.currentTimeMillis();
    private final Lock lock = new ReentrantLock();

    public CrptApi(int requestLimit, int timeUnit) {
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
    }

    public void createDocument(String uri, CrptDocument document, String signature) {
        lock.lock();
        try {
            updateRequestCountAndTime();

            if (countRequest < requestLimit) {
                makeApiRequest(uri, document, signature);
            } {
                System.out.println("Limit create document");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Thread interrupted while creating document: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error making request: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void updateRequestCountAndTime() {
        long nowTimeMillis = System.currentTimeMillis();
        if (nowTimeMillis - lastUpdateTimeMillis >= timeUnit) {
            countRequest = 0;
            lastUpdateTimeMillis = nowTimeMillis;
        }
    }

    private <T> void makeApiRequest(String uri, T document, String signature) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        RequestBody body = new RequestBody(document, signature);
        String bodyJson = objectMapper.writeValueAsString(body);

        HttpRequest request = buildPostRequest(uri, bodyJson);

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildPostRequest(String uri, String bodyJson) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CrptDocument {
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
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Description {
            private String participantInn;
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
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
    }

    @Getter
    @Setter
    @AllArgsConstructor
    static class RequestBody {
        private final Object document;
        private final String signature;
    }
}
