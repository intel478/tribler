package org.tribler.android;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class TriblerViewAdapterTouchCallback extends ItemTouchHelper.SimpleCallback {

    private TriblerViewAdapter mAdapter;
    private ItemTouchHelper mHelper;

    /**
     * Swipe left and right
     */
    public TriblerViewAdapterTouchCallback(TriblerViewAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mAdapter = adapter;
        mHelper = new ItemTouchHelper(this);
    }

    /**
     * Attaches the ItemTouchHelper to the provided RecyclerView. If TouchHelper is already
     * attached to a RecyclerView, it will first detach from the previous one. You can call this
     * method with {@code null} to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     *                     {@code null} if you want to remove ItemTouchHelper from the current
     *                     RecyclerView.
     */
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        mHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        TriblerViewAdapter.OnSwipeListener listener = mAdapter.getOnSwipeListener();
        if (listener == null)
            return;

        int adapterPosition = viewHolder.getAdapterPosition();
        // Swipe channel
        if (viewHolder instanceof TriblerViewAdapter.ChannelViewHolder) {
            TriblerChannel channel = (TriblerChannel) mAdapter.getItem(adapterPosition);
            if (swipeDir == ItemTouchHelper.LEFT) {
                listener.onSwipedLeft(channel);
            } else if (swipeDir == ItemTouchHelper.RIGHT) {
                listener.onSwipedRight(channel);
            }
        }
        // Swipe torrent
        else if (viewHolder instanceof TriblerViewAdapter.TorrentViewHolder) {
            TriblerTorrent torrent = (TriblerTorrent) mAdapter.getItem(adapterPosition);
            if (swipeDir == ItemTouchHelper.LEFT) {
                listener.onSwipedLeft(torrent);
            } else if (swipeDir == ItemTouchHelper.RIGHT) {
                listener.onSwipedRight(torrent);
            }
        }
    }

    /**
     * Not draggable
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /**
     * Not draggable
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }
};