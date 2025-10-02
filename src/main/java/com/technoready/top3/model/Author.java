package com.technoready.top3.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Author {
    private String authorId;
    private String name;
    private String affiliation;
    private Integer citedBy;
    private List<String> interests;

    public String getAuthorId() { return authorId; }
    public String getName() { return name; }
    public String getAffiliation() { return affiliation; }
    public Integer getCitedBy() { return citedBy; }
    public List<String> getInterests() { return interests; }

    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setName(String name) { this.name = name; }
    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }
    public void setCitedBy(Integer citedBy) { this.citedBy = citedBy; }
    public void setInterests(List<String> interests) { this.interests = interests; }
}
