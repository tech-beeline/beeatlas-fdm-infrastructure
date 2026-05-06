/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.dto.capability;

import lombok.*;

import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PutBusinessCapabilityDTO {

    private String code;
    private String name;
    private String description;
    private String status;
    private String author;
    private String link;
    private String owner;
    private boolean isDomain;
    private String parent;

    public boolean getIsDomain() {
        return isDomain;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        PutBusinessCapabilityDTO that = (PutBusinessCapabilityDTO) o;
        return isDomain == that.isDomain &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(status, that.status) &&
                Objects.equals(author, that.author) &&
                Objects.equals(link, that.link) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, description, status, author, link, owner, isDomain, parent);
    }
    @Override
    public String toString() {
        return "PutBusinessCapabilityDTO{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", author='" + author + '\'' +
                ", link='" + link + '\'' +
                ", owner='" + owner + '\'' +
                ", isDomain=" + isDomain +
                ", parent='" + parent + '\'' +
                '}';
    }
}
