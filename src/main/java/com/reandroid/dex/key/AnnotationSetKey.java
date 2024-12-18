/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.dex.key;

import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArraySort;

import java.util.function.Predicate;


public class AnnotationSetKey extends KeyList<AnnotationItemKey> implements Key {

    private static final AnnotationItemKey[] EMPTY = new AnnotationItemKey[0];

    public AnnotationSetKey(AnnotationItemKey[] elements) {
        super(removeNulls(elements));
    }

    public AnnotationItemKey get(TypeKey typeKey) {
        int length = size();
        for (int i = 0; i < length; i++) {
            AnnotationItemKey itemKey = get(i);
            if (itemKey != null && ObjectsUtil.equals(typeKey, itemKey.getType())) {
                return itemKey;
            }
        }
        return null;
    }

    @Override
    public AnnotationSetKey add(AnnotationItemKey item) {
        return (AnnotationSetKey) super.add(item);
    }
    @Override
    public AnnotationSetKey remove(AnnotationItemKey itemKey) {
        return (AnnotationSetKey) super.remove(itemKey);
    }
    @Override
    public AnnotationSetKey remove(int index) {
        return (AnnotationSetKey) super.remove(index);
    }
    @Override
    public AnnotationSetKey removeIf(Predicate<? super AnnotationItemKey> predicate) {
        return (AnnotationSetKey) super.removeIf(predicate);
    }
    @Override
    public AnnotationSetKey set(int i, AnnotationItemKey item) {
        return (AnnotationSetKey) super.set(i, item);
    }

    @Override
    AnnotationSetKey newInstance(AnnotationItemKey[] elements) {
        return new AnnotationSetKey(elements);
    }
    @Override
    AnnotationItemKey[] newArray(int length) {
        if (length == 0) {
            return EMPTY;
        }
        return new AnnotationItemKey[length];
    }
    @Override
    AnnotationItemKey[] initializeSortedElements(AnnotationItemKey[] elements) {
        if (elements == null || elements.length < 2) {
            return null;
        }
        boolean needsSort = false;
        int length = elements.length;
        AnnotationItemKey previous  = elements[0];
        for (int i = 1; i < length; i ++) {
            AnnotationItemKey next = elements[i];
            if (CompareUtil.compare(previous, next) > 0) {
                needsSort = true;
                break;
            }
        }
        if (!needsSort) {
            return null;
        }
        elements = elements.clone();
        ArraySort.sort(elements, CompareUtil.getComparableComparator());
        return elements;
    }

    @Override
    public AnnotationSetKey replaceKey(Key search, Key replace) {
        return (AnnotationSetKey) super.replaceKey(search, replace);
    }

    @Override
    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (!(obj instanceof AnnotationSetKey)) {
            return -1;
        }
        return compareElements((AnnotationSetKey) obj);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AnnotationSetKey)) {
            return false;
        }
        return equalsElements((AnnotationSetKey) obj);
    }

    @Override
    public int hashCode() {
        return getHashCode();
    }

    private static AnnotationItemKey[] removeNulls(AnnotationItemKey[] elements) {
        if (elements == null || elements.length == 0) {
            return EMPTY;
        }
        int length = elements.length;
        int size = 0;
        for (int i = 0; i < length; i ++) {
            AnnotationItemKey key = elements[i];
            if (key != null) {
                size ++;
            }
        }
        if (size == length) {
            return elements;
        }
        if (size == 0) {
            return EMPTY;
        }
        AnnotationItemKey[] results = new AnnotationItemKey[size];
        int j = 0;
        for (int i = 0; i < length; i ++) {
            AnnotationItemKey key = elements[i];
            if (key != null) {
                results[j] = key;
                j ++;
            }
        }
        return results;
    }
}
