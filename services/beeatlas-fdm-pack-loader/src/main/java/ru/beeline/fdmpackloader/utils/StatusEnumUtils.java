/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.utils;


import org.springframework.stereotype.Component;
import ru.beeline.fdmpackloader.domain.StatusEnum;
import ru.beeline.fdmpackloader.repository.StatusRepository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatusEnumUtils {
    private static final Map<String, Integer> statusMap = new HashMap<>();

    private final StatusRepository statusRepository;

    public StatusEnumUtils(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @PostConstruct
    public void init() {
        List<StatusEnum> statuses = statusRepository.findAll();
        for (StatusEnum status : statuses) {
            statusMap.put(status.getStatus(), status.getId());
        }
    }

//    public static String getStatusById(int id) {
//        for (Map.Entry<String, Integer> entry : statusMap.entrySet()) {
//            if (entry.getValue().equals(id)) {
//                return entry.getKey();
//            }
//        }
//        throw new IllegalArgumentException("Invalid status: " + id);
//    }

    public static int getIdByStatus(String status) {
        return statusMap.get(status);
    }
}