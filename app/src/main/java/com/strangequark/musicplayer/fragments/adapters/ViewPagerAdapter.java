package com.strangequark.musicplayer.fragments.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.strangequark.musicplayer.fragments.AlbumsFragment;
import com.strangequark.musicplayer.fragments.ArtistsFragment;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentPagerAdapter
{
    private ArrayList<Fragment> mFragmentList = new ArrayList<>();
    private ArrayList<String> mFragmentTitleList = new ArrayList<>();
    ArtistsFragment af;
    AlbumsFragment ab;

    public ViewPagerAdapter(FragmentManager supportFragmentManager)
    {
        super(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position)
    {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    public void addFragment(Fragment fragment, String title)
    {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }
}
