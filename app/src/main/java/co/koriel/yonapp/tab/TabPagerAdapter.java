package co.koriel.yonapp.tab;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import co.koriel.yonapp.fragment.HomeFragment;
import co.koriel.yonapp.fragment.HomeRootFragment;
import co.koriel.yonapp.fragment.LibraryFragment;
import co.koriel.yonapp.fragment.LibraryRootFragment;
import co.koriel.yonapp.fragment.NoticeFragment;
import co.koriel.yonapp.fragment.NoticeRootFragment;
import co.koriel.yonapp.fragment.TimeTableFragment;
import co.koriel.yonapp.fragment.TimeTableRootFragment;

public class TabPagerAdapter extends FragmentStatePagerAdapter {

    private final List<Fragment> rootFragments = new ArrayList<>();
    private final static List<Fragment> firstFragments = new ArrayList<>();

    public TabPagerAdapter(FragmentManager fm) {
        super(fm);

        rootFragments.clear();
        rootFragments.add(new HomeRootFragment());
        rootFragments.add(new NoticeRootFragment());
        rootFragments.add(new TimeTableRootFragment());
        rootFragments.add(new LibraryRootFragment());

        firstFragments.clear();
        firstFragments.add(new HomeFragment());
        firstFragments.add(new NoticeFragment());
        firstFragments.add(new TimeTableFragment());
        firstFragments.add(new LibraryFragment());
    }

    @Override
    public Fragment getItem(int position) {
        return rootFragments.get(position);
    }

    public static Fragment getFirstFragment(int position) {
        return firstFragments.get(position);
    }

    @Override
    public int getCount() {
        return rootFragments.size();
    }
}
