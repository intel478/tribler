package org.tribler.android.restapi.json;

public class TorrentsResponse {

    private TriblerTorrent[] torrents;

    TorrentsResponse() {
    }

    public TriblerTorrent[] getTorrents() {
        return torrents;
    }

}