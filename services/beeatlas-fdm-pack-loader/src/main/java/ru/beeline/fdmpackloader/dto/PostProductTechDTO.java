/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PostProductTechDTO {

    @JsonProperty("cmdb_code")
    private String cmdbCode;

    @JsonProperty("proj_lang")
    private String projLang;
}
