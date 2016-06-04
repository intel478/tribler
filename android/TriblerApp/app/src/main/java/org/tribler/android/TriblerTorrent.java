package org.tribler.android;

/**
 * Deserialization of JSON Torrent
 */
public class TriblerTorrent {

    private int id, size, num_seeders, num_leechers, last_tracker_check;
    private String infohash, name, category, thumbnail_url;

    public TriblerTorrent() {
    }

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getNumSeeders() {
        return num_seeders;
    }

    public int getNumLeechers() {
        return num_leechers;
    }

    public int getLastTrackerCheck() {
        return last_tracker_check;
    }

    public String getInfohash() {
        return infohash;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getThumbnailUrl() {
        if (thumbnail_url == null) {
            thumbnail_url = ""; //TODO: default image
        }
        return thumbnail_url;
    }

}
