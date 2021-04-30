package com.kuang.panda_agent.server;

import com.kuang.panda_agent.App;
import com.kuang.panda_agent.common.Response;
import com.kuang.panda_agent.core.mobile.Mobile;
import com.kuang.panda_agent.model.UploadFile;
import com.kuang.panda_agent.utils.Terminal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ServerClient {

    private static ServerClient INSTANCE;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${address}/mobile/list")
    private String mobileListUrl;

    @Value("${address}/mobile/save")
    private String mobileSaveUrl;

    @Value("${address}/upload/file/{fileType}")
    private String uploadFileUrl;

    @Value("${address}/driver/downloadUrl")
    private String driverDownloadUrl;

    public static ServerClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = App.getBean(ServerClient.class);
        }
        return INSTANCE;
    }

    public Mobile getMobileById(String mobileId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", mobileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Response<List<Mobile>> response = restTemplate.exchange(mobileListUrl,
                HttpMethod.POST,
                new HttpEntity<>(params, headers),
                new ParameterizedTypeReference<Response<List<Mobile>>>() {
                }).getBody();

        if (response.isSuccess()) {
            return response.getData().stream().findFirst().orElse(null);
        } else {
            throw new RuntimeException(response.getMsg());
        }
    }

    /**
     * 保存设备
     * @param mobile 设备
     */
    public void saveMobile(Mobile mobile) {
        Response response = restTemplate.postForObject(mobileSaveUrl, mobile, Response.class);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMsg());
        }
    }

    public UploadFile uploadFile(File file, Integer fileType) {
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("file", new FileSystemResource(file));

        Response<UploadFile> response = restTemplate.exchange(uploadFileUrl,
                HttpMethod.POST,
                new HttpEntity<>(multiValueMap),
                new ParameterizedTypeReference<Response<UploadFile>>() {
                },
                fileType).getBody();

        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RuntimeException(response.getMsg());
        }
    }

    public String getChromedriverDownloadUrl(String mobileId) {
        Assert.hasText(mobileId, "mobileId不能为空");

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("deviceId", mobileId);
        params.add("type", 1); // chromedriver
        params.add("platform", Terminal.PLATFORM); // 1.windows 2.linux 3.macos

        Response<Map<String, String>> response = restTemplate.exchange(driverDownloadUrl,
                HttpMethod.POST,
                new HttpEntity<>(params),
                new ParameterizedTypeReference<Response<Map<String, String>>>() {
                }).getBody();

        if (response.isSuccess()) {
            return response.getData() != null ? response.getData().get("downloadUrl") : null;
        } else {
            throw new RuntimeException(response.getMsg());
        }
    }
}
