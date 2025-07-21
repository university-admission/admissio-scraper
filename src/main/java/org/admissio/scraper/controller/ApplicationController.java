package org.admissio.scraper.controller;

import org.admissio.scraper.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;


    // Example of a request from frontend:
//    fetch('http://localhost:8081/api/proxy-application?id=1340074&last=0', {
//        method: 'POST'
//    })
//            .then(res => res.text())
//            .then(console.log);
    @PostMapping("/api/proxy-application")
    public String getApplicationProxy(
            @RequestParam("id") String id,
            @RequestParam("last") int last) {

        return applicationService.postApplicationRequest(id, last);
    }

}
