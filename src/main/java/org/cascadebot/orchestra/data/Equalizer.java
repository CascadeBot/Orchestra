package org.cascadebot.orchestra.data;

import org.cascadebot.orchestra.players.CascadePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Equalizer {
    public static int BAND_COUNT = com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT;
    boolean enabled = false;
    Map<Integer, Float> bandsMap = new HashMap<>();
    CascadePlayer player;

    public Equalizer(CascadePlayer player) {
        for (int i = 0; i < BAND_COUNT; i++) {
            bandsMap.put(0, .5f);
        }
        this.player = player;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled == false) {
            for (int i = 0; i < BAND_COUNT; i++) {
                bandsMap.put(0, .5f);
            }
            player.equzlizerChanged();
        }
    }

    public Equalizer(CascadePlayer player, List<Float> initalValues) {
        if (initalValues.size() != BAND_COUNT) {
            throw new UnsupportedOperationException("Invalid number of bands passed in!");
        }
        int i = 0;
        for (Float value : initalValues) {
            bandsMap.put(i, value);
            i++;
        }
        this.player = player;
    }

    public void setBand(int band, float value) {
        if (band >= BAND_COUNT || band < 0) {
            throw new UnsupportedOperationException("Band out of range!");
        }
        bandsMap.put(band, value);
        if (enabled) {
            player.equzlizerChanged();
        }
    }

    public Map<Integer, Float> getBandsMap() {
        return bandsMap;
    }
}
