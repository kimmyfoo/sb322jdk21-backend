package com.vercarus.sb322jdk21.backend.api.v1;

import com.vercarus.sb322jdk21.backend.integration.core.fiber.FiberHttpServlet;
import com.vercarus.sb322jdk21.backend.integration.v1.Sb322Jdk21GeneralRequest;
import com.vercarus.sb322jdk21.backend.integration.v1.Sb322Jdk21GeneralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RestController
@RequestMapping("/v1")
public class RestBaseController {
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/get"
    )
    public Sb322Jdk21GeneralResponse get(HttpServletRequest _request, HttpServletResponse _response, @RequestParam String key1, @RequestParam String key2) throws Exception {
        log.info("{}, {}",
                kv("key1", key1),
                kv("key2", key2));
        FiberHttpServlet.serve(_request, _response, this, "get",
                FiberHttpServlet.getParameterNames("key1", "key2"),
                FiberHttpServlet.getParameterArray(key1, key2),
                HttpEventHandler::requestInterceptor, HttpEventHandler::responseInterceptor, HttpEventHandler::errorInterceptor,
                true);
        return null;
    }

    public Sb322Jdk21GeneralResponse get(String key1, String key2) throws Exception {
//        Thread.sleep(5000);
        Sb322Jdk21GeneralResponse response = new Sb322Jdk21GeneralResponse();
        response.getResponse().put("key1", key1);
        response.getResponse().put("key2", key2);
        return response;
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/post"
    )
    public Sb322Jdk21GeneralResponse post(HttpServletRequest _request, HttpServletResponse _response, @RequestBody Sb322Jdk21GeneralRequest body) throws Exception {
        log.info("{}, {}",
                kv("key1", body.getKey1()),
                kv("key2", body.getKey2()));
        FiberHttpServlet.serve(_request, _response, this, "post",
                FiberHttpServlet.getParameterNames("body"),
                FiberHttpServlet.getParameterArray(body),
                HttpEventHandler::requestInterceptor, HttpEventHandler::responseInterceptor, HttpEventHandler::errorInterceptor,
                true);
        return null;
    }

    public Sb322Jdk21GeneralResponse post(Sb322Jdk21GeneralRequest body) throws Exception {
//        Thread.sleep(1000);
        Sb322Jdk21GeneralResponse response = new Sb322Jdk21GeneralResponse();
        response.getResponse().put("key1", body.getKey1());
        response.getResponse().put("key2", body.getKey2());
        return response;
    }
}
