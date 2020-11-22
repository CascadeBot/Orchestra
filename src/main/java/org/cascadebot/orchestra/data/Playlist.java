/*
 * Copyright (c) 2019 CascadeBot. All rights reserved.
 * Licensed under the MIT license.
 */

package org.cascadebot.orchestra.data;

import org.cascadebot.orchestra.data.enums.PlaylistType;

import java.util.List;

public class Playlist {

    private  Playlist() {

    }

    private String name;
    private long ownerId;
    private PlaylistType scope;

    private List<String> tracks;

    public Playlist(long ownerId, String name, PlaylistType scope, List<String> tracks) {
        this.ownerId = ownerId;
        this.name = name;
        this.scope = scope;
        this.tracks = tracks;
    }

    public void addTrack(String url) {
        tracks.add(url);
    }

    public void removeTrack(String url) {
        tracks.remove(url);
    }

    public List<String> getTracks() {
        return tracks;
    }

    public String getName() {
        return name;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public PlaylistType getScope() {
        return scope;
    }
}
