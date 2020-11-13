package org.cascadebot.orchestra.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.cascadebot.orchestra.data.enums.LoadPlaylistResult;
import org.cascadebot.orchestra.data.enums.PlaylistType;
import org.cascadebot.orchestra.data.TrackData;
import org.cascadebot.orchestra.data.enums.SavePlaylistResult;

import java.util.List;
import java.util.function.BiConsumer;

public interface PlaylistManager {

    void loadPlaylist(String name, TrackData trackData, BiConsumer<LoadPlaylistResult, List<AudioTrack>> consumer);

    void loadPlaylist(String name, TrackData trackData, PlaylistType scope, BiConsumer<LoadPlaylistResult, List<AudioTrack>> consumer);

    SavePlaylistResult saveCurrentPlaylist(long owner, PlaylistType scope, String name, boolean overwrite);


}
