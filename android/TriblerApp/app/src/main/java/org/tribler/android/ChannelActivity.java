package org.tribler.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.jakewharton.rxbinding.support.v7.widget.SearchViewQueryTextEvent;

import rx.Observer;

public class ChannelActivity extends BaseActivity {

    public static final String EXTRA_DISPERSY_CID = "org.tribler.android.dispersy.CID";
    public static final String EXTRA_SUBSCRIBED = "org.tribler.android.SUBSCRIBED";

    private ChannelFragment _fragment;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        _fragment = (ChannelFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_channel);
        _fragment.setRetainInstance(true);

        handleIntent(getIntent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        _fragment = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Add items to the action bar (if it is present)
        getMenuInflater().inflate(R.menu.menu_channel, menu);

        // Search button
        MenuItem btnFilter = menu.findItem(R.id.btn_filter_channel);
        SearchView searchView = (SearchView) btnFilter.getActionView();

        // Set search hint
        searchView.setQueryHint(getText(R.string.action_search_in_channel));

        // Filter on query text change
        rxSubs.add(RxSearchView.queryTextChangeEvents(searchView)
                .subscribe(new Observer<SearchViewQueryTextEvent>() {

                    public void onNext(SearchViewQueryTextEvent event) {
                        _fragment.getAdapter().getFilter().filter(event.queryText());
                    }

                    public void onCompleted() {
                    }

                    public void onError(Throwable e) {
                        Log.e("onCreateOptionsMenu", "SearchViewQueryTextEvent", e);
                    }
                }));

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Toggle channel favorite
        boolean subscribed = getIntent().getBooleanExtra(ChannelActivity.EXTRA_SUBSCRIBED, false);
        if (subscribed) {
            menu.findItem(R.id.btn_channel_unsubscribe).setVisible(true);
        } else {
            menu.findItem(R.id.btn_channel_subscribe).setVisible(true);
        }

        return true;
    }

    protected void handleIntent(Intent intent) {
        if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
            // Set title
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                String title = intent.getStringExtra(Intent.EXTRA_TITLE);
                boolean subscribed = intent.getBooleanExtra(EXTRA_SUBSCRIBED, false);
                if (!subscribed) {
                    title = getString(R.string.title_channel_preview) + ": " + title;
                }
                actionBar.setTitle(title);
            }
        }
    }

    public void btnSubscribeClicked(MenuItem item) {
        String dispersyCid = getIntent().getStringExtra(ChannelActivity.EXTRA_DISPERSY_CID);
        boolean subscribed = getIntent().getBooleanExtra(ChannelActivity.EXTRA_SUBSCRIBED, false);
        String name = getIntent().getStringExtra(Intent.EXTRA_TITLE);

        _fragment.subscribe(dispersyCid, subscribed, name);
    }

    public void btnUnsubscribeClicked(MenuItem item) {
        String dispersyCid = getIntent().getStringExtra(ChannelActivity.EXTRA_DISPERSY_CID);
        boolean subscribed = getIntent().getBooleanExtra(ChannelActivity.EXTRA_SUBSCRIBED, false);
        String name = getIntent().getStringExtra(Intent.EXTRA_TITLE);

        _fragment.unsubscribe(dispersyCid, subscribed, name);
    }

}
