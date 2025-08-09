package org.admissio.scraper.utils;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;

public class WebClientInsecure {

    private static final int MAX_BUFFER_SIZE = 1024 * 1024 * 100; // 10MB

    public static WebClient createInsecureWebClient(String baseUrl) {
        try {
            // Config for SSL certificate
            reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
                    .secure(t -> {
                        try {
                            t.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
                        } catch (SSLException e) {
                            throw new RuntimeException("Error creating SSL context", e);
                        }
                    });

            // Config for bigger buffer
            ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
                    .build();

            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(exchangeStrategies)
                    .build();

        } catch (RuntimeException e) {
            System.err.println("Failed to create insecure WebClient: " + e.getMessage());
            throw e;
        }
    }

    public static WebClient createInsecureWebClientWithProxy(String baseUrl, String proxyHost, int proxyPort) {
        try {
            HttpClient httpClient = HttpClient.create()
                    .secure(t -> {
                        try {
                            t.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
                        } catch (SSLException e) {
                            throw new RuntimeException("Error creating SSL context", e);
                        }
                    })

                    .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP).host(proxyHost).port(proxyPort));

            ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
                    .build();

            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(exchangeStrategies)
                    .build();

        } catch (RuntimeException e) {
            System.err.println("Failed to create insecure WebClient with proxy: " + e.getMessage());
            throw e;
        }
    }

}