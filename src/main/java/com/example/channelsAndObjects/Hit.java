package com.example.channelsAndObjects;

public class Hit {
  private final Integer pageId;
  private final Integer sectionId;

  public Hit(Integer pageId, Integer sectionId){
    this.pageId = pageId;
    this.sectionId = sectionId;
  }

  public Integer getPageId() {
    return pageId;
  }

  public Integer getSectionId() {
    return sectionId;
  }

  @Override
  public String toString() {
    return "Hit{" +
      "pageId=" + pageId +
      ", sectionId=" + sectionId +
      '}';
  }
}
