package org.tribler.android;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

public class SearchActivity extends AppCompatActivity {

    private SearchActivityFragment mSearchFragment;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGui();

        FragmentManager fm = getFragmentManager();
        // Check to see if we have retained the worker fragment
        mSearchFragment = (SearchActivityFragment) fm.findFragmentByTag("search");
        // If not retained (or first time running), we need to create it
        if (mSearchFragment == null) {
            mSearchFragment = new SearchActivityFragment();
            // Tell the framework to try to keep this fragment around during a configuration change
            mSearchFragment.setRetainInstance(true);
            fm.beginTransaction().add(mSearchFragment, "search").commit();

            handleIntent(getIntent());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchFragment.doMySearch(query);
        }
    }

    private void initGui() {
        setContentView(R.layout.activity_search);

        // Set action toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_search_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.search_results_list);
        // Improve performance
        recyclerView.setHasFixedSize(true);

        VerticalRecyclerViewFastScroller fastScroller =
                (VerticalRecyclerViewFastScroller) findViewById(R.id.fast_scroller);

        // Connect the recycler to the scroller (to let the scroller scroll the list)
        fastScroller.setRecyclerView(recyclerView);

        // Connect the scroller to the recycler (to let the recycler scroll the scroller's handle)
        recyclerView.addOnScrollListener(fastScroller.getOnScrollListener());

        // Scroll to the current position of the layout manager
        setRecyclerViewLayoutManager(recyclerView);
    }

    /**
     * @param recyclerView Set the LayoutManager of this RecycleView
     */
    private void setRecyclerViewLayoutManager(RecyclerView recyclerView) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (recyclerView.getLayoutManager() != null) {
            scrollPosition =
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(scrollPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Add items to the action bar if it is present
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_search_action_bar, menu);

        // Search button
        MenuItem btnSearch = (MenuItem) menu.findItem(R.id.btn_search);
        assert btnSearch != null;
        final SearchView searchView = (SearchView) btnSearch.getActionView();

        // Show search input field
        searchView.setIconified(false);

        // Never close search view
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean onClose() {
                // Override default behaviour with return true
                return true;
            }
        });

        // Restore last query
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query != null && !query.isEmpty()) {
            searchView.setQuery(query, false);
            searchView.clearFocus();
        }

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                Intent intent = new Intent(SearchActivity.this, SearchActivity.class);
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, query);
                onNewIntent(intent);
                return true;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

}
