package org.cascadebot.orchestra.players;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lavalink.client.player.event.IPlayerEventListener;
import org.cascadebot.orchestra.MusicHandler;
import org.cascadebot.orchestra.data.Playlist;
import org.cascadebot.orchestra.data.enums.LoopMode;
import org.cascadebot.orchestra.data.TrackData;
import org.cascadebot.orchestra.utils.StringsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public class CascadePlayer {

    private Queue<AudioTrack> queue = new LinkedList<>();

    private long guildId;

    private LoopMode loopMode = LoopMode.DISABLED;
    private boolean shuffle = false;

    private IPlayer player;

    public CascadePlayer(IPlayer player) {
        this.player = player;
    }

    public double getQueueLength(boolean includeCurrentSong) {
        double queueLength = 0;
        if (includeCurrentSong) {
            queueLength = getTrackDuration() - getTrackPosition();
        }
        for (AudioTrack track : queue) {
            queueLength += track.getDuration();
        }
        return queueLength;
    }

    public String getTrackProgressBar(boolean embed) {
        float process = (100f / getPlayingTrack().getDuration() * getTrackPosition());
        if (embed) {
            return StringsUtil.getProgressBarEmbed(process);
        } else {
            return StringsUtil.getProgressBar(process);
        }
    }

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

    public void playTracks(Collection<AudioTrack> tracks) {
        tracks.forEach(this::playTrack);
    }

    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
    }

    public boolean toggleShuffleOnRepeat() {
        boolean current = shuffle;
        shuffle = !current;
        return current;
    }

    public void shuffle() {
        List<AudioTrack> tracks = new ArrayList<>(queue);
        Collections.shuffle(tracks);
        queue.clear();
        queue.addAll(tracks);
    }

    public boolean isShuffleEnabled() {
        return shuffle;
    }

    public LoopMode getLoopMode() {
        return loopMode;
    }

    protected void playTrackInternal(AudioTrack track) {
        player.playTrack(track);
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    public void skip() {
        stopTrack();
    }

    public void stop() {
        queue.clear();
        loopMode = LoopMode.DISABLED;
        stopTrack();
    }


    public void removeTrack(int index) {
        List<AudioTrack> tracks = new ArrayList<>(queue);
        tracks.remove(index);
        queue.clear();
        queue.addAll(tracks);
    }

    public void moveTrack(int track, int pos) {
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

    public void addTrack(AudioTrack track) {
        if (getPlayingTrack() != null) {
            queue.add(track);
        } else {
            playTrack(track);
        }
    }

    public void addTracks(Collection<AudioTrack> tracks) {
        tracks.forEach(this::addTrack);
    }

    public void setQueue(LinkedList<AudioTrack> queue) {
        this.queue = queue;
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
