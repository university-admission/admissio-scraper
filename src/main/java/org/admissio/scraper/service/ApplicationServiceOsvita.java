package org.admissio.scraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.admissio.scraper.dto.url.UrlDto;
import org.admissio.scraper.utils.WebClientInsecure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ApplicationServiceOsvita {
    private final WebClient webClient;
    private ObjectMapper jacksonObjectMapper;
    private final String FULL_API_URL = "https://vstup.osvita.ua/api/";
    @Value("${scraper.osvita.api.token}")
    private String token;
    @Value("${scraper.osvita.api.cookie}")
    private String cookie;

    ApplicationServiceOsvita(ObjectMapper jacksonObjectMapper) {
        this.webClient = WebClientInsecure.createInsecureWebClient(FULL_API_URL);
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    public void printResponseUniversities(){
        String response = sendRequestForRawUniversity(16).block();
        System.out.println(response);
    }

    public void printResponseApplication(){
        String compressedResponse = sendApplicationsRequest();
        try {
            UrlDto url = jacksonObjectMapper.readValue(compressedResponse, UrlDto.class);
            System.out.println("Parsed URL: " + url.getUrl());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String sendApplicationsRequest() {
        String ckm = "88e65.703";
        String uid = "174";
        String sid = "1477258";
        String token = "0.VMfPy8V_hGkIC6HhT_5ktbvqXaq8LiSCJnMYAxe9vJ0vIz2f7dpcemg-nLMNc_etHteTs8qj4xBIHY9RROIOo8ZXfex5Zme_UgeyJU6HhQ2loRZW5H7Q681x6ZNDQme8lLYzys0ShmjIjBRifyt3KmbZVPI0apYXrnuBrUv5mOspcymsSE-1EA5IK0c8eMVPqx1yi3SKwyLgziE7BplcpbBGRsDVQkw8MqEFhlcCztWvesLbtfoivmGGSFon8Iyd9PVedzF1n-yY5WkrIWfT6hxyHT1DwWUKu74A6-1KNCrOVSGMKc8LJ3rcKSmKrlu6Q_riIJOhNWTr0uQ9Sqx44nT5S1OwGC-LajUHJYVeZGVkLw-Bhz0xMvQY2Rcfggwt5qEgAw-hXrM2gmANPoh9xIuHfGoN8tkDSHFRu5AZ1_4BCeCIuWDWHnZYIymz6bLKoTybcGS3jwcr887uVK44tJMO25cKLJ_6D0XthqBquLlvjsDVXiBFLigaMXweQN2kd4102w_TTxkFDccduvwIvTNtPI5PGiqD0K-rL_fiCIRWGLEgKlBQ3IXiy16Rwd7Qrure6_cu7HuseQGPjLcP01AXJVkNerwx8cxV4NkZhNQhDxAHRdF6pxSoykoq76OGATXlfwiWvf7vJq7aV_FEExN9oOYmBmmyct1bfYEnoc7bhqmWj16zqDTJhTCKUMvQF9ntTW8FAtAxGkXRk0ryLi8nZwbQMjpvPB_a0FbP2KEnj3Z9tDgm1g5_BJVvM8zBgqZd1o6m1Ya05a3suRaHNwgcPy3KJUcRanlA_DnJSXPW_8Ex9NuAKhJG_OrJz0SHewvsRqMuqmqZgTjPtnzqL7ESEMgsfwO4zfQUUfa0cXwWGlGduUQmLkLPz6aW5ig0WkQnMo485NvuNbWP0KoKmdIHEQ7iFneVUtIsYlKraX9_s9_B90EEdD8O1MTKgo6d.M8oo2RAMXCRetmujBqpBUA.8bf11030383184191dce410ac477eaa3b0fdcf662450e3e9f2bde3890459cd52";

        String body = "action=requests&ckm="+ckm+"&vca=cloudflare&y=2025&uid="+uid+"&sid="+sid+"&token="+token;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", "_ga=GA1.1.1546464469.1727005325; cf_clearance=XlCUCOTerxmWbd3fo.5hxmHkDifZzW3NjgbgGUnJQ.Q-1736707807-1.2.1.1-hS6EUh.hSV6s.TDAXOq8UiBQJQFgDKT.8IqMqse1smdXR3jh19h8y_PEpqvpFMckXuAUqFXkGmsgJPvOZjLyUjercK9ok8Upep_5FGHiFMTBYcCNrLFIwTc6rQ_IrkXJOxp0oyCAue0kTgn.gFVwq.mXVMPmKEvRn6EU614I9fgmS9ZYrocnMMh9qAf_sDw2oO7.lTX93AB_Nn8HtqQ3xOrhrxGIKnwor31KRlhI3CokeyDfOVISqgp3eIUtcQKEq8SkxeU9RKciuLl2pvPLkJYLoONyFxP11QyAiKOOkFk; PHPSESSID=ae5a5dba67ba75fea095c434817bef73; fvbr=Chrome; _ga_249WW3GG5N=GS2.1.s1754130362$o18$g1$t1754130371$j51$l0$h0; _ga_WM6Y0LDHD6=GS2.1.s1754130355$o16$g1$t1754131384$j58$l0$h0; _ga_F00FRGG1W3=GS2.1.s1754130355$o30$g1$t1754131384$j58$l0$h0; sdv=1754131391")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String sendRegionsRequest(){
        String uid = "79";
        String token = "0.VMfPy8V_hGkIC6HhT_5ktbvqXaq8LiSCJnMYAxe9vJ0vIz2f7dpcemg-nLMNc_etHteTs8qj4xBIHY9RROIOo8ZXfex5Zme_UgeyJU6HhQ2loRZW5H7Q681x6ZNDQme8lLYzys0ShmjIjBRifyt3KmbZVPI0apYXrnuBrUv5mOspcymsSE-1EA5IK0c8eMVPqx1yi3SKwyLgziE7BplcpbBGRsDVQkw8MqEFhlcCztWvesLbtfoivmGGSFon8Iyd9PVedzF1n-yY5WkrIWfT6hxyHT1DwWUKu74A6-1KNCrOVSGMKc8LJ3rcKSmKrlu6Q_riIJOhNWTr0uQ9Sqx44nT5S1OwGC-LajUHJYVeZGVkLw-Bhz0xMvQY2Rcfggwt5qEgAw-hXrM2gmANPoh9xIuHfGoN8tkDSHFRu5AZ1_4BCeCIuWDWHnZYIymz6bLKoTybcGS3jwcr887uVK44tJMO25cKLJ_6D0XthqBquLlvjsDVXiBFLigaMXweQN2kd4102w_TTxkFDccduvwIvTNtPI5PGiqD0K-rL_fiCIRWGLEgKlBQ3IXiy16Rwd7Qrure6_cu7HuseQGPjLcP01AXJVkNerwx8cxV4NkZhNQhDxAHRdF6pxSoykoq76OGATXlfwiWvf7vJq7aV_FEExN9oOYmBmmyct1bfYEnoc7bhqmWj16zqDTJhTCKUMvQF9ntTW8FAtAxGkXRk0ryLi8nZwbQMjpvPB_a0FbP2KEnj3Z9tDgm1g5_BJVvM8zBgqZd1o6m1Ya05a3suRaHNwgcPy3KJUcRanlA_DnJSXPW_8Ex9NuAKhJG_OrJz0SHewvsRqMuqmqZgTjPtnzqL7ESEMgsfwO4zfQUUfa0cXwWGlGduUQmLkLPz6aW5ig0WkQnMo485NvuNbWP0KoKmdIHEQ7iFneVUtIsYlKraX9_s9_B90EEdD8O1MTKgo6d.M8oo2RAMXCRetmujBqpBUA.8bf11030383184191dce410ac477eaa3b0fdcf662450e3e9f2bde3890459cd52";

        String body = "action=offers&vca=cloudflare&y=2025&uid="+uid+"&token="+token;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", "_ga=GA1.1.1546464469.1727005325; cf_clearance=XlCUCOTerxmWbd3fo.5hxmHkDifZzW3NjgbgGUnJQ.Q-1736707807-1.2.1.1-hS6EUh.hSV6s.TDAXOq8UiBQJQFgDKT.8IqMqse1smdXR3jh19h8y_PEpqvpFMckXuAUqFXkGmsgJPvOZjLyUjercK9ok8Upep_5FGHiFMTBYcCNrLFIwTc6rQ_IrkXJOxp0oyCAue0kTgn.gFVwq.mXVMPmKEvRn6EU614I9fgmS9ZYrocnMMh9qAf_sDw2oO7.lTX93AB_Nn8HtqQ3xOrhrxGIKnwor31KRlhI3CokeyDfOVISqgp3eIUtcQKEq8SkxeU9RKciuLl2pvPLkJYLoONyFxP11QyAiKOOkFk; PHPSESSID=ae5a5dba67ba75fea095c434817bef73; fvbr=Chrome; _ga_249WW3GG5N=GS2.1.s1754130362$o18$g1$t1754130371$j51$l0$h0; _ga_WM6Y0LDHD6=GS2.1.s1754130355$o16$g1$t1754131384$j58$l0$h0; _ga_F00FRGG1W3=GS2.1.s1754130355$o30$g1$t1754131384$j58$l0$h0; sdv=1754131391")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String sendUniversitiesByRegionRequest(int regionId){
        String token = "0.VMfPy8V_hGkIC6HhT_5ktbvqXaq8LiSCJnMYAxe9vJ0vIz2f7dpcemg-nLMNc_etHteTs8qj4xBIHY9RROIOo8ZXfex5Zme_UgeyJU6HhQ2loRZW5H7Q681x6ZNDQme8lLYzys0ShmjIjBRifyt3KmbZVPI0apYXrnuBrUv5mOspcymsSE-1EA5IK0c8eMVPqx1yi3SKwyLgziE7BplcpbBGRsDVQkw8MqEFhlcCztWvesLbtfoivmGGSFon8Iyd9PVedzF1n-yY5WkrIWfT6hxyHT1DwWUKu74A6-1KNCrOVSGMKc8LJ3rcKSmKrlu6Q_riIJOhNWTr0uQ9Sqx44nT5S1OwGC-LajUHJYVeZGVkLw-Bhz0xMvQY2Rcfggwt5qEgAw-hXrM2gmANPoh9xIuHfGoN8tkDSHFRu5AZ1_4BCeCIuWDWHnZYIymz6bLKoTybcGS3jwcr887uVK44tJMO25cKLJ_6D0XthqBquLlvjsDVXiBFLigaMXweQN2kd4102w_TTxkFDccduvwIvTNtPI5PGiqD0K-rL_fiCIRWGLEgKlBQ3IXiy16Rwd7Qrure6_cu7HuseQGPjLcP01AXJVkNerwx8cxV4NkZhNQhDxAHRdF6pxSoykoq76OGATXlfwiWvf7vJq7aV_FEExN9oOYmBmmyct1bfYEnoc7bhqmWj16zqDTJhTCKUMvQF9ntTW8FAtAxGkXRk0ryLi8nZwbQMjpvPB_a0FbP2KEnj3Z9tDgm1g5_BJVvM8zBgqZd1o6m1Ya05a3suRaHNwgcPy3KJUcRanlA_DnJSXPW_8Ex9NuAKhJG_OrJz0SHewvsRqMuqmqZgTjPtnzqL7ESEMgsfwO4zfQUUfa0cXwWGlGduUQmLkLPz6aW5ig0WkQnMo485NvuNbWP0KoKmdIHEQ7iFneVUtIsYlKraX9_s9_B90EEdD8O1MTKgo6d.M8oo2RAMXCRetmujBqpBUA.8bf11030383184191dce410ac477eaa3b0fdcf662450e3e9f2bde3890459cd52";

        String body = "action=universities&vca=cloudflare&y=2025&token="+token;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025/r"+regionId+"/174/1477258/")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", "_ga=GA1.1.1546464469.1727005325; cf_clearance=XlCUCOTerxmWbd3fo.5hxmHkDifZzW3NjgbgGUnJQ.Q-1736707807-1.2.1.1-hS6EUh.hSV6s.TDAXOq8UiBQJQFgDKT.8IqMqse1smdXR3jh19h8y_PEpqvpFMckXuAUqFXkGmsgJPvOZjLyUjercK9ok8Upep_5FGHiFMTBYcCNrLFIwTc6rQ_IrkXJOxp0oyCAue0kTgn.gFVwq.mXVMPmKEvRn6EU614I9fgmS9ZYrocnMMh9qAf_sDw2oO7.lTX93AB_Nn8HtqQ3xOrhrxGIKnwor31KRlhI3CokeyDfOVISqgp3eIUtcQKEq8SkxeU9RKciuLl2pvPLkJYLoONyFxP11QyAiKOOkFk; PHPSESSID=ae5a5dba67ba75fea095c434817bef73; fvbr=Chrome; _ga_249WW3GG5N=GS2.1.s1754130362$o18$g1$t1754130371$j51$l0$h0; _ga_WM6Y0LDHD6=GS2.1.s1754130355$o16$g1$t1754131384$j58$l0$h0; _ga_F00FRGG1W3=GS2.1.s1754130355$o30$g1$t1754131384$j58$l0$h0; sdv=1754131391")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String sendMajorsRequest(){
        String token = "0.VMfPy8V_hGkIC6HhT_5ktbvqXaq8LiSCJnMYAxe9vJ0vIz2f7dpcemg-nLMNc_etHteTs8qj4xBIHY9RROIOo8ZXfex5Zme_UgeyJU6HhQ2loRZW5H7Q681x6ZNDQme8lLYzys0ShmjIjBRifyt3KmbZVPI0apYXrnuBrUv5mOspcymsSE-1EA5IK0c8eMVPqx1yi3SKwyLgziE7BplcpbBGRsDVQkw8MqEFhlcCztWvesLbtfoivmGGSFon8Iyd9PVedzF1n-yY5WkrIWfT6hxyHT1DwWUKu74A6-1KNCrOVSGMKc8LJ3rcKSmKrlu6Q_riIJOhNWTr0uQ9Sqx44nT5S1OwGC-LajUHJYVeZGVkLw-Bhz0xMvQY2Rcfggwt5qEgAw-hXrM2gmANPoh9xIuHfGoN8tkDSHFRu5AZ1_4BCeCIuWDWHnZYIymz6bLKoTybcGS3jwcr887uVK44tJMO25cKLJ_6D0XthqBquLlvjsDVXiBFLigaMXweQN2kd4102w_TTxkFDccduvwIvTNtPI5PGiqD0K-rL_fiCIRWGLEgKlBQ3IXiy16Rwd7Qrure6_cu7HuseQGPjLcP01AXJVkNerwx8cxV4NkZhNQhDxAHRdF6pxSoykoq76OGATXlfwiWvf7vJq7aV_FEExN9oOYmBmmyct1bfYEnoc7bhqmWj16zqDTJhTCKUMvQF9ntTW8FAtAxGkXRk0ryLi8nZwbQMjpvPB_a0FbP2KEnj3Z9tDgm1g5_BJVvM8zBgqZd1o6m1Ya05a3suRaHNwgcPy3KJUcRanlA_DnJSXPW_8Ex9NuAKhJG_OrJz0SHewvsRqMuqmqZgTjPtnzqL7ESEMgsfwO4zfQUUfa0cXwWGlGduUQmLkLPz6aW5ig0WkQnMo485NvuNbWP0KoKmdIHEQ7iFneVUtIsYlKraX9_s9_B90EEdD8O1MTKgo6d.M8oo2RAMXCRetmujBqpBUA.8bf11030383184191dce410ac477eaa3b0fdcf662450e3e9f2bde3890459cd52";

        String body = "action=specialities&vca=cloudflare&y=2025&token="+token;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025/r27/79/1477258/")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", "_ga=GA1.1.1546464469.1727005325; cf_clearance=XlCUCOTerxmWbd3fo.5hxmHkDifZzW3NjgbgGUnJQ.Q-1736707807-1.2.1.1-hS6EUh.hSV6s.TDAXOq8UiBQJQFgDKT.8IqMqse1smdXR3jh19h8y_PEpqvpFMckXuAUqFXkGmsgJPvOZjLyUjercK9ok8Upep_5FGHiFMTBYcCNrLFIwTc6rQ_IrkXJOxp0oyCAue0kTgn.gFVwq.mXVMPmKEvRn6EU614I9fgmS9ZYrocnMMh9qAf_sDw2oO7.lTX93AB_Nn8HtqQ3xOrhrxGIKnwor31KRlhI3CokeyDfOVISqgp3eIUtcQKEq8SkxeU9RKciuLl2pvPLkJYLoONyFxP11QyAiKOOkFk; PHPSESSID=ae5a5dba67ba75fea095c434817bef73; fvbr=Chrome; _ga_249WW3GG5N=GS2.1.s1754130362$o18$g1$t1754130371$j51$l0$h0; _ga_WM6Y0LDHD6=GS2.1.s1754130355$o16$g1$t1754131384$j58$l0$h0; _ga_F00FRGG1W3=GS2.1.s1754130355$o30$g1$t1754131384$j58$l0$h0; sdv=1754131391")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Mono<String> sendRequestForRawUniversity(Integer regionCodeOsvita) {

        String body = "action=universities&vca=cloudflare&y=2025&token="+token;

        return webClient.post()
                .uri(FULL_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://vstup.osvita.ua/y2025/r"+regionCodeOsvita+"/174/1477258/")
                .header("Origin", "https://vstup.osvita.ua")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Priority", "u=1, i")
                .header("Host", "vstup.osvita.ua")
                .header("Accept-Language", "uk,en-US;q=0.9,en;q=0.8,ru;q=0.7")
                .header("Sec-Ch-Ua", "\"Opera\";v=\"120\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Cookie", cookie)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class);
    }

}
