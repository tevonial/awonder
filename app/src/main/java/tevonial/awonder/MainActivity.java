package tevonial.awonder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
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
import android.widget.Toast;

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
    public static Handler sUiHandler;
    public static ProgressBar sLoading;
    public static int sCurrentView = 0;
    public static Fragment homeFragment, answerPollFragment, pollFragment, resultsFragment, historyFragment, preferenceFragment;
    public static FragmentManager sFragmentManager;
    public static CoordinatorLayout sRootView;
    public static int FRAGMENT_HOME = 0, FRAGMENT_ANSWER_POLL = 1, FRAGMENT_POLL = 2,
                      FRAGMENT_RESULTS = 3, FRAGMENT_HISTORY = 4, FRAGMENT_PREFERENCE = 5;

    private boolean mUpNavEnabled;
    private ActionBarDrawerToggle mToggle;
    private DrawerLayout mDrawer;
    private static boolean mAllowCreateHome = true;
    private static boolean mErrorShown;

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

        sRootView = (CoordinatorLayout) findViewById(R.id.root);
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

    public static boolean allowCreateHome() {
        return mAllowCreateHome;
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
                switchView(FRAGMENT_HOME);
                break;
            case R.id.nav_history:
                switchView(FRAGMENT_HISTORY);
                break;
            case R.id.nav_preferences:
                switchView(FRAGMENT_PREFERENCE);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    //endregion

    public static void switchView(int view) {
        if (!HttpHandler.isOnline() && view != FRAGMENT_HOME) { mAllowCreateHome = false; }

        if (view != sCurrentView) {

            if (view >= 0) {
                sFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (view < 0) {
                view *= -1;
            }

            if (view > 0) {
                Fragment[] fragments = {homeFragment, answerPollFragment, pollFragment, resultsFragment, historyFragment, preferenceFragment};
                FragmentTransaction transaction = sFragmentManager.beginTransaction();
                transaction.replace(R.id.content, fragments[view]);
                transaction.addToBackStack(null);
                transaction.commit();
            }

        }

        mAllowCreateHome = true;
    }


    public class InitTask extends AsyncTask<Void, Void, Void> {

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
            sContext = MainActivity.this;
            sUiHandler = new Handler(Looper.getMainLooper());

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
            if (!HttpHandler.hasHost()) {
                switchView(5);
                Toast.makeText(MainActivity.sContext, "Please enter a host", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
