/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.dto.capability;

import lombok.*;

import java.util.List;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PutTechCapabilityDTO {
    private String code;
    private String name;
    private String description;
    private String status;
    private String author;
    private String link;
    private String owner;
    private List<String> parents;
    private String targetSystemCode ;

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        PutTechCapabilityDTO that = (PutTechCapabilityDTO) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(status, that.status) &&
                Objects.equals(author, that.author) &&
                Objects.equals(link, that.link) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(parents, that.parents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, description, status, author, link, owner);
    }

    @Override
    public String toString() {
        return "PutTechCapabilityDTO{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", author='" + author + '\'' +
                ", link='" + link + '\'' +
                ", owner='" + owner + '\'' +
                ", parents=" + parents +
                '}';
    }
}