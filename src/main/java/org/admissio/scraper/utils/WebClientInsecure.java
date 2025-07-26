package org.admissio.scraper.utils;

import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import javax.net.ssl.SSLException;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class WebClientInsecure {

    public static WebClient createInsecureWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> {
            try {
                sslContextSpec.sslContext(SslContextBuilder.forClient()
                        .trustManager(new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                            public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }).build());
            } catch (SSLException e) {
                throw new RuntimeException("SSL context build failed", e);
            }
        });

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
