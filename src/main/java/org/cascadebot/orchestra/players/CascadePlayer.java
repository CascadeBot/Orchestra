package org.cascadebot.orchestra.players;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lavalink.client.player.event.IPlayerEventListener;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.cascadebot.orchestra.MusicHandler;
import org.cascadebot.orchestra.PlayerListener;
import org.cascadebot.orchestra.data.Equalizer;
import org.cascadebot.orchestra.data.Playlist;
import org.cascadebot.orchestra.data.TrackData;
import org.cascadebot.orchestra.data.enums.LoopMode;
import org.cascadebot.orchestra.data.enums.PlayerType;
import org.cascadebot.orchestra.utils.StringsUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CascadePlayer {

    private List<AudioTrack> queue = new ArrayList<>();
    private int queuePos = 0;

    private long guildId;

    private LoopMode loopMode = LoopMode.DISABLED;
    private boolean shuffle = false;

    private IPlayer player;
    private PlayerType type;

    private Equalizer equalizer = new Equalizer(this);

    public CascadePlayer(IPlayer player) {
        this.player = player;
        if (player instanceof LavaplayerPlayerWrapper) {
            type = PlayerType.LAVAPLAYER;
        } else if (player instanceof LavalinkPlayer) {
            type = PlayerType.LAVALINK;
        } else {
            type = PlayerType.UNKNOWN;
        }
        addListener(new PlayerListener(this)); // register player listener sp we can listen for track end events
    }

    /**
     * Get the current queue length. This includes everything, including "history".
     * It gets the duration of every track in the queue object and adds them all together.
     *
     * History refers to the songs that have already been played. In reality all songs are treated the same, but it's the best we have to refer to already played songs as.
     *
     * @return The queue length, not including "history".
     * @throws UnsupportedOperationException Throws this exception if we are currently looping
     */
    public double getQueueLength() throws UnsupportedOperationException {
        if (loopMode.equals(LoopMode.DISABLED)) {
            double queueLength = 0;
            for (AudioTrack track : queue) {
                queueLength += track.getDuration();
            }
            return queueLength;
        } else {
            throw new UnsupportedOperationException("Can only get queue length when not looping!");
        }
    }

    /**
     * Get the remaining queue length, meaning all the songs yet to be played.
     * Optionally also include what has yet to be played from the current song.
     *
     * @param includeCurrentSong include what was let to be played in the current song in the queue length
     * @return The queue length
     * @throws UnsupportedOperationException Throws this exception if we are currently looping
     */
    public double getRemainingQueueLength(boolean includeCurrentSong) throws UnsupportedOperationException {
        if (loopMode.equals(LoopMode.DISABLED)) {
            List<AudioTrack> remaining = queue.subList(queuePos + 1, queue.size() - 1);
            double queueLength = 0;
            for (AudioTrack track : remaining) {
                queueLength += track.getDuration();
            }
            if (includeCurrentSong) {
                queueLength += getTrackDuration() - getTrackPosition();
            }
            return queueLength;
        } else {
            throw new UnsupportedOperationException("Can only get queue length when not looping!");
        }
    }

    /**
     * Gets a progress bar for the currently playing song.
     * If embed is set to true it is formatted as '[▬▬▬▬▬▬](url)▬▬▬▬ 60%'. url currently links to https://github.com/CascadeBot, but can be changed, and is just there for discord url formatting.
     * If embed is set to false it is formatted as '══════⚪─── 60%'
     *
     * @param embed Weather or not this progress bar is going to be displayed in an embed
     * @return The formatted string of the progress bar
     */
    public String getTrackProgressBar(boolean embed) {
        float process = (100f / getPlayingTrack().getDuration() * getTrackPosition());
        if (embed) {
            return StringsUtil.getProgressBarEmbed(process);
        } else {
            return StringsUtil.getProgressBar(process);
        }
    }

    /**
     * Gets the url for track artwork for displaying in thumbnails, or elsewhere.
     *
     * @return The url pointing to the track artwork, or null if we cannot get the track artwork.
     */
    public String getArtwork() {
        if (getPlayingTrack().getSourceManager().getSourceName().equals("yt" /* CascadeBot.INS.getMusicHandler().getYoutubeSourceName() */)) {
            return "https://img.youtube.com/vi/" + getPlayingTrack().getIdentifier() + "/hqdefault.jpg";
        }
        if (getPlayingTrack().getSourceManager().getSourceName().equals("twitch" /* CascadeBot.INS.getMusicHandler().getTwitchSourceName() */)) {
            String[] split = getPlayingTrack().getInfo().identifier.split("/");
            return "https://static-cdn.jtvnw.net/previews-ttv/live_user_" + split[split.length - 1] + "-500x400.jpg";
        }
        return null;
    }

    /**
     * Plays a list of tracks.
     * If the player isn't playing a song this plays the first track, and add the rest to the queue.
     * If the player is playing a song this just adds to the queue.
     *
     * @param tracks The list of tracks to play
     */
    public void playTracks(Collection<AudioTrack> tracks) {
        tracks.forEach(this::playTrack);
    }

    /**
     * Sets the current loop mode of this player
     *
     * @param loopMode The new loop mode to set the player to.
     */
    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
    }

    /**
     * Toggles weather or not to shuffle when the end of queue has been reached and we loop back to the beginning.
     *
     * @return The new value for shuffleOnRepeat.
     */
    public boolean toggleShuffleOnRepeat() {
        boolean current = shuffle;
        shuffle = !current;
        return current;
    }

    /**
     * Shuffles the current queue
     */
    public void shuffle() {
        Collections.shuffle(queue);
    }

    /**
     * Gets weather or not shuffle is enabled
     *
     * @return Weather or not shuffle is enabled
     */
    public boolean isShuffleEnabled() {
        return shuffle;
    }

    /**
     * Gets the current loop mode
     *
     * @return The current loop mode
     * @see LoopMode
     */
    public LoopMode getLoopMode() {
        return loopMode;
    }

    /**
     * DO NOT USE, it is internally used to call #playTrack method that on player.
     * We override the playTrack method so we need this method for internal use.
     *
     * @param track The track to play.
     */
    protected void playTrackInternal(AudioTrack track) {
        player.playTrack(track);
    }

    /**
     * Gets the entire queue
     *
     * @return The entire queue, including history.
     */
    public List<AudioTrack> getQueue() {
        return queue;
    }

    /**
     * Skips the current track.
     */
    public void skip() {
        stopTrack();
    }

    /**
     * Stops the current track, and clears the queue so no mare tracks are played.
     */
    public void stop() {
        queue.clear();
        loopMode = LoopMode.DISABLED;
        stopTrack();
    }

    /**
     * Removes a track from the queue at the specified index.
     *
     * @param index The index of the track to remove
     * @throws UnsupportedOperationException Throws this exception is the track you are trying to remove is currently playing
     */
    public void removeTrack(int index) throws UnsupportedOperationException {
        if (index < queuePos) {
            queuePos--; // Move queue position down as the track was moved down in the list
        }
        if (index == queuePos) {
            throw new UnsupportedOperationException("Cannot remove currently playing track from queue!"); // This is essentially a skip and them remove, so throw an exception and have users skip if they want to remove
        }
        queue.remove(index);
    }

    /**
     * Moves the tracks around in the queue. This just swaps the tracks.
     *
     * @param track The position of the track to move.
     * @param pos The position to move the track to.
     * @throws UnsupportedOperationException Throws this exception if your trying to move a currently playing track. This behavior will change in the future.
     */
    public void moveTrack(int track, int pos) throws UnsupportedOperationException {
        if (track == queuePos || pos == queuePos) {
            throw new UnsupportedOperationException("Cannot move currently playing track in queue!"); // TODO account for this better, and ket users move currently playing track
        }
        List<AudioTrack> tracks = new ArrayList<>(queue);
        AudioTrack trackToMove = tracks.get(track);
        if (pos >= tracks.size()) {
            // Moved to end of array
            tracks.remove(track);
            tracks.add(trackToMove);
        }

        tracks.set(track, tracks.get(pos));
        tracks.set(pos, trackToMove);
        queue.clear();
        queue.addAll(tracks);
    }

    /**
     * Plays the next track in the queue. This is used internally, and probably shouldn't be called externally unless you have a good reason to.
     *
     * @param queueLoop Weather to loop back to the beginning of the queue or not if we reached the end
     * @return The audio track that was played
     */
    public AudioTrack playNextTrack(boolean queueLoop) {
        queuePos++; // Increment queue position
        if (queuePos >= queue.size()) { // handle end of queue and looping
            if (queueLoop) {
                queuePos = 0;
                if (shuffle) {
                    Collections.shuffle(queue);
                }
            } else {
              return null;  // return nothing as we hit end of queue
            }
        }
        // get next track and play it
        AudioTrack track = queue.get(queuePos);
        playTrackInternal(track);
        return track;
    }

    /**
     * Loads a link to an audio track.
     * This can be any link accepted by the source managers.
     *
     * @param input The link to the audio track to load.
     * @param trackData The track data to set on each track.
     * @param noMatchConsumer Consumer that runs when there is no match for this link. This could be because it's not supported by out source managers, or it's just an invalid link.
     * @param exceptionConsumer Consumer that runs when there was an error loading the audio track. See {@link FriendlyException} for more info on the error.
     * @param resultTracks If everything is successful this consumer is ran with a list of the loaded track(s).
     */
    public void loadLink(String input, TrackData trackData, Consumer<String> noMatchConsumer, Consumer<FriendlyException> exceptionConsumer, Consumer<List<AudioTrack>> resultTracks) {
        MusicHandler.getInstance().getPlayerManager().loadItem(input, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                audioTrack.setUserData(trackData);
                resultTracks.accept(Collections.singletonList(audioTrack));
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = new ArrayList<>();
                for (AudioTrack track : audioPlaylist.getTracks()) {
                    track.setUserData(track);
                    tracks.add(track);
                }
                resultTracks.accept(Collections.unmodifiableList(tracks));
            }

            @Override
            public void noMatches() {
                noMatchConsumer.accept(input);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                exceptionConsumer.accept(e);
            }
        });
    }

    /**
     * Loads all the tracks in a playlist.
     *
     * @param playlist The playlist to load tracks from.
     * @param trackData The track data to set an all the loaded tracks.
     * @param loadedConsumer If everything is successful this consumer is ran with a list of the loaded track(s).
     */
    public void loadLoadedPlaylist(Playlist playlist, TrackData trackData, Consumer<List<AudioTrack>> loadedConsumer) {
        List<AudioTrack> tracks = new ArrayList<>();
        for (String url : playlist.getTracks()) {
            loadLink(url, trackData, noMatch -> {
                playlist.removeTrack(url);
            }, exception -> {
                playlist.removeTrack(url);
            }, loadedTracks -> {
                tracks.addAll(loadedTracks);
                if (tracks.size() == playlist.getTracks().size()) {
                    loadedConsumer.accept(tracks);
                }
            });
        }
    }

    /**
     * Adds a track to the queue, and plays it it nothing is playing
     *
     * @param track The track to add.
     */
    public void addTrack(AudioTrack track) {
        queue.add(track);
        if (getPlayingTrack() == null) {
            playTrack(track);
        }
    }

    /**
     * Adds multiple track to the queue.
     * Plays the first track if there is nothing playing.
     *
     * @param tracks The tracks to add the the queue
     */
    public void addTracks(Collection<AudioTrack> tracks) {
        tracks.forEach(this::addTrack);
    }

    /**
     * Replaces the entire queue with a separate queue.
     *
     * @param queue The new queue.
     */
    public void setQueue(List<AudioTrack> queue) {
        this.queue = queue;
    }

    /**
     * Gets the type of player this is.
     * Basically indicates if this is using lavalink, or lavaplayer.
     *
     * @return
     */
    public PlayerType getType() {
        return type;
    }

    /**
     * Gets the {@link Equalizer} object that the player is currently using.
     *
     * @return The {@link Equalizer} objects the the players is using.
     * @throws UnsupportedOperationException This exception is thrown if lavaplayer is in use as we currently only support using equalizers with lavalink.
     */
    public Equalizer getEqualizer() {
        if (type == PlayerType.LAVALINK) {
            return equalizer;
        } else {
            throw new UnsupportedOperationException("Can only manipulate the equalizer when using lavalink");
        }
    }

    /**
     * Used internally when the equalizer has been changed in order to actually push those changes.
     */
    public void equzlizerChanged() { // TODO https://github.com/Frederikam/Lavalink-Client/pull/30
        if (type == PlayerType.LAVALINK) {
            LavalinkPlayer player = (LavalinkPlayer) this.player;
            LavalinkSocket node = player.getLink().getNode(false);
            if (node != null) {
                JSONObject json = new JSONObject();
                json.put("op", "equalizer");
                json.put("guildId", player.getLink().getGuildId());
                JSONArray jsonArray = new JSONArray();
                for (Map.Entry<Integer, Float> entry : equalizer.getBandsMap().entrySet()) {
                    if (entry.getKey() < 0 || entry.getKey() > com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT - 1) { // Make sure band is in range
                        throw new UnsupportedOperationException("Cannot set a band that doesn't exist");
                    }
                    JSONObject bandJson = new JSONObject();
                    bandJson.put("band", entry.getKey());
                    bandJson.put("gain", entry.getValue());
                    jsonArray.put(bandJson);
                }
                json.put("bands", jsonArray);
                node.send(json.toString());
            }
        } else {
            throw new UnsupportedOperationException("Can only manipulate the equalizer when using lavalink");
        }
    }

    /**
     * Gets the voice channel the bot is currently connected to, ot null if we're not connected to anything.
     *
     * @return The voice channel we are currently connected to.
     */
    public VoiceChannel getConnectedChannel() {
        if (player instanceof LavaplayerPlayerWrapper) {
            return MusicHandler.getInstance().getGuild(guildId).getAudioManager().getConnectedChannel();
        } else if (player instanceof LavalinkPlayer) {
            String channel = ((LavalinkPlayer) player).getLink().getChannel();
            if (channel != null) {
                return MusicHandler.getInstance().getGuild(guildId).getVoiceChannelById(channel);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Has the bot leave the current voice channel
     */
    public void leave() {
        if (player instanceof LavaplayerPlayerWrapper) {
            MusicHandler.getInstance().getGuild(guildId).getAudioManager().closeAudioConnection();
        } else if (player instanceof LavalinkPlayer) {
            ((LavalinkPlayer) player).getLink().destroy();
        }
    }

    /**
     * Joins the bot to a specified voice channel
     *
     * @param channel The voice channel to join the bot to
     */
    public void join(VoiceChannel channel) {
        if (player instanceof LavaplayerPlayerWrapper) {
            MusicHandler.getInstance().getGuild(guildId).getAudioManager().openAudioConnection(channel);
        } else if (player instanceof LavalinkPlayer) {
            MusicHandler.getInstance().getLavalink().getLink(String.valueOf(guildId)).connect(channel);
        }
    }

    /* region Player methods */
    public AudioTrack getPlayingTrack() {
        return player.getPlayingTrack();
    }

    public void playTrack(AudioTrack track) {
        if (getPlayingTrack() != null) {
            queue.add(track);
        } else {
            player.playTrack(track);
        }
    }

    public void stopTrack() {
        player.stopTrack();
    }

    public void setPaused(boolean b) {
        player.setPaused(b);
    }

    public boolean isPaused() {
        return player.isPaused();
    }

    public long getTrackPosition() {
        if (player instanceof LavalinkPlayer) {
            return player.getTrackPosition();
        } else if (player instanceof LavaplayerPlayerWrapper) {
            if (player.getPlayingTrack() == null) throw new IllegalStateException("Not playing anything!");
            return player.getPlayingTrack().getDuration();
        } else {
            throw new UnsupportedOperationException("This method is only supported when using the built-in players"); // TODO maybe allow other players if we wish to make further custom players
        }
    }

    public long getTrackDuration() {
        return getPlayingTrack().getDuration();
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public void setVolume(int volume) {
        player.setVolume(volume);
    }

    public int getVolume() {
        return player.getVolume();
    }

    public void addListener(IPlayerEventListener listener) {
        player.addListener(listener);
    }

    public void removeListener(IPlayerEventListener listener) {
        player.removeListener(listener);
    }
    /* endregion */

}
