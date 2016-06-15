package tevonial.awonder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import tevonial.awonder.fragment.AnswerPollFragment;
import tevonial.awonder.fragment.HistoryFragment;
import tevonial.awonder.fragment.HomeFragment;
import tevonial.awonder.fragment.PollFragment;
import tevonial.awonder.fragment.PreferenceFragment;
import tevonial.awonder.fragment.ResultsFragment;
import tevonial.awonder.handler.HttpHandler;
import tevonial.awonder.handler.PreferenceHandler;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    public static Context sContext;
    public static ProgressBar sLoading;
    public static int sCurrentView = 0;
    public static Fragment homeFragment, answerPollFragment, pollFragment, resultsFragment, historyFragment, preferenceFragment;
    public static FragmentManager sFragmentManager;
    public static int FRAGMENT_HOME = 0, FRAGMENT_ANSWER_POLL = 1, FRAGMENT_POLL = 2,
                      FRAGMENT_RESULTS = 3, FRAGMENT_HISTORY = 4, FRAGMENT_PREFERENCE = 5;

    private boolean mUpNavEnabled;
    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sFragmentManager = getSupportFragmentManager();
        sFragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                syncNavigation();
            }
        });
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        sLoading = (ProgressBar) findViewById(R.id.main_loading);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();

        ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);
        mToolbar.setNavigationOnClickListener(this);

        if (savedInstanceState == null) {
            (new InitTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        syncNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            PreferenceHandler.saveAll();
        }
    }

    //region Navigation and Menu
    private void syncNavigation() {
        boolean last = mUpNavEnabled;
        mUpNavEnabled = (sFragmentManager.getBackStackEntryCount() > 1);
        mUpNavEnabled |= (sCurrentView == FRAGMENT_ANSWER_POLL | sCurrentView == FRAGMENT_POLL);

        if (mUpNavEnabled != last) {
            int start = mUpNavEnabled ? 0 : 1;
            ValueAnimator mNavAnimator = ValueAnimator.ofFloat(start, 1 - start);
            mNavAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float slideOffset = (Float) valueAnimator.getAnimatedValue();
                    mToggle.onDrawerSlide(mDrawer, slideOffset);
                }
            });
            mNavAnimator.setInterpolator(new DecelerateInterpolator());
            mNavAnimator.setDuration(300);
            mNavAnimator.start();
        }
    }

    @Override
    public void onClick(View v) {
        if (mUpNavEnabled) {
            onBackPressed();
        } else {
            if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                mDrawer.closeDrawer(GravityCompat.START);
            } else {
                mDrawer.openDrawer(GravityCompat.START);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                switchView(0);
                break;
            case R.id.nav_history:
                switchView(4);
                break;
            case R.id.nav_setting:
                switchView(5);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    //endregion

    public static void switchView(int view) {
        if (view != sCurrentView) {
            FragmentTransaction transaction = sFragmentManager.beginTransaction();

            if (view >= 0) {
                sFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (view < 0) {
                view *= -1;
            }

            if (view > 0) {
                Fragment[] fragments = {homeFragment, answerPollFragment, pollFragment, resultsFragment, historyFragment, preferenceFragment};
                transaction.replace(R.id.content, fragments[view]);
                transaction.addToBackStack(null);
            }

            transaction.commit();
        }
    }

    private class InitTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            ((PreferenceFragment) preferenceFragment).setScreenSize(size.x, size.y);

            PreferenceHandler.init();

            HistoryFragment.init();
            return null;
        }

        @Override
        protected void onPreExecute() {
            sLoading.setVisibility(View.VISIBLE);
            sContext = MainActivity.this;

            homeFragment = new HomeFragment();
            answerPollFragment = new AnswerPollFragment();
            pollFragment = new PollFragment();
            resultsFragment = new ResultsFragment();
            historyFragment = new HistoryFragment();
            preferenceFragment = new PreferenceFragment();

            Fragment[] fragments = {homeFragment, answerPollFragment, pollFragment, resultsFragment, historyFragment, preferenceFragment};
            for (Fragment f : fragments) {
                f.setRetainInstance(true);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content, homeFragment).commit();
        }
    }
}
