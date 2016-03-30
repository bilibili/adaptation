/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2016 Bilibili, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bilibili.lib.pageradapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * The {@code position} of fragment sometimes is unreliable.
 * So this PagerAdapter implementation use item id instead of position to saving fragments.
 */
public abstract class IDFragmentStatePagerAdapter extends PagerAdapter {
    private static final String TAG = "ID-PagerAdapter";
    private static final boolean DEBUG = false;
    private static final String KEY_PREFIX = "i";
    private static final String KEY_SAVED_STATES = "states";
    /** Fragment instances cache map, the key is id */
    private SparseArray<Fragment> mFragments = new SparseArray<>();
    /** The key is id */
    private SparseArray<Fragment.SavedState> mSavedState = new SparseArray<>();
    private final FragmentManager mFragmentManager;

    private FragmentTransaction mCurrentTransaction = null;
    private Fragment mCurrentPrimaryItem = null;

    public IDFragmentStatePagerAdapter(FragmentManager fm) {
        mFragmentManager = fm;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final int id = getItemId(position);
        // find in restored fragments cache
        Fragment f = mFragments.get(id);
        if (f != null) {
            return f;
        }

        if (mCurrentTransaction == null) {
            // will commit() when finishUpdate() call
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(id);
        if(DEBUG) {
            Log.v(TAG, "Adding item #" + position + ": id =" + id + ", f=" + fragment);
        }
        Fragment.SavedState fss = mSavedState.get(id);
        if (fss != null) {
            Bundle state;
            try {
                state = Reflection.on(Fragment.SavedState.class).fieldValue(fss, "mState");
            } catch (NoSuchFieldException e) {
                if(DEBUG) Log.w(TAG, "No field name 'mState'!");
                state = null;
            }
            if (state != null) {
                state.setClassLoader(fragment.getClass().getClassLoader());
            }
            fragment.setInitialSavedState(fss);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        mFragments.put(id, fragment);
        mCurrentTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;

        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }
        int id = getItemId(position);
        if(DEBUG)
            Log.v(TAG, "Removing item #" + position + ": id=" + id + ", f=" + object
                + " v=" + ((Fragment) object).getView());
        // The Fragment instance will be destroyed when finishUpdate() called.
        // Don't worry. Save it's state here!
        mSavedState.put(id, mFragmentManager.saveFragmentInstanceState(fragment));
        // Remove it from cache and the FragmentManager.
        mFragments.remove(id);
        mCurrentTransaction.remove(fragment);
    }

    @Override
    @CallSuper
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    /**
     * @param position Position within this adapter
     * @return Unique identifier for the item at position
     */
    protected abstract int getItemId(int position);

    /**
     * @param id The item's id
     * @return Fragment associated with a specified id
     * @see #getItemId(int)
     */
    protected abstract Fragment getItem(int id);

    /**
     * Useful for {@link #getItemPosition(Object)} to determine if
     * Fragment item's position has changed.
     * @param id Fragment item id
     * @return index in [0, {@link #getCount()}),
     *         or {@link #POSITION_UNCHANGED} if the item's position has not changed,
     *         or {@link #POSITION_NONE} if the item is no longer present.
     * @see #getItemPosition(Object)
     */
    protected abstract int getPositionOf(int id);

    @Override
    public final int getItemPosition(Object object) {
        int position = mFragments.indexOfValue((Fragment) object);
        if (position < 0) return POSITION_NONE;
        int id = mFragments.keyAt(position);
        return getPositionOf(id);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurrentTransaction != null) {
            mCurrentTransaction.commitAllowingStateLoss();
            mCurrentTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    @CallSuper
    public Parcelable saveState() {
        Bundle state = new Bundle();
        if (mSavedState.size() > 0) {
            state.putSparseParcelableArray(KEY_SAVED_STATES, mSavedState);
        }
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment f = mFragments.valueAt(i);
            String key = KEY_PREFIX + mFragments.keyAt(i);
            // save Fragment sate in FragmentManager by our key!
            mFragmentManager.putFragment(state, key, f);
        }
        return state;
    }

    @Override
    @CallSuper
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            mSavedState.clear();
            mFragments.clear();
            SparseArray<Fragment.SavedState> fss = bundle.getSparseParcelableArray(KEY_SAVED_STATES);
            if (fss != null) {
                for (int i = 0; i < fss.size(); i++) {
                    mSavedState.put(fss.keyAt(i), fss.valueAt(i));
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key : keys) {
                if (key.startsWith(KEY_PREFIX)) {
                    Fragment f = mFragmentManager.getFragment(bundle, key);
                    int id;
                    try {
                        id = Integer.parseInt(key.substring(KEY_PREFIX.length()));
                    } catch (NumberFormatException e) {
                        // should not happen!
                        throw new IllegalStateException("Can't find id at key " + key);
                    }
                    if (f != null) {
                        f.setMenuVisibility(false);
                        mFragments.put(id, f);
                    } else {
                        Log.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }
}
