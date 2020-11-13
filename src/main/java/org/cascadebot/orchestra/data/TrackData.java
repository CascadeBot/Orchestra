package org.cascadebot.orchestra.data;

public class TrackData {

    private final long userId;
    private final long errorChannelId;
    private final long guildId;

    public TrackData(long userId, long errorChannelId, long guildId) {
        this.userId = userId;
        this.errorChannelId = errorChannelId;
        this.guildId = guildId;
    }

    public long getUserId() {
        return userId;
    }

    public long getErrorChannelId() {
        return errorChannelId;
    }

    public long getGuildId() {
        return guildId;
    }

}
