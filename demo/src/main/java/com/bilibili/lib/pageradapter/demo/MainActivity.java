/*
 * Copyright (C) 2016 Bilibili, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bilibili.lib.pageradapter.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bilibili.lib.pageradapter.IDFragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author yrom.
 */
public class MainActivity extends AppCompatActivity {

    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.shuffle();
            }
        });
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        setupViewPager(savedInstanceState, viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(Bundle savedInstanceState, ViewPager pager) {
        List<Color> colors;
        if (savedInstanceState == null) {
            colors = new ArrayList<>(10);
            colors.add(new Color(1, 0xFFFF5722));
            colors.add(new Color(2, 0xFF8D6E63));
            colors.add(new Color(3, 0xFF9C27B0));
            colors.add(new Color(4, 0xFF757575));
            colors.add(new Color(5, 0xFF8BC34A));
            colors.add(new Color(6, 0xFF2196F3));
            colors.add(new Color(7, 0xFFE91E63));
            colors.add(new Color(8, 0xFFFFC107));
            colors.add(new Color(9, 0xFF795548));
            colors.add(new Color(0, 0xFF4CAF50));
        } else {
            // restore shuffled colors
            colors = fromArray(savedInstanceState.getIntArray("colors"));
        }
        Adapter adapter = new Adapter(getSupportFragmentManager(), colors);
        pager.setAdapter(adapter);
        this.adapter = adapter;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save adapter shuffled color state
        outState.putIntArray("colors", toArray(adapter.mList));
    }

    static List<Color> fromArray(int[] arr) {
        List<Color> list = new ArrayList<>(arr.length / 2);
        for (int i = 0; i < arr.length; i += 2) {
            list.add(new Color(arr[i], arr[i + 1]));
        }
        return list;
    }

    static int[] toArray(List<Color> list) {
        int[] arr = new int[list.size() * 2];
        for (int i = 0; i < list.size(); i++) {
            arr[i * 2] = list.get(i).id;
            arr[i * 2 + 1] = list.get(i).value;
        }
        return arr;
    }

    static class Adapter extends IDFragmentStatePagerAdapter {
        @NonNull
        List<Color> mList;
        Random random = new Random();

        public Adapter(FragmentManager fm, @NonNull List<Color> list) {
            super(fm);
            mList = list;
        }

        @Override
        protected int getItemId(int position) {
            return mList.get(position).id;
        }

        @Override
        protected Fragment getItem(int id) {
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).id == id) {
                    return ColorFragment.newInstance(mList.get(i).value);
                }
            }
            throw new AssertionError();
        }

        @Override
        protected int getPositionOf(int id) {
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).id == id) {
                    return i;
                }
            }
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "P-" + getItemId(position);
        }

        void shuffle() {
            Collections.shuffle(mList, random);
            notifyDataSetChanged();
        }
    }

    static class Color {
        public final int value;
        public final int id;

        public Color(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

}
