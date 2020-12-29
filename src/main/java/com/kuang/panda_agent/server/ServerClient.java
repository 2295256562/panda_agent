package com.kuang.panda_agent.server;

import com.kuang.panda_agent.common.Response;
import com.kuang.panda_agent.core.Mobile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

public class ServerClient {

    @Autowired
    private RestTemplate restTemplate;


    @Value("${server}/mobile/save")
    private String mobileSaveUrl;

    public void saveMobile(Mobile mobile) {
        Response response = restTemplate.postForObject(mobileSaveUrl, mobile, Response.class);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getMsg());
        }
    }
}
