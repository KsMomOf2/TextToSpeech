package com.bishopireton.texttospeech;

/**
 * Created by kellyt on 2/15/2017.
 */

public class RssFeedModel {

    public String link;
    public String title;

    public RssFeedModel(String link) {
        this.link = link;
    }
    public RssFeedModel(String link, String title) {
        this.link = link;
        this.title = title;
    }
}