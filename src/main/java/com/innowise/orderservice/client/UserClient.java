package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/api/users/email/{email}")
    UserInfoResponse getByEmail(@PathVariable("email") String email);
}
